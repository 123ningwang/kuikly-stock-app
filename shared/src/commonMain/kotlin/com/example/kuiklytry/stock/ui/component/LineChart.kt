package com.example.kuiklytry.stock.ui.component

import com.example.kuiklytry.stock.model.PricePoint
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext

/**
 * 可复用的股价折线图组件（基于 Kuikly 官方 Canvas 组件绘制）。
 *
 * 迷你走势卡片与详情页图表均复用本组件，保证视觉一致。
 *
 * 用法：
 * ```kotlin
 * LineChart {
 *     attr {
 *         points = priceHistory
 *         lineColor = StockTheme.up
 *     }
 * }
 * ```
 */
internal class LineChart : ComposeView<LineChartAttr, ComposeEvent>() {

    override fun createAttr(): LineChartAttr = LineChartAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas({
                attr {
                    // [修复] flex(1f) 撑满高度 + alignSelfStretch() 撑满宽度，
                    // 否则 Canvas 在列布局中宽度为 0，drawPolylineChart 提前 return，折线不渲染
                    flex(1f)
                    alignSelfStretch()
                }
            }) { context, width, height ->
                drawPolylineChart(
                    points = ctx.attr.points,
                    lineColor = ctx.attr.lineColor,
                    showArea = ctx.attr.showArea,
                    lineWidth = ctx.attr.lineWidth,
                    context = context,
                    width = width,
                    height = height
                )
            }
        }
    }
}

internal class LineChartAttr : ComposeAttr() {
    /** 走势数据点 */
    var points: List<PricePoint> by observable(emptyList())

    /** 折线颜色 */
    var lineColor: Color by observable(StockTheme.up)

    /** 是否绘制面积填充 */
    var showArea: Boolean by observable(true)

    /** 线宽 */
    var lineWidth: Float by observable(2f)
}

internal fun ViewContainer<*, *>.LineChart(init: LineChart.() -> Unit) {
    addChild(LineChart(), init)
}

/**
 * 折线图核心绘制逻辑，供组件与后续扩展复用。
 */
internal fun drawPolylineChart(
    points: List<PricePoint>,
    lineColor: Color,
    showArea: Boolean,
    lineWidth: Float,
    context: CanvasContext,
    width: Float,
    height: Float
) {
    if (points.size < 2 || width <= 0f || height <= 0f) return

    val minPrice = points.minOf { it.price }
    val maxPrice = points.maxOf { it.price }
    val range = (maxPrice - minPrice).let { if (it <= 0.0) 1.0 else it }

    // 上下各留 8% 的呼吸空间，避免折线贴边
    val padTop = height * 0.08f
    val padBottom = height * 0.08f
    val padX = 6f
    val chartHeight = height - padTop - padBottom

    fun xOf(index: Int): Float = padX + (width - padX * 2) * index / (points.size - 1)

    fun yOf(price: Double): Float =
        padTop + chartHeight * (1f - ((price - minPrice) / range).toFloat())

    // 面积填充
    if (showArea) {
        val gradient = context.createLinearGradient(0f, 0f, 0f, height)
        gradient.addColorStop(0f, lineColor.opacity(0.22f))
        gradient.addColorStop(1f, lineColor.opacity(0f))
        context.beginPath()
        context.moveTo(xOf(0), height)
        context.lineTo(xOf(0), yOf(points[0].price))
        for (i in 1 until points.size) {
            context.lineTo(xOf(i), yOf(points[i].price))
        }
        context.lineTo(xOf(points.lastIndex), height)
        context.closePath()
        context.fillStyle(gradient)
        context.fill()
    }

    // 折线
    context.beginPath()
    context.moveTo(xOf(0), yOf(points[0].price))
    for (i in 1 until points.size) {
        context.lineTo(xOf(i), yOf(points[i].price))
    }
    context.strokeStyle(lineColor)
    context.lineWidth(lineWidth)
    context.lineCapRound()
    context.stroke()
}
