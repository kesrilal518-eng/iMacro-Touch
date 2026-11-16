package com.example.imacrotouch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple persistent store for macros using SharedPreferences (JSON).
 * Suitable for small number of macros; no network or external storage required.
 */
class MacroStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): List<Macro> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { Macro.fromJson(arr.getJSONObject(it)) }
                .sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(id: String): Macro? = list().find { it.id == id }

    fun save(macro: Macro) {
        val all = list().toMutableList()
        val idx = all.indexOfFirst { it.id == macro.id }
        if (idx >= 0) all[idx] = macro else all.add(0, macro)
        write(all)
    }

    fun delete(id: String) {
        write(list().filter { it.id != id })
    }

    fun rename(id: String, newName: String) {
        val m = get(id) ?: return
        m.name = newName.trim().ifEmpty { "Untitled" }
        save(m)
    }

    fun markPlayed(id: String) {
        val m = get(id) ?: return
        m.lastPlayedAt = System.currentTimeMillis()
        save(m)
    }

    fun exportAll(): String {
        val arr = JSONArray()
        list().forEach { arr.put(it.toJson()) }
        return arr.toString(2)
    }

    fun importJson(json: String): Int {
        return try {
            val arr = JSONArray(json)
            var count = 0
            for (i in 0 until arr.length()) {
                Macro.fromJson(arr.getJSONObject(i))?.let {
                    val copy = it.copy(id = java.util.UUID.randomUUID().toString())
                    save(copy)
                    count++
                }
            }
            count
        } catch (_: Exception) {
            0
        }
    }

    private fun write(macros: List<Macro>) {
        val arr = JSONArray()
        macros.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "imacro_store"
        private const val KEY = "macros"
    }
}
