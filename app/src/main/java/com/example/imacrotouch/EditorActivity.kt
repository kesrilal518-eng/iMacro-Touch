package com.example.imacrotouch

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditorActivity : AppCompatActivity() {

    private lateinit var pad: GesturePad
    private lateinit var nameInput: TextInputEditText
    private lateinit var pointCount: TextView
    private lateinit var store: MacroStore
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        store = MacroStore(this)
        pad = findViewById(R.id.gesturePad)
        nameInput = findViewById(R.id.nameInput)
        pointCount = findViewById(R.id.pointCount)

        findViewById<MaterialToolbar>(R.id.editorToolbar).apply {
            title = "Gesture Editor"
            setNavigationOnClickListener { finish() }
            setNavigationIcon(android.R.drawable.ic_menu_revert)
        }

        pad.onPointsChanged = { count ->
            val dur = pad.durationMs()
            pointCount.text = "$count points • ${dur} ms"
        }

        editingId = intent.getStringExtra(EXTRA_MACRO_ID)
        if (editingId != null) {
            store.get(editingId!!)?.let { m ->
                nameInput.setText(m.name)
                pad.setPoints(m.points)
            }
        } else {
            nameInput.setText("Macro ${store.list().size + 1}")
        }

        findViewById<MaterialButton>(R.id.btnUndo).setOnClickListener { pad.undo() }
        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener { pad.clear() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }
    }

    private fun save() {
        val pts = pad.getPoints()
        if (pts.size < 2) {
            Toast.makeText(this, "Draw a gesture with at least two points", Toast.LENGTH_SHORT).show()
            return
        }
        val name = nameInput.text?.toString()?.trim().orEmpty().ifEmpty { "Untitled" }
        val macro = if (editingId != null) {
            val existing = store.get(editingId!!)
            Macro(
                id = editingId!!,
                name = name,
                points = pts,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                lastPlayedAt = existing?.lastPlayedAt ?: 0L
            )
        } else {
            Macro(name = name, points = pts)
        }
        store.save(macro)
        Toast.makeText(this, "Saved “${macro.name}”", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val EXTRA_MACRO_ID = "macro_id"
    }
}
