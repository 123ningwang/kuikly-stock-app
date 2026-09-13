package com.example.kuiklytry.stock.ui.component.markdown

/**
 * Markdown 语法树的节点类型（块级）。
 */
internal sealed class MarkdownNode {

    /** 标题 */
    data class Heading(val level: Int, val segments: List<InlineSegment>) : MarkdownNode()

    /** 普通段落 */
    data class Paragraph(val segments: List<InlineSegment>) : MarkdownNode()

    /** 无序列表项 */
    data class BulletItem(val segments: List<InlineSegment>) : MarkdownNode()

    /** 有序列表项 */
    data class OrderedItem(val number: Int, val segments: List<InlineSegment>) : MarkdownNode()

    /** 引用块 */
    data class Quote(val segments: List<InlineSegment>) : MarkdownNode()

    /** 分割线 */
    object Divider : MarkdownNode()

    /** 表格（表头 + 数据行） */
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownNode()
}

/**
 * 行内文本片段，携带简单的富文本样式标记。
 */
internal data class InlineSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false
)

/**
 * 轻量级 Markdown 解析器。
 *
 * 覆盖 Demo 所需的常用语法：标题、粗体、斜体、行内代码、无序/有序列表、引用、分割线。
 * 使用 Kuikly 官方 RichText/Span 组件进行渲染，是 "Kuikly-Markdown" 能力的落地实现。
 */
internal object MarkdownParser {

    fun parse(markdown: String): List<MarkdownNode> {
        val nodes = mutableListOf<MarkdownNode>()
        val lines = markdown.split("\n")

        var orderIndex = 0
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trimEnd()

            // 表格：表头行（含 |）紧跟分隔行（| --- |），随后连续数据行
            if (isTableRow(line) && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                nodes.add(
                    MarkdownNode.Table(
                        headers = parseTableRow(line),
                        rows = buildList {
                            var j = i + 2
                            while (j < lines.size && isTableRow(lines[j])) {
                                add(parseTableRow(lines[j]))
                                j += 1
                            }
                        }
                    )
                )
                // 跳过表头、分隔行与所有数据行
                i = i + 2
                while (i < lines.size && isTableRow(lines[i])) i += 1
                orderIndex = 0
                continue
            }

            when {
                line.isBlank() -> Unit // 空行仅作为分隔，不产生节点

                isDivider(line) -> {
                    nodes.add(MarkdownNode.Divider)
                    orderIndex = 0
                }

                line.startsWith("### ") -> {
                    nodes.add(MarkdownNode.Heading(3, parseInline(line.substring(4))))
                    orderIndex = 0
                }

                line.startsWith("## ") -> {
                    nodes.add(MarkdownNode.Heading(2, parseInline(line.substring(3))))
                    orderIndex = 0
                }

                line.startsWith("# ") -> {
                    nodes.add(MarkdownNode.Heading(1, parseInline(line.substring(2))))
                    orderIndex = 0
                }

                line.startsWith("> ") -> {
                    nodes.add(MarkdownNode.Quote(parseInline(line.substring(2))))
                    orderIndex = 0
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    nodes.add(MarkdownNode.BulletItem(parseInline(line.substring(2))))
                    orderIndex = 0
                }

                isOrderedItem(line) -> {
                    orderIndex += 1
                    val content = line.substringAfter(". ").ifBlank { line.substringAfter('.') }
                    nodes.add(MarkdownNode.OrderedItem(orderIndex, parseInline(content)))
                }

                else -> {
                    nodes.add(MarkdownNode.Paragraph(parseInline(line)))
                    orderIndex = 0
                }
            }
            i += 1
        }
        return nodes
    }

    private fun isDivider(line: String): Boolean {
        val t = line.trim()
        return t == "---" || t == "***" || t == "___"
    }

    private fun isOrderedItem(line: String): Boolean {
        val t = line.trim()
        val dotIndex = t.indexOf('.')
        if (dotIndex <= 0 || dotIndex + 1 >= t.length) return false
        val prefix = t.substring(0, dotIndex)
        if (!prefix.all { it.isDigit() }) return false
        return t[dotIndex + 1] == ' '
    }

    /** 是否为表格行（以 | 开头且以 | 结尾） */
    private fun isTableRow(line: String): Boolean {
        val t = line.trim()
        return t.startsWith("|") && t.endsWith("|") && t.length > 1
    }

    /** 是否为表格分隔行（如 | --- | :---: | ---: |） */
    private fun isTableSeparator(line: String): Boolean {
        val t = line.trim()
        if (!t.startsWith("|") || !t.endsWith("|")) return false
        val cells = t.substring(1, t.length - 1).split("|")
        if (cells.isEmpty()) return false
        return cells.all { cell ->
            val c = cell.trim()
            c.isNotEmpty() && c.all { it == '-' || it == ':' }
        }
    }

    /** 解析表格行为单元格列表 */
    private fun parseTableRow(line: String): List<String> {
        val t = line.trim()
        return t.substring(1, t.length - 1).split("|").map { it.trim() }
    }

    /**
     * 解析行内语法：`**粗体**`、`*斜体*`、`` `代码` ``。
     */
    private fun parseInline(text: String): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        val buffer = StringBuilder()

        var bold = false
        var italic = false
        var code = false

        fun flush() {
            if (buffer.isNotEmpty()) {
                segments.add(InlineSegment(buffer.toString(), bold, italic, code))
                buffer.clear()
            }
        }

        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    flush(); bold = !bold; i += 2
                }

                text[i] == '`' -> {
                    flush(); code = !code; i += 1
                }

                text[i] == '*' -> {
                    flush(); italic = !italic; i += 1
                }

                else -> {
                    buffer.append(text[i]); i += 1
                }
            }
        }
        flush()
        return segments
    }
}
