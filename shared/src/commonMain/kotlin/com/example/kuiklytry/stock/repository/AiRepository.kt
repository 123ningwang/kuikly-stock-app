package com.example.kuiklytry.stock.repository

import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatMessage

/**
 * AI 大模型问答数据源接口。
 *
 * 这是接入真实大模型 API 的预留扩展点：
 * 当前由 [RealAiRepository] 通过 Android 原生桥请求 DeepSeek；
 * 亦可用 [MockAiRepository] 提供本地模拟数据。
 *
 * 采用回调式签名，适配网络异步返回；UI 与业务层在拿到结果后回调渲染。
 */
internal interface AiRepository {

    /**
     * 根据用户输入与历史对话生成 AI 回复。
     *
     * @param userInput 用户当前输入文本
     * @param history 完整的历史聊天记录（按时间先后排列，最后一条为本次用户消息）
     * @param onResult 结果回调（Markdown 文本 + 股票卡片 + 折线图卡片等混合内容）
     */
    fun reply(userInput: String, history: List<ChatMessage>, onResult: (AiReply) -> Unit)
}
