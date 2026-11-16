package com.example.imacrotouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var store: MacroStore
    private lateinit var adapter: MacroAdapter
    private lateinit var statusText: TextView
    private lateinit var a11yStatus: TextView
    private lateinit var emptyHint: TextView
    private lateinit var speedSlider: Slider
    private lateinit var loopSlider: Slider
    private lateinit var speedLabel: TextView
    private lateinit var loopLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = MacroStore(this)
        statusText = findViewById(R.id.statusText)
        a11yStatus = findViewById(R.id.a11yStatus)
        emptyHint = findViewById(R.id.emptyHint)
        speedSlider = findViewById(R.id.speedSlider)
        loopSlider = findViewById(R.id.loopSlider)
        speedLabel = findViewById(R.id.speedLabel)
        loopLabel = findViewById(R.id.loopLabel)

        findViewById<MaterialToolbar>(R.id.toolbar).title = getString(R.string.app_name)

        speedSlider.addOnChangeListener { _, value, _ ->
            speedLabel.text = String.format("%.2f×", value)
        }
        loopSlider.addOnChangeListener { _, value, _ ->
            loopLabel.text = value.toInt().toString()
        }

        findViewById<MaterialButton>(R.id.btnA11y).setOnClickListener { requestAccessibility() }
        findViewById<MaterialButton>(R.id.btnOverlay).setOnClickListener { requestOverlay() }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener {
            MacroAccessibilityService.stop()
            Toast.makeText(this, "Playback stopped", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnFloating).setOnClickListener { toggleFloating() }
        findViewById<MaterialButton>(R.id.btnNew).setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }

        adapter = MacroAdapter(
            onPlay = { macro -> playMacro(macro) },
            onMore = { view, macro -> showMacroMenu(view, macro) }
        )
        findViewById<RecyclerView>(R.id.macroList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        updateA11yStatus()
    }

    private fun refreshList() {
        val macros = store.list()
        adapter.submit(macros)
        emptyHint.visibility = if (macros.isEmpty()) View.VISIBLE else View.GONE
        statusText.text = when {
            macros.isEmpty() -> "Create your first gesture macro with the New button."
            else -> "${macros.size} macro(s) saved locally. Select one to play."
        }
    }

    private fun updateA11yStatus() {
        val enabled = MacroAccessibilityService.isEnabled()
        a11yStatus.text = if (enabled) {
            "Accessibility: ENABLED ✓"
        } else {
            "Accessibility: OFF — enable to play macros"
        }
        a11yStatus.setTextColor(
            if (enabled) 0xFF1B7A3A.toInt() else 0xFFB00020.toInt()
        )
    }

    private fun playMacro(macro: Macro) {
        val speed = speedSlider.value
        val loops = loopSlider.value.toInt()
        if (!MacroAccessibilityService.playMacro(this, macro, speed, loops)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_a11y_title)
                .setMessage(R.string.permission_a11y_message)
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(
                this,
                "Playing “${macro.name}” ×$loops @ ${String.format("%.1f", speed)}×",
                Toast.LENGTH_SHORT
            ).show()
            refreshList()
        }
    }

    private fun showMacroMenu(anchor: View, macro: Macro) {
        PopupMenu(this, anchor).apply {
            menu.add("Rename")
            menu.add("Duplicate")
            menu.add("Delete")
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Rename" -> {
                        renameMacro(macro)
                        true
                    }
                    "Duplicate" -> {
                        val copy = macro.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            name = macro.name + " copy",
                            createdAt = System.currentTimeMillis()
                        )
                        store.save(copy)
                        refreshList()
                        true
                    }
                    "Delete" -> {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Delete macro?")
                            .setMessage("“${macro.name}” will be removed permanently.")
                            .setPositiveButton("Delete") { _, _ ->
                                store.delete(macro.id)
                                refreshList()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun renameMacro(macro: Macro) {
        val input = TextInputEditText(this).apply {
            setText(macro.name)
            setSelection(macro.name.length)
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename macro")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                store.rename(macro.id, input.text?.toString().orEmpty())
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestAccessibility() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_a11y_title)
            .setMessage(R.string.permission_a11y_message)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestOverlay() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_overlay_title)
            .setMessage(R.string.permission_overlay_message)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFloating() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlay()
            return
        }
        FloatingMacroService.start(this)
        Toast.makeText(this, "Floating control started (notification + bubble)", Toast.LENGTH_SHORT).show()
    }
}

class MacroAdapter(
    private val onPlay: (Macro) -> Unit,
    private val onMore: (View, Macro) -> Unit
) : RecyclerView.Adapter<MacroAdapter.VH>() {

    private var items: List<Macro> = emptyList()

    fun submit(list: List<Macro>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_macro, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.macroName)
        private val meta: TextView = itemView.findViewById(R.id.macroMeta)
        private val play: MaterialButton = itemView.findViewById(R.id.btnPlay)
        private val more: MaterialButton = itemView.findViewById(R.id.btnMore)

        fun bind(m: Macro) {
            name.text = m.name
            val dur = if (m.points.isEmpty()) 0L else m.points.maxOf { it.t }
            meta.text = "${m.points.size} points • ${dur} ms"
            play.setOnClickListener { onPlay(m) }
            more.setOnClickListener { onMore(it, m) }
            itemView.setOnClickListener { onPlay(m) }
        }
    }
}
