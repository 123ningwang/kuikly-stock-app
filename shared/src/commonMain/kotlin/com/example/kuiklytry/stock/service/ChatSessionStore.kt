package com.example.kuiklytry.stock.service

import com.example.kuiklytry.stock.model.ChatBlock
import com.example.kuiklytry.stock.model.ChatMessage
import com.example.kuiklytry.stock.model.ChatRole
import com.example.kuiklytry.stock.model.PricePoint
import com.example.kuiklytry.stock.model.StockQuote
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
    return obj
}

private fun quoteFromJson(obj: JSONObject): StockQuote = StockQuote(
    code = obj.optString("code"),
    name = obj.optString("name"),
    market = obj.optString("market", "SH"),
    price = obj.optDouble("price", 0.0),
    change = obj.optDouble("change", 0.0),
    changePercent = obj.optDouble("changePercent", 0.0)
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
