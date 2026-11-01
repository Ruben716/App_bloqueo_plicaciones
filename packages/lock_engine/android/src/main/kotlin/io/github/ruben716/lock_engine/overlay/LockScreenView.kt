package io.github.ruben716.lock_engine.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.ruben716.lock_engine.R
import io.github.ruben716.lock_engine.model.CredentialType

data class LockScreenModel(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val credentialType: CredentialType,
    val pinLength: Int?,
    val biometricAvailable: Boolean,
    val fakeCrash: Boolean,
    val theme: LockTheme,
)

/**
 * Pantalla de bloqueo nativa que se dibuja como overlay sobre la app
 * protegida. Se construye por código (sin XML) para aparecer al instante.
 */
@SuppressLint("ViewConstructor")
internal class LockScreenView(
    context: Context,
    private val model: LockScreenModel,
    private val listener: Listener,
) : FrameLayout(context) {

    interface Listener {
        fun onSubmit(secret: String)
        fun onBiometricRequested()
        fun onBackPressed()
    }

    private val content = LinearLayout(context)
    private val subtitle = TextView(context)
    private val status = TextView(context)
    private val pinBuffer = StringBuilder()

    private var dots: PinDotsView? = null
    private var pinPad: PinPadView? = null
    private var pattern: PatternView? = null
    private var password: EditText? = null
    private var fakeCrashView: FakeCrashView? = null
    private var busy = false
    private var lockoutEndsAtMs = 0L

    private val lockoutTicker = object : Runnable {
        override fun run() {
            val remaining = lockoutEndsAtMs - System.currentTimeMillis()
            if (remaining <= 0) {
                lockoutEndsAtMs = 0
                status.text = ""
                setInputEnabled(true)
            } else {
                status.text = context.getString(R.string.lock_engine_lockout, ((remaining + 999) / 1000).toInt())
                postDelayed(this, 250)
            }
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(model.theme.backgroundTop, model.theme.backgroundBottom),
        )
        buildContent()
        if (model.fakeCrash) showFakeCrash()
    }

    // --------------------------------------------------------------- API

    fun showFailure(lockoutRemainingMs: Long) {
        busy = false
        clearInput()
        content.shake()
        pattern?.showError()
        status.text = context.getString(wrongMessage())
        if (lockoutRemainingMs > 0) startLockout(lockoutRemainingMs) else setInputEnabled(true)
    }

    fun startLockout(remainingMs: Long) {
        lockoutEndsAtMs = System.currentTimeMillis() + remainingMs
        setInputEnabled(false)
        removeCallbacks(lockoutTicker)
        post(lockoutTicker)
    }

    fun setBusy(value: Boolean) {
        busy = value
        setInputEnabled(!value && lockoutEndsAtMs == 0L)
    }

    fun requestPasswordFocus() {
        val field = password ?: return
        field.requestFocus()
        field.postDelayed({
            context.getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }, 150)
    }

    // ------------------------------------------------------------ Layout

    private fun buildContent() {
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL
        val side = context.dp(24)
        content.setPadding(side, 0, side, 0)

        content.addView(spacer(), LinearLayout.LayoutParams(0, 0, 1f))
        content.addView(iconView(), LinearLayout.LayoutParams(context.dp(76), context.dp(76)))
        content.addView(text(model.appLabel, 24f, Color.WHITE, bold = true).apply {
            setPadding(0, context.dp(16), 0, context.dp(4))
        })
        subtitle.apply {
            text = context.getString(promptMessage())
            textSize = 15f
            setTextColor(withAlpha(Color.WHITE, 0.75f))
            gravity = Gravity.CENTER
        }
        content.addView(subtitle)
        status.apply {
            textSize = 14f
            setTextColor(Color.parseColor("#FCA5A5"))
            gravity = Gravity.CENTER
            minHeight = context.dp(36)
            setPadding(0, context.dp(10), 0, context.dp(4))
        }
        content.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        when (model.credentialType) {
            CredentialType.PIN -> buildPin()
            CredentialType.PATTERN -> buildPattern()
            CredentialType.PASSWORD -> buildPassword()
        }

        content.addView(spacer(), LinearLayout.LayoutParams(0, 0, 0.6f))
        content.addView(text(context.getString(R.string.lock_engine_brand), 12f, withAlpha(Color.WHITE, 0.5f)).apply {
            setPadding(0, 0, 0, context.dp(16))
            letterSpacing = 0.1f
        })

        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setOnApplyWindowInsetsListener { _, insets ->
            val top: Int
            val bottom: Int
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                top = bars.top
                bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            content.setPadding(content.paddingLeft, top, content.paddingRight, bottom)
            insets
        }
    }

    private fun buildPin() {
        val dotsView = PinDotsView(context).apply { total = model.pinLength ?: 4 }
        dots = dotsView
        content.addView(dotsView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dp(28)
        })
        val pad = PinPadView(context, model.biometricAvailable, object : PinPadView.Listener {
            override fun onDigit(digit: Char) {
                if (busy || pinBuffer.length >= 12) return
                pinBuffer.append(digit)
                dotsView.filled = pinBuffer.length
                val expected = model.pinLength
                if (expected != null && pinBuffer.length >= expected) submit(pinBuffer.toString())
            }

            override fun onDelete() {
                if (busy || pinBuffer.isEmpty()) return
                pinBuffer.deleteCharAt(pinBuffer.length - 1)
                dotsView.filled = pinBuffer.length
            }

            override fun onBiometric() = listener.onBiometricRequested()
        })
        pinPad = pad
        content.addView(pad)
    }

    private fun buildPattern() {
        val view = PatternView(context) { encoded -> if (!busy) submit(encoded) }.apply {
            accent = model.theme.accent
        }
        pattern = view
        content.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        if (model.biometricAvailable) content.addView(biometricButton())
    }

    private fun buildPassword() {
        val field = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            setTextColor(Color.WHITE)
            setHintTextColor(withAlpha(Color.WHITE, 0.5f))
            hint = context.getString(R.string.lock_engine_prompt_password)
            textSize = 18f
            gravity = Gravity.CENTER
            isSingleLine = true
            background = roundedBackground(withAlpha(Color.WHITE, 0.1f), context.dp(16).toFloat(), withAlpha(Color.WHITE, 0.25f), context.dp(1))
            val pad = context.dp(16)
            setPadding(pad, pad, pad, pad)
            setOnEditorActionListener { view, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submit(view.text.toString())
                    true
                } else {
                    false
                }
            }
        }
        password = field
        content.addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val unlock = text(context.getString(R.string.lock_engine_unlock), 16f, Color.parseColor("#0F172A"), bold = true).apply {
            background = roundedRipple(model.theme.accent, withAlpha(Color.WHITE, 0.4f), context.dp(16).toFloat())
            val pad = context.dp(16)
            setPadding(pad, pad, pad, pad)
            setOnClickListener { submit(field.text.toString()) }
        }
        content.addView(unlock, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dp(16)
        })
        if (model.biometricAvailable) content.addView(biometricButton())
    }

    private fun biometricButton(): View = FrameLayout(context).apply {
        background = circleRipple(withAlpha(Color.WHITE, 0.08f), withAlpha(Color.WHITE, 0.3f))
        contentDescription = context.getString(R.string.lock_engine_biometric_button)
        addView(
            ImageView(context).apply {
                setImageResource(R.drawable.ic_lock_engine_fingerprint)
                setColorFilter(Color.WHITE)
            },
            LayoutParams(context.dp(32), context.dp(32), Gravity.CENTER),
        )
        setOnClickListener { listener.onBiometricRequested() }
        layoutParams = LinearLayout.LayoutParams(context.dp(64), context.dp(64)).apply {
            topMargin = context.dp(24)
        }
    }

    private fun iconView(): View = ImageView(context).apply {
        setImageDrawable(model.appIcon)
        scaleType = ImageView.ScaleType.FIT_CENTER
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.28f)
            }
        }
        clipToOutline = true
    }

    private fun text(value: String, sizeSp: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun spacer() = View(context)

    // ------------------------------------------------------------- Lógica

    private fun submit(secret: String) {
        if (busy || secret.isEmpty() || lockoutEndsAtMs > 0) return
        setBusy(true)
        listener.onSubmit(secret)
    }

    private fun clearInput() {
        pinBuffer.clear()
        dots?.filled = 0
        password?.setText("")
    }

    private fun setInputEnabled(enabled: Boolean) {
        pinPad?.isEnabled = enabled
        pattern?.isEnabled = enabled
        password?.isEnabled = enabled
    }

    private fun showFakeCrash() {
        val view = FakeCrashView(
            context,
            model.appLabel,
            onClose = listener::onBackPressed,
            onReveal = {
                fakeCrashView?.let(::removeView)
                fakeCrashView = null
                requestPasswordFocus()
            },
        )
        fakeCrashView = view
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun promptMessage(): Int = when (model.credentialType) {
        CredentialType.PIN -> R.string.lock_engine_prompt_pin
        CredentialType.PASSWORD -> R.string.lock_engine_prompt_password
        CredentialType.PATTERN -> R.string.lock_engine_prompt_pattern
    }

    private fun wrongMessage(): Int = when (model.credentialType) {
        CredentialType.PIN -> R.string.lock_engine_wrong_pin
        CredentialType.PASSWORD -> R.string.lock_engine_wrong_password
        CredentialType.PATTERN -> R.string.lock_engine_wrong_pattern
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) listener.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(lockoutTicker)
        super.onDetachedFromWindow()
    }
}
