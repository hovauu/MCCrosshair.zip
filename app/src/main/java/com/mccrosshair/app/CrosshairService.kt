package com.mccrosshair.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Служба спец. возможностей.
 * 1) Следит, какое приложение открыто (для автозапуска и фильтра приложений).
 * 2) Рисует прицел окном TYPE_ACCESSIBILITY_OVERLAY: оно не мешает нажатиям.
 *    Если система такое окно не даёт — пробует обычное окно «поверх других приложений».
 */
class CrosshairService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        /** Работающий экземпляр службы (null — служба не запущена системой). */
        var instance: CrosshairService? = null
            private set

        /** true, когда система действительно запустила службу. */
        var running by mutableStateOf(false)
            private set

        /** Последнее открытое приложение — показывается в Настройках для проверки. */
        var lastApp by mutableStateOf<String?>(null)

        /** Ошибка при показе прицела, если была. */
        var lastError by mutableStateOf<String?>(null)

        /** Прицел сейчас на экране. */
        var visible by mutableStateOf(false)
            private set

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
    private var usingAppOverlay = false
    private var imePackages = emptySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        running = true
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
        if (pkg in imePackages) return

        val targets = state?.targets ?: AppState.load(appPrefs()).targets

        val now = when {
            // Любое событие от выбранной игры = игра на экране
            pkg in targets -> true
            // Смена на другое приложение засчитывается, только если открылась его activity.
            // Всплывашки поверх игры (Game Turbo, уведомления, тосты, диалоги) игнорируются.
            isActivity(pkg, event.className?.toString()) -> false
            else -> return
        }

        if (pkg != packageName) lastApp = pkg
        if (now != inTarget) {
            inTarget = now
            refresh()
        }
    }

    private val activityCache = HashMap<String, Boolean>()

    private fun isActivity(pkg: String, cls: String?): Boolean {
        if (cls.isNullOrEmpty()) return false
        val key = "$pkg/$cls"
        return activityCache.getOrPut(key) {
            try {
                packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) = refresh()

    /** Пересчитать, нужен ли прицел, и показать/спрятать его. */
    fun refresh() {
        val p = prefs ?: return
        val s = AppState.load(p)
        state = s

        val manual = s.enabled && (!s.filter || inTarget)
        val auto = s.autostart && inTarget
        if (manual || auto) showCrosshair(s) else hideCrosshair()
    }

    private fun layoutParams(s: AppState, type: Int): WindowManager.LayoutParams {
        val dm = resources.displayMetrics
        val screen = min(dm.widthPixels, dm.heightPixels)
        val size = max((screen * s.sizePct / 100f).roundToInt(), 4)

        return WindowManager.LayoutParams(
            size, size, type,
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
    }

    private fun showCrosshair(s: AppState) {
        val w = wm ?: return
        val existing = crosshair

        if (existing != null) {
            existing.apply(s)
            val type = if (usingAppOverlay) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            try {
                w.updateViewLayout(existing, layoutParams(s, type))
                return
            } catch (e: Exception) {
                // окно потерялось — создадим заново
                runCatching { w.removeView(existing) }
                crosshair = null
            }
        }

        val view = CrosshairView(this).also { it.apply(s) }

        // 1) Окно службы спец. возможностей
        try {
            w.addView(view, layoutParams(s, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY))
            crosshair = view
            usingAppOverlay = false
            visible = true
            lastError = null
            return
        } catch (e: Exception) {
            lastError = "Окно службы: ${e.javaClass.simpleName}: ${e.message}"
        }

        // 2) Запасной вариант: обычное окно «поверх других приложений»
        if (Settings.canDrawOverlays(this)) {
            try {
                w.addView(view, layoutParams(s, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))
                crosshair = view
                usingAppOverlay = true
                visible = true
                lastError = null
                return
            } catch (e: Exception) {
                lastError = (lastError ?: "") + "\nОбычное окно: ${e.javaClass.simpleName}: ${e.message}"
            }
        } else {
            lastError = (lastError ?: "") + "\nНет разрешения «Поверх других приложений»."
        }
        visible = false
    }

    private fun hideCrosshair() {
        crosshair?.let { v -> runCatching { wm?.removeView(v) } }
        crosshair = null
        visible = false
    }

    private fun shutdown() {
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
        hideCrosshair()
        if (instance === this) instance = null
        running = false
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
