package com.example.kuiklytry.stock.ui.page

import com.example.kuiklytry.base.BasePager
import com.example.kuiklytry.base.bridgeModule
import com.example.kuiklytry.base.setTimeout
import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.service.ServiceLocator
import com.example.kuiklytry.stock.service.SESSION_STORAGE_KEY
import com.example.kuiklytry.stock.service.sessionFromJson
import com.example.kuiklytry.stock.service.toSessionJson
import com.example.kuiklytry.stock.ui.component.AppIcons
import com.example.kuiklytry.stock.ui.component.ChatBubble
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

/**
 * 页面 1：AI 聊天主页面。
 *
 * 功能：聊天输入、会话记录展示（区分用户/AI 气泡）、上下滚动查看历史、混合内容渲染、
 * 点击股票卡片跳转详情页。业务逻辑委托给 [ServiceLocator.chatService]。
 */
@Page("stock_chat", supportInLocal = true)
internal class ChatPage : BasePager() {

    private val chatService = ServiceLocator.chatService

    private var messages by observableList<ChatMessage>()
    internal var inputText by observable("")
    private var isTyping by observable(false)
    internal var showSidebar by observable(false)
    internal var showKeyPanel by observable(false)
    internal var apiKeyInput by observable("")

    internal lateinit var inputRef: ViewRef<InputView>
    private lateinit var listRef: ViewRef<ListView<*, *>>

    private var messageSeq = 0
    private var contentHeight = 0f

    override fun created() {
        super.created()
        restoreMessages()
        if (messages.isEmpty()) {
            chatService.welcome { appendAssistant(it) }
        }
        if (ServiceLocator.apiKey.isNullOrBlank()) {
            showSidebar = true
            showKeyPanel = true
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val question = pageData.params.optString("question")
        if (question.isNotEmpty()) {
            ask(question)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(StockTheme.background) }

            chatNavBar(ctx)

            List {
                ref { ctx.listRef = it }
                attr {
                    flex(1f)
                    paddingLeft(StockTheme.SPACE_MD)
                    paddingRight(StockTheme.SPACE_MD)
                    showScrollerIndicator(false)
                }
                event {
                    // 跟踪列表内容高度，发送/回复后据此滚动到真实底部
                    contentSizeChanged { _, height ->
                        ctx.contentHeight = height
                    }
                }

                // 空状态
                vif({ ctx.messages.isEmpty() && !ctx.isTyping }) {
                    emptyState()
                }

                // 会话记录
                vforIndex({ ctx.messages }) { message, _, _ ->
                    ChatBubble {
                        attr {
                            this.message = message
                            onStockClick = { code -> ctx.openDetail(code) }
                        }
                    }
                }

                // 正在输入指示
                vif({ ctx.isTyping }) {
                    typingIndicator()
                }
            }

            inputBarDivider()
            chatInputBar(ctx)

            sidebar(ctx)
        }
    }

    internal fun send() {
        val text = inputText
        inputText = ""
        inputRef.view?.setText("")
        inputRef.view?.blur()
        ask(text)
    }

    private fun ask(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        if (ServiceLocator.apiKey.isNullOrBlank()) {
            showSidebar = true
            bridgeModule.toast("请先设置 API Key")
            return
        }

        val seq = ++messageSeq
        messages.add(ChatMessage.user("u$seq", trimmed))
        persistMessages()
        isTyping = true
        scrollToBottom()

        chatService.ask(trimmed, messages.toList()) { reply ->
            appendAssistant(reply)
            persistMessages()
            isTyping = false
            scrollToBottom()
        }
    }

    private fun appendAssistant(reply: AiReply) {
        val seq = ++messageSeq
        messages.add(ChatMessage.assistant("a$seq", reply.blocks))
    }

    private fun persistMessages() {
        val json = messages.toSessionJson()
        acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .setItem(SESSION_STORAGE_KEY, json)
    }

    private fun restoreMessages() {
        val json = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .getItem(SESSION_STORAGE_KEY)
        if (json.isNotEmpty()) {
            messages.addAll(sessionFromJson(json))
            messageSeq = messages.size
        }
    }

    private fun scrollToBottom() {
        // 等待列表重新布局完成（contentHeight 更新为最新值）后再滚动，确保滚到最新底部
        setTimeout(50) {
            val target = if (contentHeight > 0f) contentHeight else 100000f
            listRef.view?.setContentOffset(0f, target, true)
        }
    }

    private fun openDetail(code: String) {
        val pageData = JSONObject().apply { put("code", code) }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage("stock_detail", pageData)
    }

    internal fun saveApiKey() {
        val key = apiKeyInput.trim()
        if (key.isEmpty()) {
            bridgeModule.toast("请输入 API Key")
            return
        }
        ServiceLocator.apiKey = key
        apiKeyInput = ""
        showSidebar = false
        showKeyPanel = false
        bridgeModule.toast("API Key 已保存")
    }

