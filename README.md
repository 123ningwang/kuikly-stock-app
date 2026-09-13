# Kuiklytry

基于 Kuikly 2.7.0 + Kotlin Multiplatform 开发的 AI 股票对话 Demo，支持自然语言问答、股票卡片、迷你折线图、趋势 / 风险洞察块，跨平台 UI 渲染。

## 项目简介

AI 对话 + 金融可视化 Demo。用户输入自然语言提问，AI 识别股票标的，返回包含 Markdown 文本、股票卡片、迷你折线图、趋势判断、风险提示的结构化消息；点击股票卡片跳转详情页查看完整行情，详情页支持继续追问，会话自动持久化保存。

代码分层：UI 视图层 → 状态层 → 业务层 → 数据源层，`model/` 为纯数据模型。

技术栈：Kuikly 2.7.0、Kotlin 2.1.21、Kotlin Multiplatform，Android 为主目标平台。

> 当前可运行入口为 Android；共享代码层基于 KMP，支持扩展 H5 / 小程序 / HarmonyOS 等端。

## 功能特性

| 功能 | 说明 |
| --- | --- |
| AI 聊天 | 对接 DeepSeek（`stream=false`），支持多轮对话历史 |
| 混合内容块 | Markdown 文本、股票卡片、迷你折线图、趋势卡片、风险卡片 |
| 详情页 | 卡片点击跳转 `stock_detail`，展示完整行情与 AI 解读 |
| 继续追问 | 详情页输入追问，跨页面传参回聊天页发送 |
| 会话持久化 | 存储 Key `stock_ai_chat_session_v1`，Android 用 `SharedPreferencesModule` |
| 双模式 | `RealAiRepository` 真实接口 / `MockAiRepository` 本地离线演示 |

## 快速运行

### 环境要求

- JDK 17+（AGP 8.9.2 要求 JDK 17 起）
- Android Studio Ladybug（2024.2）及以上（兼容 AGP 8.9.2）
- 可联网：需访问 `mirrors.tencent.com`（拉取 Kuikly 依赖）及 Google / Maven Central（或阿里云镜像）

### 运行步骤

```bash
# 仅编译产出 APK（不安装）
./gradlew :androidApp:assembleDebug          # macOS / Linux
gradlew.bat :androidApp:assembleDebug        # Windows

# 编译并安装到已连接的设备 / 模拟器
./gradlew :androidApp:installDebug           # macOS / Linux
gradlew.bat :androidApp:installDebug         # Windows
```

APK 产物路径：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`

也可在 Android Studio 中打开项目，选择 `androidApp` 运行配置，连接设备或启动模拟器后点击 Run。首次同步会下载依赖，耗时较长。

应用启动后默认进入 `stock_chat`（聊天主页）。

## API 配置

AI 问答默认走 DeepSeek 真实接口，需先配置 API Key：

1. 首次启动时自动弹出「API Key 设置」面板；也可点击聊天页右上角菜单 →「API Key 设置」手动打开；
2. 填入 DeepSeek API Key 并保存；
3. Key 仅保存在内存中，重启应用后需重新输入。

未配置 Key 时只能查看 AI 欢迎语，发送消息会提示设置 Key。

## Mock 离线使用

默认走 DeepSeek 真实接口，需要 API Key 才能发消息。完全离线演示两步：

1. 在 `ServiceLocator.kt` 中将 AI 数据源替换为 Mock 实现：

```kotlin
import com.example.kuiklytry.stock.repository.MockAiRepository

// 将下面这行
private val aiRepository: AiRepository = RealAiRepository(stockRepository, { apiKey }, { model })
// 替换为
private val aiRepository: AiRepository = MockAiRepository(stockRepository)
```

2. `ChatState.ask` 仍对 API Key 做非空校验，切到 Mock 后在应用内随意填写一个非空字符串作为 Key 即可（Mock 不会真正请求 DeepSeek）。

## 扩展开发

接入真实行情 API 三步：

1. 实现 `StockRepository` 接口（`getQuote` / `getDetail` / `getAllQuotes`），通过 `BridgeModule` 走宿主网络请求真实行情接口；
2. 在 `ServiceLocator` 中将 `MockStockRepository` 替换为真实实现；
3. 切换数据源只改 `ServiceLocator` 一处，UI 与业务层零改动。

架构详见 [ARCHITECTURE.md](./ARCHITECTURE.md)。

## 原型演示视频
本视频完整演示Kuikly跨平台AI股票应用全链路功能，展示页面交互、关键操作以及AI分析问答效果：

粘贴GitHub自动生成的视频链接在这里


## License

本项目为 Demo 示例，仅供学习参考。Kuikly 框架版权归腾讯所有。
