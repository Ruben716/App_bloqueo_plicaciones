package io.github.ruben716.lock_engine.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.view.animation.CycleInterpolator

/** Utilidades de UI para las vistas nativas creadas por código. */
internal fun Context.dp(value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

internal fun Context.dp(value: Int): Int = dp(value.toFloat())

internal fun withAlpha(color: Int, alpha: Float): Int =
    Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

internal fun roundedBackground(color: Int, radiusPx: Float, strokeColor: Int? = null, strokePx: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx
        if (strokeColor != null) setStroke(strokePx, strokeColor)
    }

internal fun circleRipple(fill: Int, ripple: Int): Drawable {
    val content = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
    }
    val mask = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.WHITE)
    }
    return RippleDrawable(ColorStateList.valueOf(ripple), content, mask)
}

internal fun roundedRipple(fill: Int, ripple: Int, radiusPx: Float): Drawable {
    val content = roundedBackground(fill, radiusPx)
    val mask = roundedBackground(Color.WHITE, radiusPx)
    return RippleDrawable(ColorStateList.valueOf(ripple), content, mask)
}

/** Animación de "sacudida" para indicar un error. */
internal fun View.shake() {
    animate().cancel()
    translationX = 0f
    animate()
        .translationX(context.dp(12).toFloat())
        .setInterpolator(CycleInterpolator(3f))
        .setDuration(420)
        .withEndAction { translationX = 0f }
        .start()
}
