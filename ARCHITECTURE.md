# 项目架构设计说明

## 一、设计目标

以「AI 股票问答」为场景，在 Kuikly 跨平台框架上落地一个**分层清晰、可维护、易扩展**的 Demo，同时为接入真实大模型 / 行情 API 预留清晰的扩展点。

核心诉求：

1. **功能闭环**：聊天 → AI 混合内容回复 → 卡片跳转详情；
2. **工程分层**：UI 视图层、业务逻辑层、数据模型层严格解耦；
3. **可扩展**：接入真实 API 只改数据源层，UI 与业务层不动。

## 二、分层架构

```
┌─────────────────────────────────────────────────────┐
│                    UI 视图层 (ui/)                    │
│   page/ 页面 · component/ 组件 · theme/ 主题          │
│   ChatPage / StockDetailPage / MarkdownText / ...    │
├─────────────────────────────────────────────────────┤
│                   状态层 (state/)                     │
│   ChatState（聊天状态）· StockDetailState（详情状态）   │
├─────────────────────────────────────────────────────┤
│                 业务逻辑层 (service/)                 │
│   ChatService（聊天编排）· StockService（股票组装）     │
│   ServiceLocator（依赖装配，替换真实实现的唯一入口）     │
├─────────────────────────────────────────────────────┤
│                数据源层 (repository/)                 │
│   AiRepository / StockRepository（接口，★ 扩展点）      │
│   MockAiRepository / MockStockRepository（Mock 实现）  │
├─────────────────────────────────────────────────────┤
│                 数据模型层 (model/)                   │
│   ChatMessage / ChatBlock / StockQuote / StockDetail  │
└─────────────────────────────────────────────────────┘
```

### 依赖方向（单向，上层依赖下层）

```
ui → state → service → repository(接口) ← mock 实现
                        ↑
                    model（被各层共享，纯数据，无依赖）
```

- **数据模型层**：纯 Kotlin 数据类，不依赖 Kuikly UI，保证可在任意层复用与单元测试。
- **数据源层**：以接口定义数据契约，Mock 实现与未来真实实现并行存在，通过 `ServiceLocator` 切换。
- **业务逻辑层**：编排数据源、组装业务结果，不直接触碰 UI 状态。
- **状态层**：持有页面的响应式状态（`observable` / `observableList`）与状态变更逻辑（`ChatState` / `StockDetailState`），不依赖 ViewRef / Module / Pager；滚动、Toast、持久化等 UI 副作用由页面注入回调处理。
- **UI 视图层**：Kuikly 声明式 DSL，负责渲染与交互，页面持有状态层对象并委托状态变更。

## 三、关键设计点

### 1. 混合内容块模型（`ChatBlock`）

AI 回复被抽象为「内容块」列表，而非单一文本：

```kotlin
sealed class ChatBlock {
    data class Text(val markdown: String) : ChatBlock()       // Markdown 富文本
    data class StockCard(val stock: StockQuote) : ChatBlock()  // 股票 / 指数信息卡
    data class MiniChart(val stock: StockQuote, val points: List<PricePoint>) : ChatBlock() // 迷你折线图卡
    data class TrendCard(val direction: TrendDirection, val signals: List<String>) : ChatBlock() // 趋势判断卡
    data class RiskCard(val level: RiskLevel, val warnings: List<String>) : ChatBlock() // 风险提醒卡
}
```

渲染层通过 `when` 分发，符合**开闭原则**——新增内容形态（如表格、K 线图、图片）只需新增一个 `ChatBlock` 子类与对应渲染分支，不影响既有逻辑。

### 2. Markdown 渲染（Kuikly-Markdown 落地）

- `MarkdownParser`：轻量 Markdown 解析器，输出语法树（标题 / 段落 / 列表 / 引用 / 分割线 / 表格 + 行内粗体、斜体、代码）；
- `MarkdownText`：基于 Kuikly 官方 `RichText/Span` 组件渲染语法树，表格用 Row/Text 均分列宽渲染。

