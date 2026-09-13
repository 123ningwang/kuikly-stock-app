package com.example.kuiklytry.stock.service

import com.example.kuiklytry.stock.repository.AiRepository
import com.example.kuiklytry.stock.repository.MockStockRepository
import com.example.kuiklytry.stock.repository.RealAiRepository
import com.example.kuiklytry.stock.repository.StockRepository

/**
 * 轻量级依赖装配器。
 *
 * 集中管理各层依赖的创建与注入，UI 层通过这里获取业务服务。
 *
 * 接入真实 API 时，只需在此处把 Mock 实现替换为真实实现即可，例如：
 * ```kotlin
 * private val stockRepository: StockRepository = RealStockRepository()
 * private val aiRepository: AiRepository = RealAiRepository(stockRepository)
 * ```
 */
internal object ServiceLocator {

    // 用户当前会话的 DeepSeek API Key（仅内存，进程重启后需重新输入）
    var apiKey: String? = null

    // 当前使用的 DeepSeek 模型
    var model: String = "deepseek-chat"

    private val stockRepository: StockRepository = MockStockRepository()

    private val aiRepository: AiRepository = RealAiRepository(stockRepository, { apiKey }, { model })

    val chatService: ChatService = ChatService(aiRepository)

    val stockService: StockService = StockService(stockRepository)
}
