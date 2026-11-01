package com.mccrosshair.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Служба спец. возможностей.
 * 1) Следит, какое приложение открыто (для автозапуска и фильтра приложений).
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
    }

    private var wm: WindowManager? = null
    private var prefs: SharedPreferences? = null
    private var state: AppState? = null

    private var inTarget = false
    private var crosshair: CrosshairView? = null
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

        // Шторка, клавиатура и собственные окна-оверлеи не считаются сменой приложения
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

        val manual = s.enabled && (!s.filter || inTarget)
        val auto = s.autostart && inTarget
        if (manual || auto) showCrosshair(s) else hideCrosshair()
    }

    private fun showCrosshair(s: AppState) {
        val w = wm ?: return
        val view = crosshair ?: CrosshairView(this)
        view.apply(s)

        val dm = resources.displayMetrics
        val screen = min(dm.widthPixels, dm.heightPixels)
        val size = max((screen * s.sizePct / 100f).roundToInt(), 4)

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
            x = s.posX
            y = s.posY
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (crosshair == null) {
            runCatching { w.addView(view, lp); crosshair = view }
        } else {
            runCatching { w.updateViewLayout(view, lp) }
        }
    }

    private fun hideCrosshair() {
        crosshair?.let { v -> runCatching { wm?.removeView(v) } }
        crosshair = null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
        hideCrosshair()
        super.onDestroy()
    }
}
