package com.example.kuiklytry.stock.repository

import com.example.kuiklytry.stock.model.PricePoint
import com.example.kuiklytry.stock.model.QuoteKind
import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.model.StockQuote

/**
 * 股票行情数据源的本地 Mock 实现。
 *
 * 数据为演示用途的模拟行情，接入真实 API 后整体替换本类即可。
 */
internal class MockStockRepository : StockRepository {

    private val indexLabels = listOf(
        "08-04", "08-05", "08-06", "08-07", "08-08",
        "08-11", "08-12", "08-13", "08-14", "08-15",
        "08-18", "08-19", "08-20", "08-21", "08-22",
        "08-25", "08-26", "08-27", "08-28", "08-29"
    )

    private val details: Map<String, StockDetail> = buildList {
        add(
            stockDetail(
                code = "600519", market = "SH", name = "贵州茅台",
                price = 1850.00, change = 25.00, changePercent = 1.37,
                history = listOf(1802.0, 1812.5, 1805.0, 1820.0, 1818.5, 1828.0, 1835.5, 1830.0, 1842.0, 1825.0, 1850.0),
                labels = listOf("08-18", "08-19", "08-20", "08-21", "08-22", "08-25", "08-26", "08-27", "08-28", "08-29", "08-30"),
                summary = "贵州茅台今日高开高走，主力资金净流入明显，白酒板块整体回暖，公司高端产品批价企稳回升。",
                analysis = """
                    |### 贵州茅台（600519）AI 解读
                    |
                    |**基本面**：高端白酒龙头，盈利能力行业领先，现金流充沛，具备较强的提价与渠道议价能力。
                    |
                    |**技术面**：股价近期沿 5 日均线稳步上行，成交量温和放大，短线动能偏强。
                    |
                    |**风险提示**：需关注宏观消费复苏节奏、批价波动以及政策端对高端消费的影响。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "300750", market = "SZ", name = "宁德时代",
                price = 252.30, change = -3.20, changePercent = -1.25,
                history = listOf(258.0, 260.5, 256.8, 254.0, 257.5, 255.5, 253.0, 256.0, 252.3, 250.1, 252.3),
                labels = listOf("08-18", "08-19", "08-20", "08-21", "08-22", "08-25", "08-26", "08-27", "08-28", "08-29", "08-30"),
                summary = "宁德时代今日震荡走弱，动力电池出货量环比持平，储能业务维持高景气，短期受锂价波动扰动。",
                analysis = """
                    |### 宁德时代（300750）AI 解读
                    |
                    |**基本面**：全球动力电池龙头，技术迭代与规模优势显著，储能第二增长曲线逐步放量。
                    |
                    |**技术面**：股价处于区间震荡格局，上方 260 元附近存在压力，需放量突破确认方向。
                    |
                    |**风险提示**：行业竞争加剧、原材料价格波动、海外建厂进展不及预期等风险。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "002594", market = "SZ", name = "比亚迪",
                price = 288.60, change = 6.40, changePercent = 2.27,
                history = listOf(278.0, 275.5, 279.8, 282.0, 280.5, 276.0, 281.2, 285.0, 283.5, 282.2, 288.6),
                labels = listOf("08-18", "08-19", "08-20", "08-21", "08-22", "08-25", "08-26", "08-27", "08-28", "08-29", "08-30"),
                summary = "比亚迪今日放量上涨，高端品牌与出口业务持续发力，新车型订单表现强劲，带动整车板块情绪回暖。",
                analysis = """
                    |### 比亚迪（002594）AI 解读
                    |
                    |**基本面**：新能源整车龙头，垂直一体化布局优势明显，海外销量占比持续提升。
                    |
                    |**技术面**：股价放量突破短期平台，站上多条均线，量价配合良好，趋势偏强。
                    |
                    |**风险提示**：价格战持续、海外关税政策变化、原材料成本波动等风险。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "600036", market = "SH", name = "招商银行",
                price = 36.80, change = -0.15, changePercent = -0.41,
                history = listOf(37.2, 37.0, 37.1, 36.9, 37.05, 36.95, 36.85, 36.9, 36.78, 36.95, 36.8),
                labels = listOf("08-18", "08-19", "08-20", "08-21", "08-22", "08-25", "08-26", "08-27", "08-28", "08-29", "08-30"),
                summary = "招商银行今日窄幅整理，零售金融业务稳健，净息差压力边际缓解，高股息属性对股价形成一定支撑。",
                analysis = """
                    |### 招商银行（600036）AI 解读
                    |
                    |**基本面**：零售银行龙头，资产质量优异，ROE 行业领先，高分红具备防御价值。
                    |
                    |**技术面**：股价处于箱体震荡，37 元附近为短期多空分水岭，下方支撑较强。
                    |
                    |**风险提示**：息差收窄、地产风险敞口、宏观经济复苏不及预期等风险。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "000001", market = "SH", name = "上证指数",
                price = 3400.00, change = 15.20, changePercent = 0.45,
                history = listOf(
                    3350.5, 3358.2, 3352.0, 3360.8, 3365.4,
                    3358.9, 3370.1, 3375.6, 3368.3, 3378.0,
                    3382.5, 3376.4, 3385.2, 3390.8, 3388.1,
                    3395.6, 3392.3, 3400.5, 3402.0, 3400.0
                ),
                labels = indexLabels,
                kind = QuoteKind.INDEX,
                summary = "上证指数今日震荡上行，大盘权重股集体走强，两市成交额温和放大，市场情绪回暖，沪指重新站上 3400 点整数关口。",
                analysis = """
                    |### 上证指数（000001）AI 解读
                    |
                    |**市场面**：权重蓝筹轮动发力，金融、消费等板块回暖，带动指数震荡走高。
                    |
                    |**技术面**：指数沿 5 日均线上行，量价配合良好，3400 点上方需关注持续性放量确认。
                    |
                    |**风险提示**：需关注外围市场波动、资金面变化以及政策落地节奏。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "399001", market = "SZ", name = "深证成指",
                price = 10500.00, change = 58.30, changePercent = 0.56,
                history = listOf(
                    10350.0, 10380.5, 10360.2, 10400.8, 10420.6,
                    10405.3, 10435.0, 10450.8, 10430.5, 10460.2,
                    10480.0, 10465.4, 10490.6, 10500.2, 10488.0,
                    10510.5, 10505.8, 10520.3, 10515.0, 10500.0
                ),
                labels = indexLabels,
                kind = QuoteKind.INDEX,
                summary = "深证成指今日放量上涨，成长板块表现活跃，新能源、电子等权重股集体走强，深市整体风险偏好回升。",
                analysis = """
                    |### 深证成指（399001）AI 解读
                    |
                    |**市场面**：成长风格占优，新能源、半导体、消费电子等板块共振上行，带动深市放量走强。
                    |
                    |**技术面**：指数放量站上多条均线，趋势偏强，短期关注 10500 点附近能否有效站稳。
                    |
                    |**风险提示**：成长板块波动较大，需警惕高估值品种的回调风险与资金抱团松动。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
        add(
            stockDetail(
                code = "399006", market = "SZ", name = "创业板指",
                price = 2100.00, change = -8.50, changePercent = -0.40,
                history = listOf(
                    2130.0, 2125.5, 2132.8, 2120.4, 2118.2,
                    2124.6, 2116.8, 2112.0, 2118.5, 2110.2,
                    2108.6, 2114.0, 2109.5, 2105.2, 2108.8,
                    2103.5, 2106.0, 2100.8, 2102.4, 2100.0
                ),
                labels = indexLabels,
                kind = QuoteKind.INDEX,
                summary = "创业板指今日震荡回落，高估值成长股承压，生物医药与部分赛道股调整，指数短期进入整理格局。",
                analysis = """
                    |### 创业板指（399006）AI 解读
                    |
                    |**市场面**：高估值成长品种分化，医药生物、部分新能源细分赛道调整，指数短期承压。
                    |
                    |**技术面**：指数跌破 5 日均线，2100 点附近为短期支撑，需观察能否企稳。
                    |
                    |**风险提示**：创业板波动较大，需关注流动性变化与高估值品种的估值回归风险。
                    |
                    |> 本内容由 AI 生成，仅供演示参考，不构成投资建议。
                """.trimMargin()
            )
        )
    }.associateBy { it.quote.code }

    override fun getQuote(code: String): StockQuote? = details[code]?.quote

    override fun getDetail(code: String): StockDetail? = details[code]

    override fun getAllQuotes(): List<StockQuote> = details.values.map { it.quote }

    private fun stockDetail(
        code: String,
        market: String,
        name: String,
        price: Double,
        change: Double,
        changePercent: Double,
        history: List<Double>,
        labels: List<String>,
        summary: String,
        analysis: String,
        kind: QuoteKind = QuoteKind.STOCK
    ): StockDetail {
        val points = history.mapIndexed { index, value ->
            PricePoint(labels[index % labels.size], value)
        }
        return StockDetail(
            quote = StockQuote(code, name, market, price, change, changePercent, kind),
            priceHistory = points,
            summary = summary,
            aiAnalysis = analysis
        )
    }
}
