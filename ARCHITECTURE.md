# 架构设计

基于 Kuikly 2.7.0（Kotlin Multiplatform）的 AI 股票对话 Demo。依赖方向单向：UI 视图层 → 状态层 → 业务层 → 数据源层，数据模型层被各层共享。

## 分层架构

```
┌─────────────────────────────────────────────────────┐
│                 UI 视图层 (ui/)                      │
│   page/ 页面 · component/ 组件 · theme/ 主题          │
│   ChatPage / StockDetailPage / MarkdownText / ...   │
├─────────────────────────────────────────────────────┤
│                 状态层 (state/)                      │
│   ChatState · StockDetailState                      │
├─────────────────────────────────────────────────────┤
│               业务逻辑层 (service/)                   │
│   ChatService · StockService · ChatSessionStore      │
│   ServiceLocator（依赖装配）                         │
├─────────────────────────────────────────────────────┤
│               数据源层 (repository/)                  │
│   AiRepository / StockRepository（接口）              │
│   RealAiRepository / MockAiRepository               │
│   MockStockRepository                               │
├─────────────────────────────────────────────────────┤
│                数据模型层 (model/)                    │
│   ChatMessage / ChatBlock / StockQuote / StockDetail │
└─────────────────────────────────────────────────────┘
```

依赖方向：

```
ui → state → service → repository(接口) ← mock / real 实现
                        ↑
                  model（纯数据，被各层共享，不依赖 Kuikly UI）
```

四层分离（页面 / 组件 / 数据 / 状态）：

| 层 | 目录 | 职责 |
| --- | --- | --- |
| 页面 | `ui/page/` | 页面生命周期、路由、渲染、UI 副作用注入 |
| 组件 | `ui/component/` | 可复用视图（气泡 / 卡片 / 图表 / Markdown） |
| 数据 | `model/` | 纯数据模型（`ChatMessage` / `ChatBlock` / `StockQuote` / `StockDetail`） |
| 状态 | `state/` | 响应式状态 + 状态变更逻辑（`ChatState` / `StockDetailState`） |

## 数据流

**聊天问答**

```
用户输入
  → ChatPage.send → ChatState.ask
  → ChatService.ask → AiRepository.reply(userInput, history, onResult)
  → onResult(AiReply) → ChatState.appendAssistant → messages(observableList)
  → 列表增量刷新（List + vforIndex）
```

**详情页**

```
点击卡片 → ChatPage.openDetail → RouterModule.openPage("stock_detail", { code })
  → StockDetailPage.created → StockDetailState.load(code)
  → StockService.getDetail → StockRepository.getDetail → StockDetail
  → detail(observable) → 页面渲染
```

## 依赖装配

所有依赖集中在 `ServiceLocator` 创建，UI 层只通过 `chatService` / `stockService` 获取业务能力，不感知数据源实现。切换真实 / Mock 只改这一处。

| 成员 | 类型 / 值 | 说明 |
| --- | --- | --- |
| `apiKey` | `String?` | DeepSeek API Key，仅内存 |
| `model` | `String` | `deepseek-chat` / `deepseek-reasoner` |
| `pendingQuestion` | `String?` | 详情页「继续追问」跨页面瞬态传递 |
| `stockRepository` | `MockStockRepository` | 行情数据源（当前 Mock） |
| `aiRepository` | `RealAiRepository` | AI 数据源（当前 DeepSeek） |
| `chatService` / `stockService` | — | 对外暴露的业务服务 |

## 接口定义

```kotlin
internal interface AiRepository {
    fun reply(userInput: String, history: List<ChatMessage>, onResult: (AiReply) -> Unit)
}

internal interface StockRepository {
    fun getQuote(code: String): StockQuote?
    fun getDetail(code: String): StockDetail?
    fun getAllQuotes(): List<StockQuote>
}
```

接口为 `internal`，采用回调式签名适配网络异步返回。

| 实现 | 数据源 | 行为 |
| --- | --- | --- |
| `RealAiRepository` | DeepSeek `chat/completions` | `stream=false` 单次回调；Bearer 鉴权；多轮历史拼接；识别到股票时附加卡片 / 图表 / 洞察内容块 |
| `MockAiRepository` | 本地关键字匹配 | 按「代码 → 名称」匹配；命中返回结构化回复，未命中返回引导语 |
| `MockStockRepository` | 硬编码 | 4 只股票 + 3 个指数，含走势点、摘要、AI 解读 |

## ChatBlock 设计

AI 回复抽象为内容块列表，而非单一文本：

```kotlin
sealed class ChatBlock {
    data class Text(val markdown: String) : ChatBlock()
    data class StockCard(val stock: StockQuote) : ChatBlock()
    data class MiniChart(val stock: StockQuote, val points: List<PricePoint>) : ChatBlock()
    data class TrendCard(val direction: TrendDirection, val signals: List<String>) : ChatBlock()
    data class RiskCard(val level: RiskLevel, val warnings: List<String>) : ChatBlock()
}
```

- 渲染层通过 `when` 分发，新增内容形态只需加子类 + 分支，符合开闭原则。
- Markdown 由 `MarkdownParser` 解析为语法树，`MarkdownText` 基于官方 `RichText/Span` 渲染，表格用 `Row/Text` 均分列宽。
- 折线图由 `LineChart`（`Canvas`）绘制，绘制逻辑抽为纯函数 `drawPolylineChart(...)`，被迷你卡片与详情页复用。
- `buildInsightBlocks(quote)` 按涨跌幅阈值 ±1.0% 生成「趋势判断 + 风险提醒」，Mock 与真实模式共用。

## 路由

| 页面 | 路由名 | 说明 |
| --- | --- | --- |
| 聊天主页 | `stock_chat` | 应用默认入口（`KuiklyRenderActivity` 的 `pageName` 兜底值） |
| 股票详情页 | `stock_detail` | 经 `RouterModule.openPage("stock_detail", { code })` 进入 |

- 详情页在 `created()` 中读取 `pageData.params.optString("code")` 加载数据。
- 「继续追问」通过 `ServiceLocator.pendingQuestion` 跨页传递：详情页写入并关闭，聊天页在 `pageDidAppear` 中消费后发起提问。

## 持久化

`ChatSessionStore` 提供 `toSessionJson` / `sessionFromJson`，序列化整条 `ChatMessage`（含 `ChatBlock` 全形态）。存储 Key 为 `stock_ai_chat_session_v1`，Android 侧由 `SharedPreferencesModule` 落盘。页面在 `created()` 中恢复，消息变更时写回。

## 扩展点

接入真实行情 API 三步：

1. 实现 `StockRepository`（`getQuote` / `getDetail` / `getAllQuotes`），通过 `BridgeModule`（`ssoRequest` / `httpRequest`）走宿主网络请求真实行情接口；
2. 在 `ServiceLocator` 将 `MockStockRepository` 替换为真实实现；
3. 切回本地演示：将 `RealAiRepository` 换回 `MockAiRepository(stockRepository)`，并在应用内填一个非空 Key（`ChatState.ask` 仍校验 Key）。

新增内容形态 = 新增 `ChatBlock` 子类 + 渲染分支 + `ChatSessionStore` 序列化分支，三者配套。
