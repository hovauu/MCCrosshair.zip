package com.mccrosshair.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.max
import kotlin.math.min

/** Сам прицел, который висит поверх игры. Окно имеет размер прицела. */
class CrosshairView(context: Context) : View(context) {

    private var style = Defaults.STYLE
    private var color = Color.WHITE

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun apply(s: AppState) {
        style = s.style
        color = s.color
        alpha = s.opacity.coerceIn(Limits.OPACITY_MIN, 1f)
        rotation = s.rotation
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val s = min(w, h)

        paint.color = color
        when (style) {
            CrossStyle.DOT -> canvas.drawCircle(cx, cy, s * 0.15f, paint)
            CrossStyle.CROSS -> {
                val t = max(1.5f * resources.displayMetrics.density, s * 0.07f)
                canvas.drawRect(cx - s / 2f, cy - t / 2f, cx + s / 2f, cy + t / 2f, paint)
                canvas.drawRect(cx - t / 2f, cy - s / 2f, cx + t / 2f, cy + s / 2f, paint)
            }
        }
    }
}
