package com.example.kuiklytry.stock.ui.page

import com.example.kuiklytry.base.BasePager
import com.example.kuiklytry.stock.service.ServiceLocator
import com.example.kuiklytry.stock.state.StockDetailState
import com.example.kuiklytry.stock.ui.component.AppIcons
import com.example.kuiklytry.stock.ui.component.LineChart
import com.example.kuiklytry.stock.ui.component.markdown.MarkdownText
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

/**
 * 页面 3：股票详情承接页。
 *
 * 从聊天页点击股票卡片进入，展示基础行情、走势图表、行情摘要与 AI 解读分析，
 * 底部提供追问输入框，发送后回到聊天页继续对话。
 * 股票代码通过路由参数 `code` 传入，数据由 [StockDetailState]（状态层）加载自 [ServiceLocator.stockService]。
 */
@Page("stock_detail", supportInLocal = true)
internal class StockDetailPage : BasePager() {

    internal val state = StockDetailState(ServiceLocator.stockService)

    internal lateinit var inputRef: ViewRef<InputView>

    override fun created() {
        super.created()
        state.load(pageData.params.optString("code"))
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(StockTheme.background) }

            detailNavBar(ctx)

            vif({ ctx.state.detail != null }) {
                detailContent(ctx)
            }
            vif({ ctx.state.detail == null }) {
                notFound()
            }

            detailInputDivider()
            detailInputBar(ctx)
        }
    }

    internal fun closePage() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    internal fun sendQuestion() {
        val text = state.inputText.trim()
        if (text.isEmpty()) return
        state.inputText = ""
        inputRef.view?.setText("")
        inputRef.view?.blur()
        // 关闭详情页回退到聊天页，问题经 ServiceLocator 传递，由 ChatPage.pageDidAppear 消费并发送
        ServiceLocator.pendingQuestion = text
        closePage()
    }
}

// ---------------------------------------------------------------------------
// 页面局部 UI 构建
// ---------------------------------------------------------------------------

