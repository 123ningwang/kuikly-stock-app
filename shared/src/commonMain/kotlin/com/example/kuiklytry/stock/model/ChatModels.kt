package com.example.kuiklytry.stock.model

/**
 * 聊天消息角色。
 */
enum class ChatRole {
    /** 用户 */
    USER,

    /** AI 助手 */
    ASSISTANT
}

/**
 * AI 回复中的内容块（混合渲染的核心抽象）。
 *
 * 一条 AI 回复可以由多种内容块组合而成：
 *  - [Text]：Markdown 富文本
 *  - [StockCard]：股票结构化信息卡片
 *  - [MiniChart]：近 N 日股价走势迷你折线图卡片
 *
 * 使用 sealed class 方便后续扩展新的内容形态（例如表格、图片、K 线等），
 * 渲染层通过 when 分支分发，符合开闭原则。
 */
/** 趋势方向 */
enum class TrendDirection {
    UP,
    DOWN,
    FLAT
}

/** 风险等级 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

sealed class ChatBlock {

    /** Markdown 富文本块 */
    data class Text(val markdown: String) : ChatBlock()

    /** 股票信息卡片块，可点击跳转详情页 */
    data class StockCard(val stock: StockQuote) : ChatBlock()

    /** 迷你折线图卡片块，可点击跳转详情页 */
    data class MiniChart(
        val stock: StockQuote,
        val points: List<PricePoint>
    ) : ChatBlock()

    /** 趋势判断卡片块（方向 + 信号解读） */
    data class TrendCard(
        val direction: TrendDirection,
        val signals: List<String>
    ) : ChatBlock()

    /** 风险提醒卡片块（等级 + 风险点） */
    data class RiskCard(
        val level: RiskLevel,
        val warnings: List<String>
    ) : ChatBlock()
}

/**
 * 一条完整的聊天消息。
 *
 * 用户消息通常只包含一个 [ChatBlock.Text]；
 * AI 消息可以包含多个混合 [ChatBlock]。
 */
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val blocks: List<ChatBlock>,
    val timestamp: Long = 0L
) {
    companion object {
        /** 构造一条纯文本用户消息 */
        fun user(id: String, text: String, timestamp: Long = 0L): ChatMessage =
            ChatMessage(id, ChatRole.USER, listOf(ChatBlock.Text(text)), timestamp)

        /** 构造一条 AI 消息 */
        fun assistant(id: String, blocks: List<ChatBlock>, timestamp: Long = 0L): ChatMessage =
            ChatMessage(id, ChatRole.ASSISTANT, blocks, timestamp)
    }
}

/**
 * 大模型回复的结果封装。
 *
 * 除正文内容块外，预留了后续接入真实模型时可能需要的字段，
 * 例如意图识别、引用来源、追问建议等。
 */
data class AiReply(
    val blocks: List<ChatBlock>,
    val intent: String? = null,
    val followUpQuestions: List<String> = emptyList()
)

/**
 * 将一条聊天消息转换为发送给大模型的历史文本内容。
 *
 * 卡片 / 图表等结构化块不含可复述的文本，历史上下文仅拼接 Markdown 文本块；
 * 用于多轮对话时让模型记住之前的问答内容。
 */
internal fun ChatMessage.toApiContent(): String =
    blocks.filterIsInstance<ChatBlock.Text>().joinToString("\n") { it.markdown }

/**
 * 基于行情快照生成结构化「趋势判断 + 风险提醒」内容块。
 *
 * Mock 与真实 AI 模式共用，让「趋势判断 / 风险提醒 / 信号解读」能力
 * 以确定性卡片形式呈现（不依赖大模型自由发挥），保证演示稳定。
 */
internal fun buildInsightBlocks(quote: StockQuote): List<ChatBlock> {
    val trend = when {
        quote.changePercent > 1.0 -> ChatBlock.TrendCard(
            TrendDirection.UP,
            listOf("短线动能偏强", "量价配合良好", "站上短期均线")
        )
        quote.changePercent < -1.0 -> ChatBlock.TrendCard(
            TrendDirection.DOWN,
            listOf("短线承压回落", "跌破短期均线", "资金净流出")
        )
        else -> ChatBlock.TrendCard(
            TrendDirection.FLAT,
            listOf("区间震荡整理", "方向尚待确认", "多空力量均衡")
        )
    }
    val risk = when {
        quote.changePercent > 1.0 -> ChatBlock.RiskCard(
            RiskLevel.LOW,
            listOf("注意追高风险", "关注获利回吐压力")
        )
        quote.changePercent < -1.0 -> ChatBlock.RiskCard(
            RiskLevel.HIGH,
            listOf("下行趋势未止", "建议控制仓位")
        )
        else -> ChatBlock.RiskCard(
            RiskLevel.MEDIUM,
            listOf("方向不明", "轻仓观望为宜")
        )
    }
    return listOf(trend, risk)
}
