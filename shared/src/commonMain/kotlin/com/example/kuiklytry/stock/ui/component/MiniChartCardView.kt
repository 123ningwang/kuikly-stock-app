package com.example.kuiklytry.stock.ui.component

import com.example.kuiklytry.stock.model.PricePoint
import com.example.kuiklytry.stock.model.StockQuote
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

/**
 * 迷你折线图卡片。
 *
 * 展示股票名称、最新价与近 5 日走势预览，点击可跳转详情页。
 * 这是 AI 回复中三种内容形态之一。
 */
internal class MiniChartCardView : ComposeView<MiniChartCardAttr, ComposeEvent>() {

    override fun createAttr(): MiniChartCardAttr = MiniChartCardAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val stock = ctx.attr.stock
        val changeColor = if (stock.isUp) StockTheme.up else StockTheme.down

        return {
            View {
                attr {
                    flexDirectionColumn()
                    padding(StockTheme.SPACE_LG)
                    borderRadius(StockTheme.RADIUS_MD)
                    backgroundColor(StockTheme.surface)
                    border(Border(1f, BorderStyle.SOLID, StockTheme.divider))
                }
                event {
                    click { ctx.attr.onClick?.invoke() }
                }

                // 头部：名称 + 代码 / 最新价 + 涨跌幅
                Row {
                    attr { alignItemsCenter() }

                    View {
                        attr {
                            // [改动] 左侧文字区 flex(1f) 自适应收缩，给右侧价格/涨跌幅留出固定空间，长名称不溢出
                            flex(1f)
                            flexDirectionColumn()
                        }
                        Text {
                            attr {
                                text(stock.name)
                                fontSize(15f)
                                fontWeightSemiBold()
                                color(StockTheme.textPrimary)
                            }
                        }
                        Text {
                            attr {
                                text("${stock.kindLabel} · ${stock.fullCode}")
                                fontSize(11f)
                                color(StockTheme.textSecondary)
                                marginTop(2f)
                            }
                        }
                    }

                    View {
                        attr { flexDirectionColumn() }
                        Text {
                            attr {
                                text(stock.priceText)
                                fontSize(16f)
                                fontWeightBold()
                                color(StockTheme.textPrimary)
                            }
                        }
                        Text {
                            attr {
                                text(stock.changePercentText)
                                fontSize(12f)
                                fontWeightMedium()
                                color(changeColor)
                                marginTop(2f)
                            }
                        }
                    }
                }

                // 近 5 日走势预览
                View {
                    attr {
                        height(72f)
                        marginTop(StockTheme.SPACE_MD)
                    }
                    LineChart {
                        attr {
                            points = ctx.attr.points
                            lineColor = changeColor
                            showArea = true
                            lineWidth = 2f
                        }
                    }
                }

                // 底部提示
                Text {
                    attr {
                        // [改动] 撑满父容器宽度，文本默认自动换行，窄屏下不横向溢出
                        alignSelfStretch()
                        text("近 ${ctx.attr.points.size} 个交易日走势 · 点击查看详情")
                        fontSize(11f)
                        color(StockTheme.textSecondary)
                        marginTop(StockTheme.SPACE_SM)
                    }
                }
            }
        }
    }
}

internal class MiniChartCardAttr : ComposeAttr() {
    var stock: StockQuote by observable(StockQuote("", "", "SH", 0.0, 0.0, 0.0))
    var points: List<PricePoint> by observable(emptyList())

    /** 点击回调，用于跳转详情页 */
    var onClick: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.MiniChartCard(init: MiniChartCardView.() -> Unit) {
    addChild(MiniChartCardView(), init)
}
