package com.example.kuiklytry.stock.ui.component

import com.example.kuiklytry.stock.model.RiskLevel
import com.example.kuiklytry.stock.model.TrendDirection
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

/**
 * 结构化「趋势判断」卡片：方向徽章 + 信号解读列表。
 */
internal class TrendCardView : ComposeView<TrendCardAttr, ComposeEvent>() {

    override fun createAttr(): TrendCardAttr = TrendCardAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val color = when (ctx.attr.direction) {
            TrendDirection.UP -> StockTheme.up
            TrendDirection.DOWN -> StockTheme.down
            TrendDirection.FLAT -> StockTheme.textSecondary
        }
        val label = when (ctx.attr.direction) {
            TrendDirection.UP -> "看涨 ▲"
            TrendDirection.DOWN -> "看跌 ▼"
            TrendDirection.FLAT -> "震荡 ─"
        }
        return {
            View {
                attr {
                    flexDirectionColumn()
                    padding(StockTheme.SPACE_LG)
                    borderRadius(StockTheme.RADIUS_MD)
                    backgroundColor(StockTheme.surface)
                    border(Border(1f, BorderStyle.SOLID, StockTheme.divider))
                }
                Row {
                    attr { alignItemsCenter() }
                    Text {
                        attr {
                            text("趋势判断")
                            fontSize(14f)
                            fontWeightSemiBold()
                            color(StockTheme.textPrimary)
                        }
                    }
                    View {
                        attr {
                            marginLeft(StockTheme.SPACE_SM)
                            paddingTop(2f)
                            paddingBottom(2f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            borderRadius(6f)
                            backgroundColor(color.opacity(0.1f))
                        }
                        Text {
                            attr {
                                text(label)
                                fontSize(13f)
                                fontWeightBold()
                                color(color)
                            }
                        }
                    }
                }
                ctx.attr.signals.forEach { signal ->
                    Text {
                        attr {
                            text("• $signal")
                            fontSize(13f)
                            color(StockTheme.textPrimary)
                            lineHeight(20f)
                            marginTop(StockTheme.SPACE_SM)
                        }
                    }
                }
            }
        }
    }
}

internal class TrendCardAttr : ComposeAttr() {
    var direction: TrendDirection by observable(TrendDirection.FLAT)
    var signals: List<String> by observable(emptyList())
}

internal fun ViewContainer<*, *>.TrendCard(init: TrendCardView.() -> Unit) {
    addChild(TrendCardView(), init)
}

/**
 * 结构化「风险提醒」卡片：等级徽章 + 风险点列表。
 */
internal class RiskCardView : ComposeView<RiskCardAttr, ComposeEvent>() {

    override fun createAttr(): RiskCardAttr = RiskCardAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val color = when (ctx.attr.level) {
            RiskLevel.LOW -> StockTheme.down
            RiskLevel.MEDIUM -> StockTheme.primary
            RiskLevel.HIGH -> StockTheme.up
        }
        val label = when (ctx.attr.level) {
            RiskLevel.LOW -> "低风险"
            RiskLevel.MEDIUM -> "中风险"
            RiskLevel.HIGH -> "高风险"
        }
        return {
            View {
                attr {
                    flexDirectionColumn()
                    padding(StockTheme.SPACE_LG)
                    borderRadius(StockTheme.RADIUS_MD)
                    backgroundColor(StockTheme.surface)
                    border(Border(1f, BorderStyle.SOLID, StockTheme.divider))
                }
                Row {
                    attr { alignItemsCenter() }
                    Text {
                        attr {
                            text("风险提醒")
                            fontSize(14f)
                            fontWeightSemiBold()
                            color(StockTheme.textPrimary)
                        }
                    }
                    View {
                        attr {
                            marginLeft(StockTheme.SPACE_SM)
                            paddingTop(2f)
                            paddingBottom(2f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            borderRadius(6f)
                            backgroundColor(color.opacity(0.1f))
                        }
                        Text {
                            attr {
                                text(label)
                                fontSize(13f)
                                fontWeightBold()
                                color(color)
                            }
                        }
                    }
                }
                ctx.attr.warnings.forEach { warning ->
                    Text {
                        attr {
                            text("• $warning")
                            fontSize(13f)
                            color(StockTheme.textPrimary)
                            lineHeight(20f)
                            marginTop(StockTheme.SPACE_SM)
                        }
                    }
                }
            }
        }
    }
}

internal class RiskCardAttr : ComposeAttr() {
    var level: RiskLevel by observable(RiskLevel.MEDIUM)
    var warnings: List<String> by observable(emptyList())
}

internal fun ViewContainer<*, *>.RiskCard(init: RiskCardView.() -> Unit) {
    addChild(RiskCardView(), init)
}