private fun ViewContainer<*, *>.detailNavBar(ctx: StockDetailPage) {
    View {
        attr {
            paddingTop(ctx.pagerData.statusBarHeight)
            backgroundColor(StockTheme.surface)
        }
        View {
            attr {
                height(44f)
                allCenter()
            }
            // 返回按钮
            View {
                attr {
                    absolutePosition(left = StockTheme.SPACE_MD, top = 0f, bottom = 0f)
                    width(40f)
                    allCenter()
                }
                event { click { ctx.closePage() } }
                Text {
                    attr {
                        text(AppIcons.BACK)
                        fontSize(26f)
                        color(StockTheme.textPrimary)
                    }
                }
            }
            Text {
                attr {
                    text(if (ctx.state.detail?.quote?.isIndex == true) "指数详情" else "股票详情")
                    fontSize(17f)
                    fontWeightSemiBold()
                    color(StockTheme.textPrimary)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.detailContent(ctx: StockDetailPage) {
    val detail = ctx.state.detail ?: return
    val quote = detail.quote
    val changeColor = if (quote.isUp) StockTheme.up else StockTheme.down
    val changeIcon = if (quote.isUp) AppIcons.UP else AppIcons.DOWN
    // [改动] 价格字号随长度自适应缩小，超长价格（异常大额）不再横向溢出
    val priceFontSize = when {
        quote.priceText.length > 10 -> 24f
        quote.priceText.length > 7 -> 30f
        else -> 36f
    }

    Scroller {
        attr {
            flex(1f)
            showScrollerIndicator(false)
        }

        // 基础行情卡片
        View {
            attr {
                flexDirectionColumn()
                padding(StockTheme.SPACE_XL)
                backgroundColor(StockTheme.surface)
            }
            Row {
                attr { alignItemsCenter() }
                Text {
                    attr {
                        text(quote.name)
                        fontSize(20f)
                        fontWeightBold()
                        color(StockTheme.textPrimary)
                    }
                }
                Text {
                    attr {
                        text(quote.fullCode)
                        fontSize(13f)
                        color(StockTheme.textSecondary)
                        marginLeft(StockTheme.SPACE_SM)
                    }
                }
            }
            Row {
                attr {
                    alignItemsFlexEnd()
                    marginTop(StockTheme.SPACE_MD)
                    // [改动] 总宽超屏时徽章自动换行，兜底避免横向溢出
                    flexWrapWrap()
                }
                Text {
                    attr {
                        text(quote.priceText)
                        fontSize(priceFontSize)
                        fontWeightBold()
                        color(changeColor)
                        // [改动] 价格超长时换行，配合字号缩小双重兜底
                        maxWidth(ctx.pagerData.pageViewWidth * 0.55f)
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginLeft(StockTheme.SPACE_MD)
                        marginBottom(6f)
                        paddingTop(4f)
                        paddingBottom(4f)
                        paddingLeft(8f)
                        paddingRight(8f)
                        borderRadius(6f)
                        backgroundColor(changeColor.opacity(0.1f))
                    }
                    Text {
                        attr {
                            text("$changeIcon ${quote.changePercentText}")
                            fontSize(14f)
                            fontWeightBold()
                            color(changeColor)
                        }
                    }
                    Text {
                        attr {
                            text(quote.changeText)
                            fontSize(12f)
                            color(changeColor)
                            marginLeft(6f)
                        }
                    }
                }
            }
        }

        // 走势图表卡片
        sectionCard(if (detail.quote.isIndex) "指数走势" else "股价走势") {
            View {
                attr {
                    // [修复] 撑满卡片宽度，确保内部 LineChart 的 Canvas 拿到非零宽度
                    alignSelfStretch()
                    height(180f)
                    marginTop(StockTheme.SPACE_SM)
                }
                LineChart {
                    attr {
                        // [修复] alignSelfStretch() 撑满宽度 + flex(1f) 撑满高度，
                        // 否则 LineChartView 高度塌陷为 0，内部 Canvas 的宽/高算成 0，触发 drawPolylineChart 提前 return
                        alignSelfStretch()
                        flex(1f)
                        points = detail.priceHistory
                        lineColor = changeColor
                        showArea = true
                        lineWidth = 2f
                    }
                }
            }
            Row {
                attr {
                    justifyContentSpaceBetween()
                    marginTop(StockTheme.SPACE_SM)
                }
                Text {
                    attr {
                        text(detail.priceHistory.firstOrNull()?.label ?: "")
                        fontSize(11f)
                        color(StockTheme.textSecondary)
                    }
                }
                Text {
                    attr {
                        text(detail.priceHistory.lastOrNull()?.label ?: "")
                        fontSize(11f)
                        color(StockTheme.textSecondary)
                    }
                }
            }
        }

        // 行情摘要卡片
        sectionCard("行情摘要") {
            Text {
                attr {
                    text(detail.summary)
                    fontSize(14f)
                    color(StockTheme.textPrimary)
                    lineHeight(22f)
                    marginTop(StockTheme.SPACE_SM)
                }
            }
        }

        // AI 解读分析卡片
        sectionCard("AI 解读") {
            MarkdownText {
                attr { text = detail.aiAnalysis }
            }
        }

        // 底部留白
        View { attr { height(StockTheme.SPACE_XL) } }
    }
}

/** 通用分区卡片 */
private fun ViewContainer<*, *>.sectionCard(title: String, content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            flexDirectionColumn()
            marginTop(StockTheme.SPACE_MD)
            marginLeft(StockTheme.SPACE_MD)
            marginRight(StockTheme.SPACE_MD)
            padding(StockTheme.SPACE_LG)
            borderRadius(StockTheme.RADIUS_MD)
            backgroundColor(StockTheme.surface)
        }
        Text {
            attr {
                text(title)
                fontSize(16f)
                fontWeightSemiBold()
                color(StockTheme.textPrimary)
            }
        }
        content.invoke(this)
    }
}

private fun ViewContainer<*, *>.notFound() {
    View {
        attr {
            flex(1f)
            allCenter()
        }
        Text {
            attr {
                text("未找到该股票信息")
                fontSize(15f)
                color(StockTheme.textSecondary)
            }
        }
    }
}

private fun ViewContainer<*, *>.detailInputDivider() {
    View {
        attr {
            height(0.5f)
            backgroundColor(StockTheme.divider)
        }
    }
}

private fun ViewContainer<*, *>.detailInputBar(ctx: StockDetailPage) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(StockTheme.SPACE_MD)
            paddingBottom(StockTheme.SPACE_LG)
            backgroundColor(StockTheme.surface)
        }

        View {
            attr {
                flex(1f)
                height(40f)
                flexDirectionRow()
                alignItemsCenter()
                paddingLeft(StockTheme.SPACE_MD)
                paddingRight(StockTheme.SPACE_MD)
                borderRadius(20f)
                backgroundColor(StockTheme.background)
            }
            Input {
                ref { ctx.inputRef = it }
                attr {
                    flex(1f)
                    fontSize(15f)
                    color(StockTheme.textPrimary)
                    placeholder("继续追问，如：这只股票还能买吗？")
                    placeholderColor(StockTheme.textSecondary)
                    returnKeyTypeSend()
                }
                event {
                    textDidChange { ctx.state.inputText = it.text }
                    inputReturn { ctx.sendQuestion() }
                }
            }
        }

        View {
            attr {
                size(40f, 40f)
                borderRadius(20f)
                allCenter()
                marginLeft(StockTheme.SPACE_SM)
                backgroundColor(StockTheme.primary)
            }
            event {
                click { ctx.sendQuestion() }
            }
            Text {
                attr {
                    text(AppIcons.SEND)
                    fontSize(18f)
                    color(Color.WHITE)
                }
            }
        }
    }
}
