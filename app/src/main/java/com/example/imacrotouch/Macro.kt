package com.example.imacrotouch

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A single recorded gesture macro.
 * Points are normalized [0..1] screen-relative coordinates with relative timestamps in ms.
 */
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val points: List<GesturePoint>,
    val createdAt: Long = System.currentTimeMillis(),
    var lastPlayedAt: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
        put("lastPlayedAt", lastPlayedAt)
        put("points", JSONArray().apply {
            points.forEach { put(it.toJson()) }
        })
    }

    companion object {
        fun fromJson(obj: JSONObject): Macro? = try {
            val pts = mutableListOf<GesturePoint>()
            val arr = obj.getJSONArray("points")
            for (i in 0 until arr.length()) {
                GesturePoint.fromJson(arr.getJSONObject(i))?.let { pts.add(it) }
            }
            if (pts.isEmpty()) return null
            Macro(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Untitled"),
                points = pts,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                lastPlayedAt = obj.optLong("lastPlayedAt", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class GesturePoint(
    val x: Float, // 0..1
    val y: Float, // 0..1
    val t: Long   // ms from start
) {
    fun toJson(): JSONObject = JSONObject()
        .put("x", x.toDouble())
        .put("y", y.toDouble())
        .put("t", t)

    companion object {
        fun fromJson(obj: JSONObject): GesturePoint? = try {
            GesturePoint(
                x = obj.getDouble("x").toFloat().coerceIn(0f, 1f),
                y = obj.getDouble("y").toFloat().coerceIn(0f, 1f),
                t = obj.getLong("t").coerceAtLeast(0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}
