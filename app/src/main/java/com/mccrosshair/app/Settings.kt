package com.mccrosshair.app

import android.content.Context
import android.content.SharedPreferences

/** Встроенные прицелы: точка и крестик. */
enum class CrossStyle { DOT, CROSS }

/** Ключи настроек. */
object K {
    const val PREFS = "crosshair_prefs"
    const val ENABLED = "enabled"            // кнопка START/STOP
    const val AUTOSTART = "autostart"        // включать сам при входе в игру
    const val MINI = "mini"                  // мини-настройки в игре
    const val MINI_ON_START = "mini_on_start"
    const val DARK = "dark_theme"
    const val CUSTOM = "custom_mode"         // своя картинка вместо встроенного
    const val STYLE = "style"
    const val COLOR = "color"
    const val OPACITY = "opacity"
    const val SIZE = "size_dp"
    const val OFF_X = "offset_x_dp"
    const val OFF_Y = "offset_y_dp"
    const val TINT_CUSTOM = "tint_custom"
    const val CUSTOM_VERSION = "custom_version"
    const val TARGETS = "targets"            // пакеты игр для автозапуска
}

object Defaults {
    const val COLOR = 0xFFFFFFFF.toInt()
    const val OPACITY = 1f
    const val SIZE = 24f
    const val TARGETS = "com.mojang.minecraftpe"
    val STYLE = CrossStyle.CROSS
}

object Limits {
    const val OPACITY_MIN = 0.1f
    const val SIZE_MIN = 4f
    const val SIZE_MAX = 150f
    const val OFFSET = 600f
}

data class AppState(
    val enabled: Boolean,
    val autostart: Boolean,
    val mini: Boolean,
    val miniOnStart: Boolean,
    val dark: Boolean,
    val custom: Boolean,
    val style: CrossStyle,
    val color: Int,
    val opacity: Float,
    val sizeDp: Float,
    val offsetXDp: Float,
    val offsetYDp: Float,
    val tintCustom: Boolean,
    val customVersion: Long,
    val targets: Set<String>,
) {
    companion object {
        fun load(p: SharedPreferences) = AppState(
            enabled = p.getBoolean(K.ENABLED, false),
            autostart = p.getBoolean(K.AUTOSTART, false),
            mini = p.getBoolean(K.MINI, false),
            miniOnStart = p.getBoolean(K.MINI_ON_START, false),
            dark = p.getBoolean(K.DARK, true),
            custom = p.getBoolean(K.CUSTOM, false),
            style = runCatching { CrossStyle.valueOf(p.getString(K.STYLE, null) ?: "") }
                .getOrDefault(Defaults.STYLE),
            color = p.getInt(K.COLOR, Defaults.COLOR),
            opacity = p.getFloat(K.OPACITY, Defaults.OPACITY),
            sizeDp = p.getFloat(K.SIZE, Defaults.SIZE),
            offsetXDp = p.getFloat(K.OFF_X, 0f),
            offsetYDp = p.getFloat(K.OFF_Y, 0f),
            tintCustom = p.getBoolean(K.TINT_CUSTOM, false),
            customVersion = p.getLong(K.CUSTOM_VERSION, 0L),
            targets = parseTargets(p.getString(K.TARGETS, Defaults.TARGETS) ?: ""),
        )

        fun parseTargets(s: String): Set<String> =
            s.split('\n', ',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

fun Context.appPrefs(): SharedPreferences = getSharedPreferences(K.PREFS, Context.MODE_PRIVATE)
