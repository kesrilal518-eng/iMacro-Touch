package com.example.imacrotouch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * In-app drawing surface for creating a single continuous gesture path.
 * Coordinates are stored normalized to the view size so they can be scaled
 * to any screen at playback time.
 *
 * Note: Android does not allow third-party apps to silently record raw
 * touches that occur in other applications. This pad is the supported way
 * to author gestures.
 */
class GesturePad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2864DC.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
        textSize = 36f
    }
    private val points = mutableListOf<GesturePoint>()
    private val history = mutableListOf<List<GesturePoint>>()
    private var startTime = 0L
    private var isDrawing = false

    var onPointsChanged: ((Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFFF1F4FA.toInt())
        if (points.isEmpty()) {
            canvas.drawText("Draw a gesture here", 32f, 56f, hintPaint)
            canvas.drawText(
                "Single continuous stroke",
                32f,
                100f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF888888.toInt()
                    textSize = 28f
                }
            )
        }
        canvas.drawPath(path, paint)
        if (points.isNotEmpty()) {
            val sx = points.first().x * width
            val sy = points.first().y * height
            canvas.drawCircle(sx, sy, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF22AA55.toInt()
                style = Paint.Style.FILL
            })
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val x = (event.x / w).coerceIn(0f, 1f)
        val y = (event.y / h).coerceIn(0f, 1f)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (points.isNotEmpty()) {
                    history.add(points.toList())
                    if (history.size > 20) history.removeAt(0)
                }
                points.clear()
                path.reset()
                startTime = System.currentTimeMillis()
                isDrawing = true
                points.add(GesturePoint(x, y, 0L))
                path.moveTo(event.x, event.y)
                invalidate()
                onPointsChanged?.invoke(points.size)
            }
            MotionEvent.ACTION_MOVE -> if (isDrawing) {
                val t = System.currentTimeMillis() - startTime
                points.add(GesturePoint(x, y, t))
                path.lineTo(event.x, event.y)
                invalidate()
                onPointsChanged?.invoke(points.size)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isDrawing) {
                val t = System.currentTimeMillis() - startTime
                points.add(GesturePoint(x, y, t))
                path.lineTo(event.x, event.y)
                isDrawing = false
                invalidate()
                onPointsChanged?.invoke(points.size)
            }
        }
        return true
    }

    fun clear() {
        if (points.isNotEmpty()) {
            history.add(points.toList())
            if (history.size > 20) history.removeAt(0)
        }
        points.clear()
        path.reset()
        invalidate()
        onPointsChanged?.invoke(0)
    }

    fun undo() {
        if (history.isEmpty()) return
        points.clear()
        points.addAll(history.removeAt(history.lastIndex))
        rebuildPath()
        onPointsChanged?.invoke(points.size)
    }

    fun getPoints(): List<GesturePoint> = points.toList()

    fun setPoints(pts: List<GesturePoint>) {
        points.clear()
        points.addAll(pts)
        rebuildPath()
        onPointsChanged?.invoke(points.size)
    }

    private fun rebuildPath() {
        path.reset()
        if (points.isEmpty()) {
            invalidate()
            return
        }
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        path.moveTo(points[0].x * w, points[0].y * h)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x * w, points[i].y * h)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (points.isNotEmpty()) rebuildPath()
    }

    fun durationMs(): Long = if (points.isEmpty()) 0L else points.maxOf { it.t }.coerceAtLeast(50L)
}
