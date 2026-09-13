package com.example.kuiklytry.stock.repository

import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.model.StockQuote

/**
 * 股票行情数据源接口。
 *
 * 这是接入真实股票行情 API 的预留扩展点：
 * 当前由 [MockStockRepository] 提供本地模拟数据；
 * 后续接入真实行情时，实现 `RealStockRepository`（例如通过
 * `BridgeModule.ssoRequest(...)` 请求行情接口）替换即可。
 */
internal interface StockRepository {

    /**
     * 获取单只股票的基础行情快照。
     *
     * @param code 股票代码，例如 "600519"
     * @return 行情快照，未找到返回 null
     */
    fun getQuote(code: String): StockQuote?

    /**
     * 获取股票详情数据（基础行情 + 走势图 + 摘要 + AI 解读）。
     *
     * @param code 股票代码
     * @return 详情数据，未找到返回 null
     */
    fun getDetail(code: String): StockDetail?

    /**
     * 获取全部可咨询的股票行情快照（用于兜底推荐与列表展示）。
     */
    fun getAllQuotes(): List<StockQuote>
}
