package com.example.kuiklytry.stock.model

import kotlin.math.roundToLong

/**
 * 行情标的类型：股票 or 指数。
 */
enum class QuoteKind {
    STOCK,
    INDEX
}

/**
 * 股票 / 指数基础行情快照（结构化信息卡片与详情页共用）。
 */
data class StockQuote(
    val code: String,
    val name: String,
    val market: String = "SH",
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val kind: QuoteKind = QuoteKind.STOCK
) {
    /** 是否上涨（A 股习惯：红涨绿跌） */
    val isUp: Boolean get() = change >= 0

    /** 是否为指数标的 */
    val isIndex: Boolean get() = kind == QuoteKind.INDEX

    /** 类型标签，如「股票」「指数」 */
    val kindLabel: String get() = if (isIndex) "指数" else "股票"

    /** 现价文本，例如 "1850.00" */
    val priceText: String get() = price.format(2)

    /** 涨跌额文本，例如 "+25.00" */
    val changeText: String get() = (if (isUp) "+" else "") + change.format(2)

    /** 涨跌幅文本，例如 "+1.37%" */
    val changePercentText: String get() = (if (isUp) "+" else "") + changePercent.format(2) + "%"

    /** 带交易所前缀的完整代码，例如 "SH600519" */
    val fullCode: String get() = "$market$code"
}

/**
 * 股价走势中的一个数据点。
 */
data class PricePoint(
    val label: String,
    val price: Double
)

/**
 * 股票详情页数据。
 *
 * 包含基础行情、走势图数据、行情摘要与 AI 解读分析文本。
 */
data class StockDetail(
    val quote: StockQuote,
    val priceHistory: List<PricePoint>,
    val summary: String,
    val aiAnalysis: String
)

/**
 * 将 Double 格式化为保留 [decimals] 位小数的字符串。
 *
 * 注意：Kotlin Multiplatform 下（尤其 JS / Native）不可使用 String.format，
 * 这里采用纯算术 + 字符串拼接实现，保证跨端一致。
 */
internal fun Double.format(decimals: Int): String {
    val factor = pow10(decimals)
    val rounded = (this * factor).roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val abs = if (rounded < 0) -rounded else rounded
    val intPart = abs / factor
    val decPart = abs % factor
    return if (decimals == 0) {
        sign + intPart.toString()
    } else {
        sign + intPart.toString() + "." + decPart.toString().padStart(decimals, '0')
    }
}

private fun pow10(n: Int): Long {
    var result = 1L
    repeat(n) { result *= 10 }
    return result
}
