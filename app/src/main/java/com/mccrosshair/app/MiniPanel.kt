package com.mccrosshair.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

private const val WRAP = -2 // ViewGroup.LayoutParams.WRAP_CONTENT

/**
 * Мини-настройки прямо в игре: маленькая кнопка у левого края,
 * по нажатию раскрывается панель (стиль, цвет, прозрачность, размер).
 * Положение прицела здесь не меняется — только в приложении.
 */
class MiniPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val prefs: SharedPreferences,
) {
    private val density = ctx.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).roundToInt()

    private val swatches = longArrayOf(
        0xFFFFFFFF, 0xFFFF3B30, 0xFF00FF66, 0xFF00E5FF, 0xFFFFEB3B, 0xFFE92FAA
    ).map { it.toInt() }

    private var expanded = false
    private var attached = false

    private val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val toggle = ImageView(ctx)
    private val panel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    private val swatchViews = mutableListOf<View>()
    private val styleBtn: TextView
    private val opacityText: TextView
    private val sizeText: TextView

    init {
        toggle.setImageResource(R.drawable.ic_tune)
        toggle.setColorFilter(Color.WHITE)
        toggle.background = rounded(0xB31D1F26.toInt(), 22)
        dp(10).let { toggle.setPadding(it, it, it, it) }
        toggle.setOnClickListener { setExpanded(!expanded) }
        root.addView(toggle, LinearLayout.LayoutParams(dp(44), dp(44)))

        panel.background = rounded(0xEB1D1F26.toInt(), 18)
        panel.setPadding(dp(12), dp(8), dp(12), dp(10))

        styleBtn = button("") { cycleStyle() }
        panel.addView(row(label("Стиль"), styleBtn))

        val colorsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        swatches.forEach { c ->
            val v = View(ctx).apply {
                background = circle(c, false)
                setOnClickListener { prefs.edit().putInt(K.COLOR, c).apply() }
            }
            colorsRow.addView(v, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(6) })
            swatchViews += v
        }
        panel.addView(colorsRow, rowParams())

        opacityText = valueText()
        panel.addView(row(label("Прозр."), button("−") { changeOpacity(-0.1f) }, opacityText, button("+") { changeOpacity(0.1f) }))

        sizeText = valueText()
        panel.addView(row(label("Размер"), button("−") { changeSize(-2f) }, sizeText, button("+") { changeSize(2f) }))

        panel.addView(button("Свернуть") { setExpanded(false) }, rowParams())

        panel.visibility = View.GONE
        root.addView(panel, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(6) })
    }

    // ---------- действия ----------

    private fun current() = AppState.load(prefs)

    private fun cycleStyle() {
        val s = current()
        val next = when {
            s.custom -> s.style
            s.style == CrossStyle.DOT -> CrossStyle.CROSS
            else -> CrossStyle.DOT
        }
        prefs.edit().putBoolean(K.CUSTOM, false).putString(K.STYLE, next.name).apply()
    }

    private fun changeOpacity(delta: Float) {
        val v = ((current().opacity + delta) * 10f).roundToInt() / 10f
        prefs.edit().putFloat(K.OPACITY, v.coerceIn(Limits.OPACITY_MIN, 1f)).apply()
    }

    private fun changeSize(delta: Float) {
        val v = (current().sizeDp + delta).coerceIn(Limits.SIZE_MIN, Limits.SIZE_MAX)
        prefs.edit().putFloat(K.SIZE, v).apply()
    }

    fun update(s: AppState) {
        styleBtn.text = when {
            s.custom -> "Своя"
            s.style == CrossStyle.DOT -> "Точка"
            else -> "Крестик"
        }
        opacityText.text = String.format(Locale.forLanguageTag("ru"), "%.1f", s.opacity)
        sizeText.text = s.sizeDp.roundToInt().toString()
        swatchViews.forEachIndexed { i, v -> v.background = circle(swatches[i], swatches[i] == s.color) }
    }

    fun show(expanded: Boolean) {
        if (attached) return
        setExpanded(expanded)
        val lp = WindowManager.LayoutParams(
            WRAP, WRAP,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = dp(8)
            CrosshairService.applyCutout(this)
        }
        runCatching { wm.addView(root, lp); attached = true }
    }

    fun setExpanded(e: Boolean) {
        expanded = e
        panel.visibility = if (e) View.VISIBLE else View.GONE
    }

    fun remove() {
        if (attached) runCatching { wm.removeView(root) }
        attached = false
    }

    // ---------- вид ----------

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun circle(color: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(if (selected) 3 else 1), if (selected) 0xFFB4C3FF.toInt() else 0x66FFFFFF)
    }

    private fun label(t: String) = TextView(ctx).apply {
        text = t
        setTextColor(0xFFB0B3BC.toInt())
        textSize = 13f
        layoutParams = LinearLayout.LayoutParams(dp(60), WRAP)
    }

    private fun button(t: String, onClick: () -> Unit) = TextView(ctx).apply {
        text = t
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        background = rounded(0xFF353A48.toInt(), 10)
        minWidth = dp(36)
        minHeight = dp(34)
        setPadding(dp(10), dp(4), dp(10), dp(4))
        setOnClickListener { onClick() }
    }

    private fun valueText() = TextView(ctx).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        minWidth = dp(48)
    }

    private fun row(vararg views: View) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEach { addView(it) }
        layoutParams = rowParams()
    }

    private fun rowParams() = LinearLayout.LayoutParams(WRAP, WRAP).apply {
        topMargin = dp(6)
        bottomMargin = dp(2)
    }
}
