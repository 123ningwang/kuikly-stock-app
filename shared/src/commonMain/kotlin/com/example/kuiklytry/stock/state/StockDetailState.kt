package com.example.kuiklytry.stock.state

import com.example.kuiklytry.stock.model.StockDetail
import com.example.kuiklytry.stock.service.StockService
import com.tencent.kuikly.core.reactive.handler.observable

/**
 * 股票详情页的「状态层」。
 *
 * 持有详情页的可观察状态（详情数据 + 追问输入文本），页面只负责渲染与导航。
 */
internal class StockDetailState(
    private val stockService: StockService
) {
    /** 详情数据，未加载到为 null */
    var detail by observable<StockDetail?>(null)

    /** 追问输入框文本 */
    var inputText by observable("")

    /** 按股票代码加载详情 */
    fun load(code: String) {
        detail = stockService.getDetail(code)
    }
}
