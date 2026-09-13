package com.example.kuiklytry.stock.repository

import com.example.kuiklytry.stock.model.AiReply
import com.example.kuiklytry.stock.model.ChatBlock
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.model.buildInsightBlocks

/**
 * AI 大模型的本地 Mock 实现。
 *
 * 通过关键字匹配模拟"AI 投研助手"的回复行为：
 * 识别到股票代码或名称时返回「Markdown 解读 + 股票卡片 + 迷你折线图」的混合内容；
 * 未识别时返回引导性文本，帮助用户发现可咨询的股票。
 *
 * 接入真实大模型后，本类整体替换为网络实现即可，返回结构保持不变。
 */
internal class MockAiRepository(
    private val stockRepository: StockRepository
) : AiRepository {

    override fun reply(userInput: String, history: List<ChatMessage>, onResult: (AiReply) -> Unit) {
        val detail = matchStock(userInput)
        onResult(if (detail != null) buildStockReply(detail) else buildGuideReply())
    }

    /**
     * 根据输入匹配股票：优先匹配代码，其次匹配名称。
     */
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

    private fun buildStockReply(detail: StockDetail): AiReply {
        val quote = detail.quote
        val blocks = mutableListOf<ChatBlock>()

        blocks.add(
            ChatBlock.Text(
                """
                |已为您查询 **${quote.name}（${quote.code}）** 的最新行情，关键信息如下：
                |
                |- 最新价：**${quote.priceText}** 元
                |- 涨跌幅：**${quote.changePercentText}**（${quote.changeText}）
                |
                |下方卡片展示了该股近 5 个交易日的走势预览，点击卡片可查看完整行情与 AI 深度解读。
                """.trimMargin()
            )
        )
        blocks.add(ChatBlock.StockCard(quote))
        blocks.add(ChatBlock.MiniChart(quote, detail.priceHistory.takeLast(5)))
        blocks.addAll(buildInsightBlocks(quote))

        return AiReply(blocks = blocks, intent = "stock_query")
    }

    private fun buildGuideReply(): AiReply {
        val quotes = stockRepository.getAllQuotes()
        val names = quotes.joinToString("、") { "**${it.name}**（${it.code}）" }
        val blocks = mutableListOf<ChatBlock>()
        blocks.add(
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
        )
        return AiReply(blocks = blocks, intent = "guide")
    }
}
