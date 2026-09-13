package com.example.kuiklytry.stock.ui.component.markdown

import com.example.kuiklytry.stock.ui.theme.StockTheme
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.RichTextView
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Row

/**
 * Kuikly-Markdown 富文本渲染组件。
 *
 * 基于 Kuikly 官方 RichText/Span 组件，将 [MarkdownParser] 解析出的语法树渲染为富文本。
 *
 * 用法：
 * ```kotlin
 * MarkdownText {
 *     attr {
 *         text = markdownString
 *         maxWidth = 280f  // 可选：强制 RichText 在该宽度内换行，避免溢出
 *     }
 * }
 * ```
 */
internal class MarkdownText : ComposeView<MarkdownTextAttr, ComposeEvent>() {

    override fun createAttr(): MarkdownTextAttr = MarkdownTextAttr()

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val nodes = MarkdownParser.parse(ctx.attr.text)
        val maxWidth = ctx.attr.maxWidth
        return {
            View {
                attr {
                    flexDirectionColumn()
                    // [修复] 撑满父容器宽度，配合下方 RichText 的 maxWidth 强制换行
                    alignSelfStretch()
                }
                nodes.forEach { node -> renderNode(node, maxWidth) }
            }
        }
    }
}

internal class MarkdownTextAttr : ComposeAttr() {
    var text: String by observable("")

    /** 富文本内容最大宽度（像素）。<=0 表示不限制；聊天气泡场景请传入气泡可用宽度以强制换行 */
    var maxWidth: Float by observable(0f)
}

internal fun ViewContainer<*, *>.MarkdownText(init: MarkdownText.() -> Unit) {
    addChild(MarkdownText(), init)
}

// ---------------------------------------------------------------------------
// 渲染实现
// ---------------------------------------------------------------------------

private const val CODE_COLOR = 0xFFC7254EL

private fun ViewContainer<*, *>.renderNode(node: MarkdownNode, maxWidth: Float) {
    when (node) {
        is MarkdownNode.Heading -> renderHeading(node, maxWidth)
        is MarkdownNode.Paragraph -> renderParagraph(node, maxWidth)
        is MarkdownNode.BulletItem -> renderBullet(node, maxWidth)
        is MarkdownNode.OrderedItem -> renderOrdered(node, maxWidth)
        is MarkdownNode.Quote -> renderQuote(node, maxWidth)
        is MarkdownNode.Table -> renderTable(node, maxWidth)
        MarkdownNode.Divider -> renderDivider()
    }
}

private fun ViewContainer<*, *>.renderHeading(node: MarkdownNode.Heading, maxWidth: Float) {
    RichText {
        attr {
            marginTop(StockTheme.SPACE_MD)
            marginBottom(StockTheme.SPACE_XS)
            // [修复] 强制按宽度换行；maxWidth 让 RichText 收到有限测量宽度
            textOverFlowWordWrapping()
            if (maxWidth > 0f) maxWidth(maxWidth)
        }
        node.segments.forEach { seg ->
            Span {
                text(seg.text)
                fontSize(when (node.level) {
                    1 -> 20f
                    2 -> 18f
                    else -> 16f
                })
                fontWeightBold()
                color(StockTheme.textPrimary)
            }
        }
    }
}

private fun ViewContainer<*, *>.renderParagraph(node: MarkdownNode.Paragraph, maxWidth: Float) {
    RichText {
        attr {
            marginTop(StockTheme.SPACE_XS)
            marginBottom(StockTheme.SPACE_XS)
            // [修复] 强制按宽度换行，长段落不横向溢出
            textOverFlowWordWrapping()
            if (maxWidth > 0f) maxWidth(maxWidth)
        }
        inline(node.segments)
    }
}

private fun ViewContainer<*, *>.renderBullet(node: MarkdownNode.BulletItem, maxWidth: Float) {
    RichText {
        attr {
            marginTop(2f)
            marginBottom(2f)
            // [修复] 强制按宽度换行
            textOverFlowWordWrapping()
            if (maxWidth > 0f) maxWidth(maxWidth)
        }
        Span {
            text("•  ")
            color(StockTheme.textSecondary)
        }
        inline(node.segments)
    }
}

private fun ViewContainer<*, *>.renderOrdered(node: MarkdownNode.OrderedItem, maxWidth: Float) {
    RichText {
        attr {
            marginTop(2f)
            marginBottom(2f)
            // [修复] 强制按宽度换行
            textOverFlowWordWrapping()
            if (maxWidth > 0f) maxWidth(maxWidth)
        }
        Span {
            text("${node.number}.  ")
            color(StockTheme.primary)
        }
        inline(node.segments)
    }
}

private fun ViewContainer<*, *>.renderQuote(node: MarkdownNode.Quote, maxWidth: Float) {
    Row {
        attr {
            alignItemsFlexStart()
            marginTop(StockTheme.SPACE_SM)
            marginBottom(StockTheme.SPACE_SM)
            padding(StockTheme.SPACE_SM)
            borderRadius(StockTheme.RADIUS_SM)
            backgroundColor(StockTheme.primaryLight)
            border(Border(0.5f, BorderStyle.SOLID, StockTheme.divider))
        }
        RichText {
            attr {
                flex(1f)
                // [修复] 强制按宽度换行
                textOverFlowWordWrapping()
                if (maxWidth > 0f) maxWidth(maxWidth)
            }
            inline(node.segments, italic = true, color = StockTheme.textSecondary)
        }
    }
}

private fun ViewContainer<*, *>.renderDivider() {
    View {
        attr {
            height(1f)
            marginTop(StockTheme.SPACE_MD)
            marginBottom(StockTheme.SPACE_MD)
            backgroundColor(StockTheme.divider)
        }
    }
}

private fun ViewContainer<*, *>.renderTable(node: MarkdownNode.Table, maxWidth: Float) {
    View {
        attr {
            flexDirectionColumn()
            alignSelfStretch()
            if (maxWidth > 0f) maxWidth(maxWidth)
            marginTop(StockTheme.SPACE_SM)
            marginBottom(StockTheme.SPACE_SM)
            borderRadius(StockTheme.RADIUS_SM)
            border(Border(0.5f, BorderStyle.SOLID, StockTheme.divider))
        }
        tableRow(node.headers, isHeader = true)
        node.rows.forEach { row -> tableRow(row, isHeader = false) }
    }
}

/** 渲染表格单行：表头浅底加粗，数据行常规；各列 flex(1f) 均分宽度 */
private fun ViewContainer<*, *>.tableRow(cells: List<String>, isHeader: Boolean) {
    Row {
        attr {
            alignSelfStretch()
            if (isHeader) backgroundColor(StockTheme.primaryLight)
        }
        cells.forEach { cell ->
            View {
                attr {
                    flex(1f)
                    padding(StockTheme.SPACE_SM)
                }
                Text {
                    attr {
                        text(cell)
                        fontSize(13f)
                        color(StockTheme.textPrimary)
                        if (isHeader) fontWeightBold()
                        textOverFlowWordWrapping()
                    }
                }
            }
        }
    }
}

/**
 * 将行内片段渲染为 Span 序列，需在 [RichText] 作用域内调用。
 */
private fun RichTextView.inline(
    segments: List<InlineSegment>,
    italic: Boolean = false,
    color: Color = StockTheme.textPrimary
) {
    segments.forEach { seg ->
        if (seg.text.isEmpty()) return@forEach
        Span {
            text(seg.text)
            fontSize(15f)
            color(if (seg.code) Color(CODE_COLOR) else color)
            if (seg.bold) fontWeightBold()
            if (seg.italic || italic) fontStyleItalic()
        }
    }
}
