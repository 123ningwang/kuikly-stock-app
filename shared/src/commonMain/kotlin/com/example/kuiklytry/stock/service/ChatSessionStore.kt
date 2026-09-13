package com.example.kuiklytry.stock.service

import com.example.kuiklytry.stock.model.ChatBlock
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.model.ChatRole
import com.example.kuiklytry.stock.model.PricePoint
import com.example.kuiklytry.stock.model.QuoteKind
import com.example.kuiklytry.stock.model.RiskLevel
import com.example.kuiklytry.stock.model.StockQuote
import com.example.kuiklytry.stock.model.TrendDirection
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 会话聊天记录本地持久化 Key（Android 侧由 SharedPreferences 存储）。
 */
internal const val SESSION_STORAGE_KEY = "stock_ai_chat_session_v1"

/**
 * 将聊天消息列表序列化为 JSON 字符串，用于本地持久化。
 */
internal fun List<ChatMessage>.toSessionJson(): String {
    val arr = JSONArray()
    forEach { arr.put(messageToJson(it)) }
    return arr.toString()
}

/**
 * 从 JSON 字符串反序列化聊天消息列表；空串或解析失败返回空列表。
 */
internal fun sessionFromJson(json: String): List<ChatMessage> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val result = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { result.add(messageFromJson(it)) }
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}

private fun messageToJson(msg: ChatMessage): JSONObject {
    val obj = JSONObject()
    obj.put("id", msg.id)
    obj.put("role", msg.role.name)
    obj.put("timestamp", msg.timestamp)
    val blocks = JSONArray()
    msg.blocks.forEach { blocks.put(blockToJson(it)) }
    obj.put("blocks", blocks)
    return obj
}

private fun messageFromJson(obj: JSONObject): ChatMessage {
    val id = obj.optString("id")
    val role = try {
        ChatRole.valueOf(obj.optString("role"))
    } catch (e: Exception) {
        ChatRole.USER
    }
    val timestamp = obj.optLong("timestamp", 0L)
    val blocks = mutableListOf<ChatBlock>()
    obj.optJSONArray("blocks")?.let { arr ->
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { blockFromJson(it)?.let { b -> blocks.add(b) } }
        }
    }
    return ChatMessage(id, role, blocks, timestamp)
}

private fun blockToJson(block: ChatBlock): JSONObject {
    val obj = JSONObject()
    when (block) {
        is ChatBlock.Text -> {
            obj.put("type", "text")
            obj.put("markdown", block.markdown)
        }
        is ChatBlock.StockCard -> {
            obj.put("type", "stock_card")
            obj.put("stock", quoteToJson(block.stock))
        }
        is ChatBlock.MiniChart -> {
            obj.put("type", "mini_chart")
            obj.put("stock", quoteToJson(block.stock))
            obj.put("points", pointsToJson(block.points))
        }
        is ChatBlock.TrendCard -> {
            obj.put("type", "trend_card")
            obj.put("direction", block.direction.name)
            obj.put("signals", stringListToJson(block.signals))
        }
        is ChatBlock.RiskCard -> {
            obj.put("type", "risk_card")
            obj.put("level", block.level.name)
            obj.put("warnings", stringListToJson(block.warnings))
        }
    }
    return obj
}

private fun blockFromJson(obj: JSONObject): ChatBlock? = when (obj.optString("type")) {
    "text" -> ChatBlock.Text(obj.optString("markdown"))
    "stock_card" -> {
        val stock = obj.optJSONObject("stock")?.let { quoteFromJson(it) } ?: return null
        ChatBlock.StockCard(stock)
    }
    "mini_chart" -> {
        val stock = obj.optJSONObject("stock")?.let { quoteFromJson(it) } ?: return null
        val points = obj.optJSONArray("points")?.let { pointsFromJson(it) } ?: emptyList()
        ChatBlock.MiniChart(stock, points)
    }
    "trend_card" -> {
        val direction = try {
            TrendDirection.valueOf(obj.optString("direction"))
        } catch (e: Exception) {
            TrendDirection.FLAT
        }
        val signals = obj.optJSONArray("signals")?.let { stringListFromJson(it) } ?: emptyList()
        ChatBlock.TrendCard(direction, signals)
    }
    "risk_card" -> {
        val level = try {
            RiskLevel.valueOf(obj.optString("level"))
        } catch (e: Exception) {
            RiskLevel.MEDIUM
        }
        val warnings = obj.optJSONArray("warnings")?.let { stringListFromJson(it) } ?: emptyList()
        ChatBlock.RiskCard(level, warnings)
    }
    else -> null
}

private fun quoteToJson(q: StockQuote): JSONObject {
    val obj = JSONObject()
    obj.put("code", q.code)
    obj.put("name", q.name)
    obj.put("market", q.market)
    obj.put("price", q.price)
    obj.put("change", q.change)
    obj.put("changePercent", q.changePercent)
    obj.put("kind", q.kind.name)
    return obj
}

private fun quoteFromJson(obj: JSONObject): StockQuote = StockQuote(
    code = obj.optString("code"),
    name = obj.optString("name"),
    market = obj.optString("market", "SH"),
    price = obj.optDouble("price", 0.0),
    change = obj.optDouble("change", 0.0),
    changePercent = obj.optDouble("changePercent", 0.0),
    kind = try {
        QuoteKind.valueOf(obj.optString("kind", "STOCK"))
    } catch (e: Exception) {
        QuoteKind.STOCK
    }
)

private fun pointsToJson(points: List<PricePoint>): JSONArray {
    val arr = JSONArray()
    points.forEach { p ->
        val obj = JSONObject()
        obj.put("label", p.label)
        obj.put("price", p.price)
        arr.put(obj)
    }
    return arr
}

private fun pointsFromJson(arr: JSONArray): List<PricePoint> {
    val list = mutableListOf<PricePoint>()
    for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        list.add(PricePoint(obj.optString("label"), obj.optDouble("price", 0.0)))
    }
    return list
}

private fun stringListToJson(list: List<String>): JSONArray {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr
}

private fun stringListFromJson(arr: JSONArray): List<String> {
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
        list.add(arr.optString(i) ?: "")
    }
    return list
}
