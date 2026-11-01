package io.github.ruben716.lock_engine.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Indicador de dígitos introducidos (círculos rellenos / vacíos). */
internal class PinDotsView(context: Context) : View(context) {

    private val filledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1.5f).toFloat()
    }
    private val radius = context.dp(7).toFloat()
    private val gap = context.dp(18).toFloat()

    var total: Int = 4
        set(value) {
            field = value.coerceIn(1, 12)
            requestLayout()
            invalidate()
        }

    var filled: Int = 0
        set(value) {
            field = value.coerceAtLeast(0)
            invalidate()
        }

    var color: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val count = maxOf(total, filled)
        val width = (count * radius * 2 + (count - 1) * gap + paddingLeft + paddingRight).toInt()
        val height = (radius * 2 + paddingTop + paddingBottom).toInt() + context.dp(4)
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val count = maxOf(total, filled)
        val contentWidth = count * radius * 2 + (count - 1) * gap
        var x = (width - contentWidth) / 2f + radius
        val y = height / 2f
        filledPaint.color = color
        emptyPaint.color = withAlpha(color, 0.6f)
        repeat(count) { index ->
            if (index < filled) {
                canvas.drawCircle(x, y, radius, filledPaint)
            } else {
                canvas.drawCircle(x, y, radius - emptyPaint.strokeWidth / 2, emptyPaint)
            }
            x += radius * 2 + gap
        }
    }
}
