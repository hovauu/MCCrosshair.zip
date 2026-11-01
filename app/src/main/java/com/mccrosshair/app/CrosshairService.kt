package com.mccrosshair.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Служба спец. возможностей.
 * 1) Следит, какое приложение открыто: если это игра из списка — показывает прицел.
 * 2) Рисует прицел окном TYPE_ACCESSIBILITY_OVERLAY: оно не мешает нажатиям
 *    и не требует отдельного разрешения «поверх других приложений».
 */
class CrosshairService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun isEnabled(ctx: Context): Boolean {
            val flat = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val me = ComponentName(ctx, CrosshairService::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun dp(ctx: Context, v: Float) = (v * ctx.resources.displayMetrics.density).roundToInt()

        fun applyCutout(lp: WindowManager.LayoutParams) {
            if (Build.VERSION.SDK_INT >= 30) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private var wm: WindowManager? = null
    private var prefs: SharedPreferences? = null
    private var state: AppState? = null

    private var inTarget = false
    private var shown = false
    private var crosshair: CrosshairView? = null
    private var mini: MiniPanel? = null

    private var bitmap: Bitmap? = null
    private var bitmapVersion = -1L
    private var imePackages = emptySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = appPrefs().also { it.registerOnSharedPreferenceChangeListener(this) }
        imePackages = runCatching {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .enabledInputMethodList.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
        refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Шторка, клавиатура и наши собственные окна-оверлеи не считаются сменой приложения
        if (pkg == "com.android.systemui" || pkg in imePackages) return
        if (pkg == packageName && event.className?.toString() != MainActivity::class.java.name) return

        val targets = state?.targets ?: return
        val now = pkg in targets
        if (now != inTarget) {
            inTarget = now
            refresh()
        }
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) = refresh()

    private fun refresh() {
        val p = prefs ?: return
        val s = AppState.load(p)
        state = s

        val shouldShow = s.enabled || (s.autostart && inTarget)
        if (shouldShow) {
            val justShown = !shown
            showCrosshair(s)
            shown = true

            if (s.mini) {
                val m = mini ?: MiniPanel(this, wm!!, p).also {
                    mini = it
                    it.show(expanded = justShown && s.miniOnStart)
                }
                m.update(s)
            } else {
                mini?.remove()
                mini = null
            }
        } else {
            hideAll()
        }
    }

    private fun showCrosshair(s: AppState) {
        val w = wm ?: return
        if (s.custom && bitmapVersion != s.customVersion) {
            bitmap = CustomImage.load(this)
            bitmapVersion = s.customVersion
        }
        val view = crosshair ?: CrosshairView(this)
        view.apply(s, bitmap)

        val size = max(dp(this, s.sizeDp), 4)
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = dp(this@CrosshairService, s.offsetXDp)
            y = dp(this@CrosshairService, s.offsetYDp)
            applyCutout(this)
        }

        if (crosshair == null) {
            runCatching { w.addView(view, lp); crosshair = view }
        } else {
            runCatching { w.updateViewLayout(view, lp) }
        }
    }

    private fun hideAll() {
        crosshair?.let { v -> runCatching { wm?.removeView(v) } }
        crosshair = null
        mini?.remove()
        mini = null
        shown = false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
        hideAll()
        super.onDestroy()
    }
}
