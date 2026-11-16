package com.example.imacrotouch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingMacroService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var expanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    private fun showBubble() {
        if (bubbleView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 200
        }

        val container = FrameLayout(this)
        val button = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundColor(0xDD2864DC.toInt())
            setPadding(28, 28, 28, 28)
            contentDescription = "iMacro floating control"
            setOnClickListener { toggleExpanded(container, params) }
            setOnTouchListener(DragTouchListener(params))
        }
        container.addView(button)
        bubbleView = container
        try {
            windowManager?.addView(container, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to show overlay: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun toggleExpanded(container: FrameLayout, params: WindowManager.LayoutParams) {
        if (expanded) {
            container.removeAllViews()
            val button = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_media_play)
                setBackgroundColor(0xDD2864DC.toInt())
                setPadding(28, 28, 28, 28)
                setOnClickListener { toggleExpanded(container, params) }
                setOnTouchListener(DragTouchListener(params))
            }
            container.addView(button)
            expanded = false
        } else {
            container.removeAllViews()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(0xEE222222.toInt())
                setPadding(12, 12, 12, 12)
            }
            fun addAction(label: String, icon: Int, action: () -> Unit) {
                val b = ImageButton(this).apply {
                    setImageResource(icon)
                    setBackgroundColor(0x00000000)
                    setPadding(20, 20, 20, 20)
                    contentDescription = label
                    setOnClickListener { action() }
                }
                row.addView(b)
            }
            addAction("Play last", android.R.drawable.ic_media_play) {
                playLastMacro()
            }
            addAction("Open app", android.R.drawable.ic_menu_view) {
                val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
            addAction("Close", android.R.drawable.ic_menu_close_clear_cancel) {
                stopSelf()
            }
            container.addView(row)
            expanded = true
        }
        windowManager?.updateViewLayout(container, params)
    }

    private fun playLastMacro() {
        val store = MacroStore(this)
        val last = store.list().maxByOrNull { it.lastPlayedAt.takeIf { t -> t > 0 } ?: it.createdAt }
        if (last == null) {
            Toast.makeText(this, "No macros saved yet", Toast.LENGTH_SHORT).show()
            return
        }
        if (!MacroAccessibilityService.playMacro(this, last, 1.0f, 1)) {
            Toast.makeText(this, "Enable Accessibility service first", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Playing: ${last.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeBubble() {
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }
        bubbleView = null
    }

    private inner class DragTouchListener(
        private val params: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var moved = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = initialX - dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(bubbleView, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    return moved
                }
            }
            return false
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "floating_macro"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating macro control",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, FloatingMacroService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("iMacro Touch floating control")
            .setContentText("Tap the bubble to play macros or close")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.imacrotouch.STOP_FLOATING"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val i = Intent(context, FloatingMacroService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingMacroService::class.java))
        }
    }
}
