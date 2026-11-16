package com.example.imacrotouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicBoolean

class MacroAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playing = AtomicBoolean(false)
    private var remainingLoops = 0
    private var currentMacro: Macro? = null
    private var speedMultiplier = 1.0f
    private var cancelRequested = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        stopPlayback()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        stopPlayback()
        super.onDestroy()
    }

    fun isPlaying(): Boolean = playing.get()

    fun play(
        macro: Macro,
        speed: Float = 1.0f,
        loops: Int = 1
    ) {
        if (macro.points.size < 2) return
        stopPlayback()
        currentMacro = macro
        speedMultiplier = speed.coerceIn(0.25f, 4.0f)
        remainingLoops = loops.coerceIn(1, 50)
        cancelRequested = false
        playing.set(true)
        dispatchNextLoop()
    }

    fun stopPlayback() {
        cancelRequested = true
        playing.set(false)
        remainingLoops = 0
        currentMacro = null
    }

    private fun dispatchNextLoop() {
        if (cancelRequested || remainingLoops <= 0) {
            playing.set(false)
            currentMacro = null
            return
        }
        val macro = currentMacro ?: return
        val metrics = resources.displayMetrics
        val path = Path()
        var duration = 0L
        macro.points.forEachIndexed { i, p ->
            val x = (p.x * metrics.widthPixels).coerceIn(0f, metrics.widthPixels.toFloat() - 1f)
            val y = (p.y * metrics.heightPixels).coerceIn(0f, metrics.heightPixels.toFloat() - 1f)
            duration = maxOf(duration, p.t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val scaledDuration = (duration / speedMultiplier).toLong().coerceIn(50L, 60_000L)

        val stroke = GestureDescription.StrokeDescription(path, 0, scaledDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                remainingLoops--
                if (!cancelRequested && remainingLoops > 0) {
                    mainHandler.postDelayed({ dispatchNextLoop() }, 120)
                } else {
                    playing.set(false)
                    currentMacro = null
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                playing.set(false)
                remainingLoops = 0
                currentMacro = null
            }
        }, mainHandler)

        if (!result) {
            Log.w(TAG, "dispatchGesture returned false")
            playing.set(false)
        }
    }

    companion object {
        private const val TAG = "iMacroA11y"
        @Volatile
        private var instance: MacroAccessibilityService? = null

        fun getInstance(): MacroAccessibilityService? = instance

        fun isEnabled(): Boolean = instance != null

        fun playMacro(
            context: Context,
            macro: Macro,
            speed: Float = 1.0f,
            loops: Int = 1
        ): Boolean {
            val svc = instance ?: return false
            svc.play(macro, speed, loops)
            MacroStore(context).markPlayed(macro.id)
            return true
        }

        fun stop() {
            instance?.stopPlayback()
        }
    }
}
