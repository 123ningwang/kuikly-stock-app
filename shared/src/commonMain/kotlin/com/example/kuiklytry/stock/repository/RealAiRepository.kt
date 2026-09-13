package com.example.kuiklytry.stock.repository

import com.example.kuiklytry.base.Utils
import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatBlock
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.model.ChatRole
import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.model.buildInsightBlocks
import com.example.kuiklytry.stock.model.toApiContent
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 真实大模型数据源：通过 Android 原生桥请求 DeepSeek chat completions 接口。
 *
 * 行情数据仍复用 [StockRepository]（当前为 Mock），用于在识别到股票时
 * 附加「股票卡片 + 迷你折线图」内容块，保留点击跳转详情的闭环。
 */
internal class RealAiRepository(
    private val stockRepository: StockRepository,
    private val apiKeyProvider: () -> String?,
    private val modelProvider: () -> String
) : AiRepository {

    override fun reply(userInput: String, history: List<ChatMessage>, onResult: (AiReply) -> Unit) {
        if (userInput.isBlank()) {
            onResult(buildGuideReply())
            return
        }

        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            onResult(errorReply(null, "未设置 API Key，请点击右上角「设置」输入"))
            return
        }

        val matched = matchStock(userInput)

        val headers = JSONObject().apply {
            put("Content-Type", "application/json")
            put("Authorization", "Bearer $apiKey")
        }
        val messages = JSONArray()
        messages.put(
            JSONObject().apply {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            }
        )
        // 多轮上下文：完整历史（含本次用户消息，位于列表末尾）拼在 system 之后
        history.forEach { msg ->
            val content = msg.toApiContent()
            if (content.isNotBlank()) {
                messages.put(
                    JSONObject().apply {
                        put("role", if (msg.role == ChatRole.USER) "user" else "assistant")
                        put("content", content)
                    }
                )
            }
        }
        val body = JSONObject().apply {
            put("model", modelProvider())
            put("messages", messages)
            put("stream", false)
        }.toString()

        Utils.currentBridgeModule().httpRequest(ENDPOINT, "POST", headers, body, TIMEOUT_MS) { resp ->
            val status = resp?.optInt("status") ?: -1
            val data = resp?.optString("data")
            if (status in 200..299 && data != null) {
                onResult(buildReply(data, matched))
            } else {
                onResult(errorReply(data, resp?.optString("error")))
            }
        }
    }

    private fun buildReply(data: String, matched: StockDetail?): AiReply {
        val markdown = parseContent(data)
        val blocks = mutableListOf<ChatBlock>()
        blocks.add(ChatBlock.Text(markdown.ifBlank { "（模型未返回内容）" }))
        matched?.let { detail ->
            blocks.add(ChatBlock.StockCard(detail.quote))
            blocks.add(ChatBlock.MiniChart(detail.quote, detail.priceHistory.takeLast(5)))
            blocks.addAll(buildInsightBlocks(detail.quote))
        }
        return AiReply(blocks = blocks, intent = if (matched != null) "stock_query" else "chat")
    }

    private fun parseContent(data: String): String {
        return try {
            val root = JSONObject(data)
            root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractErrorMessage(data: String?): String? {
        if (data.isNullOrBlank()) return null
        return try {
            JSONObject(data).optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
    }

    private fun matchStock(userInput: String): StockDetail? {
        val normalized = userInput.trim()
        val quotes = stockRepository.getAllQuotes()
        quotes.firstOrNull { normalized.contains(it.code) }?.let { matched ->
            return stockRepository.getDetail(matched.code)
        }
        quotes.firstOrNull { normalized.contains(it.name) }?.let { matched ->
            return stockRepository.getDetail(matched.code)
        }
        return null
    }

    private fun buildGuideReply(): AiReply {
        val names = stockRepository.getAllQuotes().joinToString("、") { "**${it.name}**（${it.code}）" }
        return AiReply(
            blocks = listOf(
                ChatBlock.Text(
                    """
                    |你好，我是 **AI 投研助手** 👋
                    |
                    |我可以为你提供股票的**实时行情**、**走势图表**与 **AI 解读分析**。
                    |
                    |你可以直接输入股票代码或名称向我提问，例如：
                    |
                    |- `600519` 或「贵州茅台」
                    |- `300750` 或「宁德时代」
                    |
                    |当前 Demo 内置以下股票：$names
                    |
                    |试试发送一只股票，开启你的智能投研之旅吧。
                    """.trimMargin()
                )
            ),
            intent = "guide"
        )
    }

    private fun errorReply(data: String?, error: String?): AiReply {
        val message = extractErrorMessage(data) ?: error?.takeIf { it.isNotBlank() } ?: "未知错误"
        return AiReply(
            blocks = listOf(
                ChatBlock.Text("抱歉，请求 AI 服务失败：$message。请检查 API Key 与网络后重试。")
            ),
            intent = "error"
        )
    }

    companion object {
        private const val ENDPOINT = "https://api.deepseek.com/chat/completions"
        private const val TIMEOUT_MS = 60_000

        private const val SYSTEM_PROMPT =
            "你是一名专业的 AI 投研助手，擅长用简洁、结构化的中文 Markdown 回答股票相关问题。" +
                "回答时使用标题、列表等 Markdown 语法，语言专业但不冗长，并在涉及投资建议时提示风险、注明不构成投资建议。"
    }
}
