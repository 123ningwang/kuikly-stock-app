package com.example.kuiklytry.stock.ui.component

import com.example.kuiklytry.stock.model.ChatBlock
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.model.ChatRole
import com.example.kuiklytry.stock.ui.component.markdown.MarkdownText
import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Column
import com.tencent.kuikly.core.views.layout.Row

/**
 * 单条聊天气泡组件。
 *
 * 区分用户 / AI 两种角色：
 *  - 用户：右侧彩色气泡，展示纯文本；
 *  - AI：左侧白色气泡 + 结构化卡片，支持 Markdown、股票卡片、迷你折线图混合渲染。
 */
internal class ChatBubbleView : ComposeView<ChatBubbleAttr, ComposeEvent>() {

    override fun createAttr(): ChatBubbleAttr = ChatBubbleAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val message = ctx.attr.message
        val isUser = message.role == ChatRole.USER
        val pageWidth = pagerData.pageViewWidth

        return {
            Row {
                attr {
                    // [修复] 撑满 List 容器宽度，给气泡一个确定的父宽，约束子元素在屏内换行
                    alignSelfStretch()
                    alignItemsFlexStart()
                    if (isUser) justifyContentFlexEnd() else justifyContentFlexStart()
                    paddingTop(StockTheme.SPACE_SM)
                    paddingBottom(StockTheme.SPACE_SM)
                }

                if (isUser) {
                    userBubble(pageWidth, message)
                } else {
                    assistantBubble(pageWidth, message, ctx.attr.onStockClick)
                }
            }
        }
    }
}

internal class ChatBubbleAttr : ComposeAttr() {
    var message: ChatMessage by observable(ChatMessage("", ChatRole.USER, emptyList()))

    /** 点击股票卡片时回调，参数为股票代码 */
    var onStockClick: ((String) -> Unit)? = null
}

internal fun ViewContainer<*, *>.ChatBubble(init: ChatBubbleView.() -> Unit) {
    addChild(ChatBubbleView(), init)
}

// ---------------------------------------------------------------------------
// 渲染实现
// ---------------------------------------------------------------------------

/** 用户消息：右侧彩色气泡，纯文本 */
private fun ViewContainer<*, *>.userBubble(pageWidth: Float, message: ChatMessage) {
    View {
        attr {
            // [修复] 用户气泡最大宽 74% 屏宽
            maxWidth(pageWidth * 0.74f)
            padding(StockTheme.SPACE_MD)
            borderRadius(StockTheme.RADIUS_LG)
            backgroundColor(StockTheme.bubbleUser)
        }
        message.blocks.forEach { block ->
            if (block is ChatBlock.Text) {
                Text {
                    attr {
                        text(block.markdown)
                        fontSize(15f)
                        color(StockTheme.bubbleUserText)
                        lineHeight(21f)
                        // [修复] 强制按容器宽度自动换行（wordWrapping），长文本不横向溢出
                        textOverFlowWordWrapping()
                    }
                }
            }
        }
    }
}

/** AI 消息：左侧内容区，按内容块类型分发渲染 */
private fun ViewContainer<*, *>.assistantBubble(
    pageWidth: Float,
    message: ChatMessage,
    onStockClick: ((String) -> Unit)?
) {
    Column {
        attr {
            // [修复] AI 内容区最大宽 82% 屏宽
            maxWidth(pageWidth * 0.82f)
            flexDirectionColumn()
        }
        message.blocks.forEach { block ->
            renderBlock(block, onStockClick, pageWidth)
        }
    }
}

private fun ViewContainer<*, *>.renderBlock(
    block: ChatBlock,
    onStockClick: ((String) -> Unit)?,
    pageWidth: Float
) {
    when (block) {
        is ChatBlock.Text -> {
            View {
                attr {
                    flexDirectionColumn()
                    padding(StockTheme.SPACE_MD)
                    borderRadius(StockTheme.RADIUS_LG)
                    backgroundColor(StockTheme.bubbleAssistant)
                }
                MarkdownText {
                    attr {
                        text = block.markdown
                        // [修复] 传入气泡可用宽度，强制 RichText 在该宽度内换行
                        maxWidth = pageWidth * 0.82f - 2 * StockTheme.SPACE_MD
                    }
                }
            }
        }

        is ChatBlock.StockCard -> {
            StockCard {
                attr {
                    stock = block.stock
                    onClick = { onStockClick?.invoke(block.stock.code) }
                }
            }
        }

        is ChatBlock.MiniChart -> {
            MiniChartCard {
                attr {
                    stock = block.stock
                    points = block.points
                    onClick = { onStockClick?.invoke(block.stock.code) }
                }
            }
        }

        is ChatBlock.TrendCard -> {
            TrendCard {
                attr {
                    direction = block.direction
                    signals = block.signals
                }
            }
        }

        is ChatBlock.RiskCard -> {
            RiskCard {
                attr {
                    level = block.level
                    warnings = block.warnings
                }
            }
        }
    }
}