对外只暴露 `MarkdownText { attr { text = ... } }`，未来若官方推出独立 Markdown 组件，仅需替换该组件内部实现。

### 3. 折线图复用

`LineChart` 组件基于 Kuikly 官方 `Canvas` 绘制，绘制逻辑抽离为纯函数 `drawPolylineChart(...)`，被迷你走势卡片与详情页图表复用，保证视觉一致。

### 4. 响应式状态与列表

- 会话记录等状态集中在状态层 `ChatState`，使用 `observableList<ChatMessage>`，配合 `List` + `vforIndex` 实现增量更新与虚拟化滚动；
- 「正在输入」「空状态」等边界态通过 `vif` 条件渲染；
- 滚动到底部通过 `ListView.setContentOffset` 实现（由页面注入给状态层）。

### 5. 依赖装配（`ServiceLocator`）

所有依赖在 `ServiceLocator` 中集中创建。接入真实 API 时，仅需在此处把 `MockXxxRepository` 替换为 `RealXxxRepository`，UI 与业务层零改动。

### 6. 指数类型区分

行情标的通过 `StockQuote.kind`（`QuoteKind.STOCK` / `QuoteKind.INDEX`）区分股票与指数。卡片在代码旁标注「股票 / 指数」，详情页据此动态切换标题（「股票详情」/「指数详情」）与走势卡片标题（「股价走势」/「指数走势」）。指数与股票共用同一套卡片、详情页与跳转闭环，无需重复实现。

### 7. 趋势判断与风险提醒卡片

`buildInsightBlocks(quote)` 基于涨跌幅确定性生成「趋势判断」（方向 + 信号解读）与「风险提醒」（等级 + 风险点）两类结构化卡片，Mock 与真实 AI 模式共用。这使 AI 场景中的「趋势判断 / 风险提醒 / 信号解读」能力以可演示的卡片形式落地，而非依赖大模型自由发挥。

### 8. 状态层（state/）与四层分离

为对齐评分维度「页面 / 组件 / 数据 / 状态四层分离」，将页面中的响应式状态与状态变更逻辑从 UI 层抽离，形成独立的状态层：

| 层 | 目录 | 职责 |
| --- | --- | --- |
| 页面 | `ui/page/` | 页面生命周期、路由、渲染与 UI 副作用注入 |
| 组件 | `ui/component/` | 可复用视图组件（气泡 / 卡片 / 图表 / Markdown） |
| 数据 | `model/` | 纯数据模型（`ChatMessage` / `StockQuote` / `StockDetail`） |
| 状态 | `state/` | 响应式状态 + 状态变更逻辑（`ChatState` / `StockDetailState`） |

`ChatState` 持有聊天页全部可观察状态（`messages` / `isTyping` / `inputText` 等）与 `ask` / `welcome` / `clearConversation` / `saveApiKey` 等状态变更方法，不依赖 `ViewRef` / `Module` / `Pager`；页面在 `created()` 中注入 `scrollToBottom` / `toast` / `persist` 三个回调，把 UI 副作用留在页面层，保证状态层可复用、可单元测试。

## 四、页面导航

| 页面 | 路由名 | 说明 |
| --- | --- | --- |
| 聊天主页面 | `stock_chat` | 应用入口页 |
| 股票详情页 | `stock_detail` | 通过 `RouterModule.openPage("stock_detail", { code })` 进入 |

详情页在 `created()` 中读取路由参数 `code`，再由 `StockService` 加载数据。

## 五、扩展方向预留

1. **真实大模型 API**：实现 `AiRepository`，签名升级为 suspend / 回调，走 `BridgeModule.ssoRequest`；
2. **真实行情 API**：实现 `StockRepository`；
3. **流式输出**：`ChatBlock.Text` 支持增量追加，配合 `observable` 实现打字机效果；
4. **多轮上下文**：`ChatService` 维护历史上下文，提交给大模型；
5. **图标替换**：`AppIcons` 中的字形可整体替换为 iconfont / 官方图标组件。
