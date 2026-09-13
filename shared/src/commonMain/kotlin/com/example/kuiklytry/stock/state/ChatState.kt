package com.example.kuiklytry.stock.state

import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.service.ChatService
import com.example.kuiklytry.stock.service.ServiceLocator
import com.example.kuiklytry.stock.service.sessionFromJson
import com.example.kuiklytry.stock.service.toSessionJson
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList

/**
 * 聊天页的「状态层」（页面 / 组件 / 数据 / 状态 四层分离中的状态层）。
 *
 * 持有聊天页全部可观察 UI 状态与状态变更逻辑；页面只负责渲染（body）与
 * 注入 UI 副作用（滚动到底、Toast、会话持久化）。本类不依赖 ViewRef / Module / Pager，
 * 便于复用与单元测试。
 */
internal class ChatState(
    private val chatService: ChatService
) {
    /** 会话消息列表 */
    var messages by observableList<ChatMessage>()

    /** 输入框文本 */
    var inputText by observable("")

    /** 是否正在等待 AI 回复 */
    var isTyping by observable(false)

    /** 是否显示侧边抽屉 */
    var showSidebar by observable(false)

    /** 是否展开 API Key 设置面板 */
    var showKeyPanel by observable(false)

    /** API Key 输入框文本 */
    var apiKeyInput by observable("")

    /** 消息自增序号，用于生成消息 id */
    private var messageSeq = 0

    /** UI 副作用：滚动到列表底部（由页面注入） */
    var scrollToBottom: (() -> Unit)? = null

    /** UI 副作用：Toast 提示（由页面注入） */
    var toast: ((String) -> Unit)? = null

    /** 会话持久化：将序列化后的 JSON 写入本地（由页面注入） */
    var persist: ((String) -> Unit)? = null

    /** 从本地 JSON 恢复会话历史 */
    fun restore(json: String) {
        if (json.isNotEmpty()) {
            messages.addAll(sessionFromJson(json))
            messageSeq = messages.size
        }
    }

    /** 首次进入时拉取欢迎语（仅无历史时调用） */
    fun welcome() {
        chatService.welcome { appendAssistant(it) }
    }

    /** 消费详情页「继续追问」传回的待发送问题，消费后即清空 */
    fun consumePendingQuestion(): String? {
        val pending = ServiceLocator.pendingQuestion
        if (!pending.isNullOrBlank()) {
            ServiceLocator.pendingQuestion = null
            return pending
        }
        return null
    }

    /** 发起一次提问 */
    fun ask(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (ServiceLocator.apiKey.isNullOrBlank()) {
            showSidebar = true
            toast?.invoke("请先设置 API Key")
            return
        }
        val seq = ++messageSeq
        messages.add(ChatMessage.user("u$seq", trimmed))
        persistMessages()
        isTyping = true
        scrollToBottom?.invoke()
        chatService.ask(trimmed, messages.toList()) { reply ->
            appendAssistant(reply)
            persistMessages()
            isTyping = false
            scrollToBottom?.invoke()
        }
    }

    /** 保存 API Key */
    fun saveApiKey() {
        val key = apiKeyInput.trim()
        if (key.isEmpty()) {
            toast?.invoke("请输入 API Key")
            return
        }
        ServiceLocator.apiKey = key
        apiKeyInput = ""
        showSidebar = false
        showKeyPanel = false
        toast?.invoke("API Key 已保存")
    }

    /** 切换 DeepSeek 模型 */
    fun toggleModel() {
        val next = if (ServiceLocator.model == "deepseek-chat") "deepseek-reasoner" else "deepseek-chat"
        ServiceLocator.model = next
        toast?.invoke("已切换为 $next")
    }

    /** 清空会话 */
    fun clearConversation() {
        messages.clear()
        messageSeq = 0
        persist?.invoke("")
    }

    private fun appendAssistant(reply: AiReply) {
        val seq = ++messageSeq
        messages.add(ChatMessage.assistant("a$seq", reply.blocks))
    }

    private fun persistMessages() {
        persist?.invoke(messages.toSessionJson())
    }
}
