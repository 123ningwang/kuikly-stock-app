# Kuikly AI 股票问答应用 Demo

基于腾讯 [Kuikly](https://github.com/Tencent-TDS/KuiklyUI) 跨平台框架实现的 **AI 股票问答应用** 示例，完整实现「聊天交互 → AI 混合内容回复 → 点击卡片跳转详情」的闭环交互。AI 问答已接入 DeepSeek 真实接口（支持在应用内配置 API Key），行情数据使用本地 Mock 模拟；代码分层清晰，预留了接入真实行情 API 的扩展点。

## 一、功能特性

| 页面 | 能力 |
| --- | --- |
| **AI 聊天主页面** | 聊天输入与发送、会话记录展示（用户 / AI 气泡区分）、上下滚动查看历史、空状态与「正在输入」边界状态、多轮上下文 |
| **AI 返回内容渲染** | 多形态混合渲染：Markdown 富文本（含表格）、股票 / 指数结构化卡片、迷你折线图、趋势判断卡片、风险提醒卡片，卡片可点击跳转 |
| **详情承接页** | 股票 / 指数详情：基础行情、走势图表、行情摘要、AI 解读，底部支持继续追问 |

交互闭环：`聊天发送 → AI 返回文本 + 股票卡片 → 点击卡片 → 跳转详情页`。

## 二、技术栈

- **框架**：Kuikly 2.7.0（Kotlin Multiplatform，DSL 声明式 UI）
- **语言**：Kotlin 2.1.21
- **组件**：Kuikly 官方 `RichText/Span`（Markdown 渲染）、`Canvas`（折线图）、`List/Scroller`（聊天列表）、`Input`（输入框）、`RouterModule`（页面路由）
- **架构**：页面 / 组件 / 数据 / 状态四层分离，业务逻辑层与数据源层接口预留

## 三、目录结构

```
shared/src/commonMain/kotlin/com/example/kuiklytry/
├── base/                       # 工程既有基础封装（BasePager、BridgeModule 等）
└── stock/                      # ★ 本次新增：AI 股票问答业务模块
    ├── model/                  # 数据模型层
    │   ├── ChatModels.kt       #   聊天消息 / 内容块（Markdown、股票卡、图表卡）
    │   └── StockModels.kt      #   股票行情 / 走势点 / 详情模型
    ├── repository/             # 数据源层（★ 接入真实 API 的预留接口）
    │   ├── AiRepository.kt     #   大模型问答接口
    │   ├── StockRepository.kt  #   股票行情接口
    │   ├── MockAiRepository.kt #   大模型 Mock 实现
    │   ├── RealAiRepository.kt #   DeepSeek 真实实现（应用内配置 API Key）
    │   └── MockStockRepository.kt # 行情 Mock 实现
    ├── service/                # 业务逻辑层
    │   ├── ChatService.kt      #   聊天编排
    │   ├── StockService.kt     #   股票业务组装
    │   ├── ChatSessionStore.kt #   会话持久化（序列化 / 反序列化）
    │   └── ServiceLocator.kt   #   依赖装配（替换真实实现的唯一入口）
    ├── state/                  # 状态层（页面 / 组件 / 数据 / 状态 四层分离）
    │   ├── ChatState.kt        #   聊天页状态与状态变更逻辑
    │   └── StockDetailState.kt #   详情页状态
    └── ui/                     # UI 视图层（页面 / 组件）
        ├── theme/              #   主题色板与尺寸
        ├── component/          #   可复用组件
        │   ├── markdown/       #   Markdown 解析器 + RichText 渲染组件（含表格）
        │   ├── LineChart.kt    #   折线图（Canvas）
        │   ├── StockCardView.kt / MiniChartCardView.kt
        │   ├── InsightCardView.kt  # 趋势判断 / 风险提醒卡片
        │   ├── ChatBubbleView.kt / Icons.kt
        └── page/               # 页面
            ├── ChatPage.kt     #   页面 1：聊天主页面
            └── StockDetailPage.kt # 页面 3：股票详情承接页
```

## 四、启动运行

### 环境要求

- JDK 17+（推荐 17 / 21）
- Android Studio（或命令行 Gradle）
- 可访问 `mirrors.tencent.com` Maven 仓库（拉取 Kuikly 依赖）

### 运行步骤（Android）

```bash
# 1. 编译并安装到设备 / 模拟器
./gradlew :androidApp:assembleDebug

# 2. 或在 Android Studio 中打开项目，直接运行 androidApp 模块
```

应用启动后默认进入 `stock_chat`（AI 聊天主页面），无需额外配置。

> 说明：本仓库 `settings.gradle.kts` 中 `h5App` / `miniApp` 目录在当前仓库不存在，已注释，保证 Android Demo 可正常构建；如需 H5 / 小程序端，补齐对应模块目录后取消注释即可。

### 快速体验路径

1. 进入聊天页，自动收到 AI 欢迎语；
2. 输入 `贵州茅台` / `600519` / `宁德时代` / `上证指数` / `创业板指` 等股票或指数并发送；
3. AI 返回 Markdown 解读（含表格）+ 股票/指数卡片 + 迷你折线图 + 趋势判断 + 风险提醒卡片；
4. 点击任意卡片跳转到详情页查看完整行情与 AI 解读，底部可继续追问。

## 五、接入真实行情 API 改造指南

**当前状态**：AI 问答已接入 DeepSeek（`RealAiRepository`，回调式），行情数据由 `MockStockRepository` 提供。接入真实行情 API 只需三步：

1. 实现 `StockRepository` 接口（`getQuote` / `getDetail` / `getAllQuotes`），通过 `BridgeModule` 走宿主网络请求真实行情接口；
2. 在 `ServiceLocator` 中将 `MockStockRepository` 替换为真实实现；
3. 如需切回本地演示模式（不依赖网络与 API Key），在 `ServiceLocator` 中将 `RealAiRepository` 替换回 `MockAiRepository`。

AI 回复的结构化内容块（Markdown 文本、股票/指数卡片、折线图、趋势判断、风险提醒）统一由 `ChatBlock` 承载，渲染层通过 `when` 分发——扩展新内容形态无需改动既有逻辑。

## 六、架构设计

详见 [ARCHITECTURE.md](./ARCHITECTURE.md)。

## 七、License

本项目为 Demo 示例，仅供学习参考。Kuikly 框架版权归腾讯所有。
