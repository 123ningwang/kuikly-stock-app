package com.example.kuiklytry.stock.service

import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.model.StockQuote
import com.example.kuiklytry.stock.repository.StockRepository

/**
 * 股票业务逻辑层。
 *
 * 对行情数据做业务组装，UI 层只依赖本层，不直接接触数据源。
 * 后续可在此补充：
 *  - 行情轮询/订阅与缓存
 *  - 涨跌停、复权等业务规则
 *  - 自选股管理等
 */
internal class StockService(
    private val stockRepository: StockRepository
) {

    /**
     * 获取股票详情（基础行情 + 走势图 + 摘要 + AI 解读）。
     */
    fun getDetail(code: String): StockDetail? = stockRepository.getDetail(code)

    /**
     * 获取全部可咨询股票的基础行情快照。
     */
    fun getQuotes(): List<StockQuote> = stockRepository.getAllQuotes()
}
