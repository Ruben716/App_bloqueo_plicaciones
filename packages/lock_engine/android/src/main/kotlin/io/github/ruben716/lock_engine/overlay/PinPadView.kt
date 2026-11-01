package io.github.ruben716.lock_engine.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.ruben716.lock_engine.R

/** Teclado numérico circular. Fila inferior: [biometría] 0 [borrar]. */
internal class PinPadView(
    context: Context,
    private val showBiometric: Boolean,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onDigit(digit: Char)
        fun onDelete()
        fun onBiometric()
    }

    private val keys = mutableListOf<View>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val rows = listOf("123", "456", "789")
        rows.forEach { row -> addView(row(row.map { digitKey(it) })) }
        val biometric = if (showBiometric) {
            iconKey(R.drawable.ic_lock_engine_fingerprint, R.string.lock_engine_biometric_button) { listener.onBiometric() }
        } else {
            spacer()
        }
        val delete = iconKey(R.drawable.ic_lock_engine_backspace, R.string.lock_engine_delete) { listener.onDelete() }
        addView(row(listOf(biometric, digitKey('0'), delete)))
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        keys.forEach {
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.4f
        }
    }

    private fun row(children: List<View>) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        children.forEach { child ->
            addView(child, LayoutParams(context.dp(KEY_DP), context.dp(KEY_DP)).apply {
                val margin = context.dp(MARGIN_DP)
                setMargins(margin, margin / 2, margin, margin / 2)
            })
        }
    }

    private fun digitKey(digit: Char): View = TextView(context).apply {
        text = digit.toString()
        textSize = 28f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = circleRipple(withAlpha(Color.WHITE, 0.08f), withAlpha(Color.WHITE, 0.3f))
        isClickable = true
        isFocusable = false
        setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            listener.onDigit(digit)
        }
        keys += this
    }

    private fun iconKey(icon: Int, description: Int, onClick: () -> Unit): View = FrameLayout(context).apply {
        background = circleRipple(Color.TRANSPARENT, withAlpha(Color.WHITE, 0.3f))
        contentDescription = context.getString(description)
        isClickable = true
        isFocusable = false
        addView(
            ImageView(context).apply {
                setImageResource(icon)
                setColorFilter(Color.WHITE)
            },
            FrameLayout.LayoutParams(context.dp(28), context.dp(28), Gravity.CENTER),
        )
        setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
        keys += this
    }

    private fun spacer(): View = View(context)

    private companion object {
        const val KEY_DP = 72
        const val MARGIN_DP = 14
    }
}
