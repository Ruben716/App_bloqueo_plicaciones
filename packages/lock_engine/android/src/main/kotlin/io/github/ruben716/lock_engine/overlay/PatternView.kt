package io.github.ruben716.lock_engine.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/** Cuadrícula 3x3 para dibujar el patrón propio de la app. */
internal class PatternView(
    context: Context,
    private val onComplete: (String) -> Unit,
) : View(context) {

    private val path = mutableListOf<Int>()
    private val centers = Array(9) { PointF() }
    private var finger: PointF? = null
    private var error = false

    var accent: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotRadius = context.dp(6).toFloat()
    private val hitRadius = context.dp(34).toFloat()

    private val clearRunnable = Runnable { reset() }

    fun reset() {
        removeCallbacks(clearRunnable)
        path.clear()
        finger = null
        error = false
        invalidate()
    }

    fun showError() {
        error = true
        invalidate()
        removeCallbacks(clearRunnable)
        postDelayed(clearRunnable, 900)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(MeasureSpec.getSize(widthMeasureSpec), context.dp(300))
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val cell = w / 3f
        for (i in 0 until 9) {
            centers[i].set(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
        }
        linePaint.strokeWidth = context.dp(4).toFloat()
        ringPaint.strokeWidth = context.dp(2).toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                parent?.requestDisallowInterceptTouchEvent(true)
                track(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> track(event.x, event.y)
            MotionEvent.ACTION_UP -> {
                finger = null
                invalidate()
                if (path.isNotEmpty()) onComplete(PatternGeometry.encode(path))
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun track(x: Float, y: Float) {
        finger = PointF(x, y)
        val hit = centers.indices.firstOrNull { hypot(centers[it].x - x, centers[it].y - y) <= hitRadius }
        if (hit != null && hit !in path) {
            PatternGeometry.append(path, hit)
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val color = if (error) ERROR_COLOR else accent
        linePaint.color = withAlpha(color, 0.85f)

        for (i in 1 until path.size) {
            val a = centers[path[i - 1]]
            val b = centers[path[i]]
            canvas.drawLine(a.x, a.y, b.x, b.y, linePaint)
        }
        val tip = finger
        if (tip != null && path.isNotEmpty()) {
            val last = centers[path.last()]
            canvas.drawLine(last.x, last.y, tip.x, tip.y, linePaint)
        }

        centers.forEachIndexed { index, center ->
            val selected = index in path
            dotPaint.color = if (selected) color else withAlpha(Color.WHITE, 0.7f)
            canvas.drawCircle(center.x, center.y, if (selected) dotRadius * 1.4f else dotRadius, dotPaint)
            if (selected) {
                ringPaint.color = withAlpha(color, 0.35f)
                canvas.drawCircle(center.x, center.y, dotRadius * 4f, ringPaint)
            }
        }
    }

    private companion object {
        val ERROR_COLOR: Int = Color.parseColor("#F87171")
    }
}
