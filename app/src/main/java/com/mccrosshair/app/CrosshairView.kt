package com.mccrosshair.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.min

/** Сам прицел, который висит поверх игры. Окно имеет размер прицела. */
class CrosshairView(context: Context) : View(context) {

    private var style = Defaults.STYLE
    private var color = Color.WHITE
    private var custom = false
    private var tint = false
    private var bitmap: Bitmap? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dst = RectF()

    fun apply(s: AppState, bmp: Bitmap?) {
        style = s.style
        color = s.color
        custom = s.custom
        tint = s.tintCustom
        bitmap = bmp
        alpha = s.opacity.coerceIn(Limits.OPACITY_MIN, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val s = min(w, h)

        if (custom) {
            val b = bitmap ?: return
            val scale = s / max(b.width, b.height)
            val bw = b.width * scale
            val bh = b.height * scale
            dst.set(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
            bmpPaint.colorFilter = if (tint) PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) else null
            canvas.drawBitmap(b, null, dst, bmpPaint)
            return
        }

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