    internal fun toggleModel() {
        val next = if (ServiceLocator.model == "deepseek-chat") "deepseek-reasoner" else "deepseek-chat"
        ServiceLocator.model = next
        bridgeModule.toast("已切换为 $next")
    }

    internal fun clearConversation() {
        messages.clear()
        messageSeq = 0
        acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .setItem(SESSION_STORAGE_KEY, "")
        bridgeModule.toast("对话已清除")
    }
}

// ---------------------------------------------------------------------------
// 页面局部 UI 构建
// ---------------------------------------------------------------------------

private fun ViewContainer<*, *>.chatNavBar(ctx: ChatPage) {
    View {
        attr {
            paddingTop(ctx.pagerData.statusBarHeight)
            backgroundColor(StockTheme.surface)
        }
        View {
            attr {
                height(44f)
                allCenter()
            }
            Text {
                attr {
                    text("AI 投研助手")
                    fontSize(17f)
                    fontWeightSemiBold()
                    color(StockTheme.textPrimary)
                }
            }
            View {
                attr {
                    absolutePosition(top = 0f, left = StockTheme.SPACE_SM, bottom = 0f)
                    width(40f)
                    allCenter()
                }
                event { click { ctx.showSidebar = true } }
                Text {
                    attr {
                        text(AppIcons.MENU)
                        fontSize(22f)
                        color(StockTheme.textPrimary)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.emptyState() {
    View {
        attr {
            flex(1f)
            allCenter()
            paddingTop(80f)
        }
        Text {
            attr {
                text("开始提问吧，试试输入「贵州茅台」")
                fontSize(14f)
                color(StockTheme.textSecondary)
            }
        }
    }
}

private fun ViewContainer<*, *>.typingIndicator() {
    Row {
        attr {
            alignItemsFlexStart()
            justifyContentFlexStart()
            paddingTop(StockTheme.SPACE_SM)
            paddingBottom(StockTheme.SPACE_SM)
        }
        View {
            attr {
                size(34f, 34f)
                borderRadius(17f)
                allCenter()
                backgroundColor(StockTheme.primary)
                marginLeft(StockTheme.SPACE_SM)
                marginRight(StockTheme.SPACE_SM)
            }
            Text {
                attr {
                    text(AppIcons.AI_AVATAR)
                    fontSize(12f)
                    fontWeightBold()
                    color(Color.WHITE)
                }
            }
        }
        View {
            attr {
                padding(StockTheme.SPACE_MD)
                borderRadius(StockTheme.RADIUS_LG)
                backgroundColor(StockTheme.bubbleAssistant)
            }
            Text {
                attr {
                    text("正在思考…")
                    fontSize(14f)
                    color(StockTheme.textSecondary)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.inputBarDivider() {
    View {
        attr {
            height(1f)
            backgroundColor(StockTheme.divider)
        }
    }
}

private fun ViewContainer<*, *>.sidebar(ctx: ChatPage) {
    // [修复] 抽屉与遮罩都改为 vif 条件渲染，关闭时不渲染、不占布局、不遮挡底部输入框
    vif({ ctx.showSidebar }) {
        View {
            attr {
                absolutePositionAllZero()
                backgroundColor(Color(0x80000000))
            }
            event { click { ctx.showSidebar = false } }
        }
        View {
            attr {
                absolutePosition(top = 0f, left = 0f, bottom = 0f)
                width(280f)
                backgroundColor(StockTheme.surface)
                flexDirectionColumn()
            }
            Row {
                attr {
                    height(56f)
                    alignItemsCenter()
                    paddingLeft(StockTheme.SPACE_LG)
                    paddingRight(StockTheme.SPACE_LG)
                }
                Text {
                    attr {
                        text("菜单")
                        fontSize(16f)
                        fontWeightSemiBold()
                        color(StockTheme.textPrimary)
                    }
                }
                View {
                    attr {
                        absolutePosition(top = 0f, right = StockTheme.SPACE_SM, bottom = 0f)
                        width(40f)
                        allCenter()
                    }
                    event { click { ctx.showSidebar = false } }
                    Text {
                        attr {
                            text("✕")
                            fontSize(18f)
                            color(StockTheme.textSecondary)
                        }
                    }
                }
            }
            View {
                attr {
                    height(0.5f)
                    backgroundColor(StockTheme.divider)
                }
            }
            menuItem("API Key 设置") { ctx.showKeyPanel = !ctx.showKeyPanel }
            menuItem("切换模型") { ctx.toggleModel() }
            menuItem("清除对话") { ctx.clearConversation() }
            menuItem("关于") { ctx.bridgeModule.toast("Kuikly AI 投研助手 Demo") }
            View {
                attr {
                    height(0.5f)
                    backgroundColor(StockTheme.divider)
                }
            }
            vif({ ctx.showKeyPanel }) {
                keyPanel(ctx)
            }
        }
    }
}

private fun ViewContainer<*, *>.menuItem(title: String, onClick: () -> Unit) {
    Row {
        attr {
            height(48f)
            alignItemsCenter()
            paddingLeft(StockTheme.SPACE_LG)
            paddingRight(StockTheme.SPACE_LG)
        }
        event { click { onClick() } }
        Text {
            attr {
                text(title)
                fontSize(15f)
                color(StockTheme.textPrimary)
                flex(1f)
            }
        }
        Text {
            attr {
                text("›")
                fontSize(18f)
                color(StockTheme.textSecondary)
            }
        }
    }
}

private fun ViewContainer<*, *>.keyPanel(ctx: ChatPage) {
    Text {
        attr {
            text("请输入你的 DeepSeek API Key，未设置将无法使用 AI 问答。\nKey 仅保存在内存中，重启应用后需重新输入。")
            fontSize(13f)
            color(StockTheme.textSecondary)
            lineHeight(20f)
            marginLeft(StockTheme.SPACE_LG)
            marginRight(StockTheme.SPACE_LG)
            marginTop(StockTheme.SPACE_SM)
        }
    }
    View {
        attr {
            // [改动] 强制高度 52f、浅灰背景 #F5F7FA、圆角 12f，确保输入框实体可见
            height(52f)
            flexDirectionRow()
            alignItemsCenter()
            marginLeft(StockTheme.SPACE_LG)
            marginRight(StockTheme.SPACE_LG)
            marginTop(StockTheme.SPACE_MD)
            paddingLeft(StockTheme.SPACE_MD)
            paddingRight(StockTheme.SPACE_MD)
            borderRadius(StockTheme.RADIUS_MD)
            backgroundColor(Color(0xFFF5F7FA))
        }
        Input {
            attr {
                flex(1f)
                // [改动] Input 自身高度，确保渲染不被压缩
                height(52f)
                fontSize(14f)
                color(StockTheme.textPrimary)
                placeholder("请粘贴DeepSeek API Key")
                placeholderColor(StockTheme.textSecondary)
            }
            event {
                textDidChange { ctx.apiKeyInput = it.text }
            }
        }
    }
    View {
        attr {
            height(44f)
            allCenter()
            marginLeft(StockTheme.SPACE_LG)
            marginRight(StockTheme.SPACE_LG)
            marginTop(StockTheme.SPACE_MD)
            borderRadius(StockTheme.RADIUS_SM)
            backgroundColor(StockTheme.primary)
        }
        event { click { ctx.saveApiKey() } }
        Text {
            attr {
                text("保存并使用")
                fontSize(15f)
                fontWeightSemiBold()
                color(Color.WHITE)
            }
        }
    }
}

private fun ViewContainer<*, *>.chatInputBar(ctx: ChatPage) {
    View {
        attr {
            // [改动] 横向布局：输入框(左) + 发送按钮(右)
            flexDirectionRow()
            alignItemsCenter()
            padding(StockTheme.SPACE_MD)
            // [改动] 底部留白，避免被系统导航栏遮挡
            paddingBottom(StockTheme.SPACE_LG)
            backgroundColor(StockTheme.surface)
        }

        // 输入框容器（左侧，flex(1f) 占满剩余宽度）
        View {
            attr {
                flex(1f)
                // [改动] 强制高度 48f，禁止被压缩为 0
                height(48f)
                flexDirectionRow()
                alignItemsCenter()
                paddingLeft(StockTheme.SPACE_MD)
                paddingRight(StockTheme.SPACE_MD)
                // [改动] 与发送按钮间距 12f，防挤压
                marginRight(StockTheme.SPACE_MD)
                borderRadius(StockTheme.RADIUS_MD)
                // [改动] 浅灰背景 #F3F4F6，让输入框实体可见
                backgroundColor(Color(0xFFF3F4F6))
            }
            Input {
                ref { ctx.inputRef = it }
                attr {
                    flex(1f)
                    // [改动] Input 自身高度，确保渲染不被压缩
                    height(48f)
                    fontSize(15f)
                    color(StockTheme.textPrimary)
                    placeholder("请输入股票名称或代码，如：贵州茅台")
                    placeholderColor(StockTheme.textSecondary)
                    returnKeyTypeSend()
                }
                event {
                    textDidChange { ctx.inputText = it.text }
                    inputReturn { ctx.send() }
                }
            }
        }

        // 发送按钮（右侧，固定）
        View {
            attr {
                // [改动] 与输入框同高 48f
                height(48f)
                paddingLeft(StockTheme.SPACE_LG)
                paddingRight(StockTheme.SPACE_LG)
                allCenter()
                borderRadius(StockTheme.RADIUS_MD)
                backgroundColor(StockTheme.primary)
            }
            event {
                click { ctx.send() }
            }
            Text {
                attr {
                    text("发送")
                    fontSize(15f)
                    fontWeightSemiBold()
                    color(Color.WHITE)
                }
            }
        }
    }
}
