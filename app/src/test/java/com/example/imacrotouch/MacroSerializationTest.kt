package com.example.imacrotouch

import org.junit.Assert.*
import org.junit.Test

class MacroSerializationTest {

    @Test
    fun pointRoundTrip() {
        val p = GesturePoint(0.5f, 0.25f, 123L)
        val json = p.toJson()
        val restored = GesturePoint.fromJson(json)!!
        assertEquals(0.5f, restored.x, 0.0001f)
        assertEquals(0.25f, restored.y, 0.0001f)
        assertEquals(123L, restored.t)
    }

    @Test
    fun macroRoundTrip() {
        val pts = listOf(
            GesturePoint(0.1f, 0.2f, 0),
            GesturePoint(0.5f, 0.5f, 200),
            GesturePoint(0.9f, 0.8f, 400)
        )
        val m = Macro(name = "Test swipe", points = pts)
        val json = m.toJson()
        val restored = Macro.fromJson(json)!!
        assertEquals("Test swipe", restored.name)
        assertEquals(3, restored.points.size)
        assertEquals(0.1f, restored.points[0].x, 0.0001f)
        assertEquals(400L, restored.points[2].t)
    }

    @Test
    fun coordinateClamp() {
        val json = org.json.JSONObject()
            .put("x", 1.5)
            .put("y", -0.2)
            .put("t", -10)
        val p = GesturePoint.fromJson(json)!!
        assertEquals(1.0f, p.x, 0.0001f)
        assertEquals(0.0f, p.y, 0.0001f)
        assertEquals(0L, p.t)
    }
}
