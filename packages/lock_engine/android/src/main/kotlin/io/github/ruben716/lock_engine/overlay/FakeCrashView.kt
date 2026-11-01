package io.github.ruben716.lock_engine.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.ruben716.lock_engine.R

/**
 * Camuflaje: imita el diálogo de "la app se ha detenido". Quien no conoce el
 * truco cierra la app; el dueño mantiene pulsado el título para ver la
 * pantalla de bloqueo real.
 */
internal class FakeCrashView(
    context: Context,
    appLabel: String,
    onClose: () -> Unit,
    onReveal: () -> Unit,
) : FrameLayout(context) {

    init {
        setBackgroundColor(withAlpha(Color.BLACK, 0.55f))
        isClickable = true

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(Color.parseColor("#FAFAFA"), context.dp(24).toFloat())
            val padding = context.dp(24)
            setPadding(padding, padding, padding, context.dp(12))
            elevation = context.dp(12).toFloat()
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.lock_engine_fake_crash_title, appLabel)
            textSize = 20f
            setTextColor(Color.parseColor("#1F1F1F"))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isLongClickable = true
            setOnLongClickListener {
                onReveal()
                true
            }
        }
        val message = TextView(context).apply {
            text = context.getString(R.string.lock_engine_fake_crash_message)
            textSize = 15f
            setTextColor(Color.parseColor("#444444"))
            setPadding(0, context.dp(12), 0, context.dp(16))
        }
        card.addView(title)
        card.addView(message)
        card.addView(action(R.string.lock_engine_fake_crash_close, onClose))
        card.addView(action(R.string.lock_engine_fake_crash_feedback, onClose))

        addView(
            card,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
                val margin = context.dp(32)
                setMargins(margin, 0, margin, 0)
            },
        )
    }

    private fun action(textRes: Int, onClick: () -> Unit): View = TextView(context).apply {
        text = context.getString(textRes)
        textSize = 15f
        setTextColor(Color.parseColor("#1F1F1F"))
        val vertical = context.dp(14)
        setPadding(context.dp(4), vertical, context.dp(4), vertical)
        background = roundedRipple(Color.TRANSPARENT, withAlpha(Color.BLACK, 0.12f), context.dp(8).toFloat())
        setOnClickListener { onClick() }
    }
}
