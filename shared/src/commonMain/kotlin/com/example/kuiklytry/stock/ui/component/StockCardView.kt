package com.example.kuiklytry.stock.ui.component

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
 * 股票结构化信息卡片。
 *
 * 展示股票名称、代码、最新价、涨跌幅，点击可跳转详情页。
 * 这是 AI 回复中三种内容形态之一。
 */
internal class StockCardView : ComposeView<StockCardAttr, ComposeEvent>() {

    override fun createAttr(): StockCardAttr = StockCardAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val stock = ctx.attr.stock
        val changeColor = if (stock.isUp) StockTheme.up else StockTheme.down
        val changeIcon = if (stock.isUp) AppIcons.UP else AppIcons.DOWN

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

                // 第一行：名称 + 代码 / 涨跌幅
                Row {
                    attr { alignItemsCenter() }

                    View {
                        attr {
                            // [改动] flex(1f) 权重布局：名称区弹性收缩、长名称自动换行，徽章固定靠右不被挤出屏幕
                            flex(1f)
                            flexDirectionColumn()
                        }
                        Text {
                            attr {
                                text(stock.name)
                                fontSize(16f)
                                fontWeightSemiBold()
                                color(StockTheme.textPrimary)
                            }
                        }
                        Text {
                            attr {
                                text("${stock.kindLabel} · ${stock.fullCode}")
                                fontSize(12f)
                                color(StockTheme.textSecondary)
                                marginTop(2f)
                            }
                        }
                    }

                    View {
                        attr {
                            // [改动] 徽章不设 flex（默认 0 0 auto），固定内容宽度不收缩，始终靠右
                            flexDirectionRow()
                            alignItemsCenter()
                            paddingTop(4f)
                            paddingBottom(4f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            borderRadius(6f)
                            backgroundColor(changeColor.opacity(0.1f))
                        }
                        Text {
                            attr {
                                text("$changeIcon ${stock.changePercentText}")
                                fontSize(13f)
                                fontWeightBold()
                                color(changeColor)
                            }
                        }
                    }
                }

                // 第二行：最新价 + 涨跌额
                Row {
                    attr {
                        alignItemsFlexEnd()
                        marginTop(StockTheme.SPACE_MD)
                    }
                    Text {
                        attr {
                            text(stock.priceText)
                            fontSize(28f)
                            fontWeightBold()
                            color(StockTheme.textPrimary)
                        }
                    }
                    Text {
                        attr {
                            text(stock.changeText)
                            fontSize(13f)
                            fontWeightMedium()
                            color(changeColor)
                            marginLeft(StockTheme.SPACE_SM)
                            marginBottom(4f)
                        }
                    }
                }
            }
        }
    }
}

internal class StockCardAttr : ComposeAttr() {
    var stock: StockQuote by observable(StockQuote("", "", "SH", 0.0, 0.0, 0.0))

    /** 点击回调，用于跳转详情页 */
    var onClick: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.StockCard(init: StockCardView.() -> Unit) {
    addChild(StockCardView(), init)
}
