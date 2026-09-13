package com.example.kuiklytry.stock.service

import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.repository.AiRepository

/**
 * 聊天业务逻辑层。
 *
 * 负责编排"用户提问 → 大模型回复"的核心流程，是 UI 层与数据源层之间的桥梁。
 * 后续可在此层补充更多业务规则，例如：
 *  - 上下文/多轮对话拼接
 *  - 敏感词过滤、意图路由
 *  - 回复结果的后处理与缓存
 */
internal class ChatService(
    private val aiRepository: AiRepository
) {

    /**
     * 向 AI 提问并获取结构化回复，携带完整历史聊天记录以支持多轮追问。
     *
     * @param history 完整历史消息（最后一条为本次用户消息）
     */
    fun ask(userInput: String, history: List<ChatMessage>, onResult: (AiReply) -> Unit) =
        aiRepository.reply(userInput, history, onResult)

    /**
     * 获取首次进入聊天页的欢迎语（无历史上下文）。
     */
    fun welcome(onResult: (AiReply) -> Unit) = aiRepository.reply("", emptyList(), onResult)
}
