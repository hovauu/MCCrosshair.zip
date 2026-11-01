package com.mccrosshair.app

import android.content.Context
import android.content.SharedPreferences

/** Встроенные прицелы: точка и крестик. */
enum class CrossStyle { DOT, CROSS }

/** Ключи настроек. */
object K {
    const val PREFS = "crosshair_prefs"
    const val ENABLED = "enabled"        // кнопка START/STOP
    const val AUTOSTART = "autostart"    // включать сам при входе в выбранное приложение
    const val FILTER = "filter_apps"     // показывать прицел только в выбранных приложениях
    const val DARK = "dark_theme"
    const val STYLE = "style"
    const val COLOR = "color"
    const val OPACITY = "opacity"
    const val ROTATION = "rotation"
    const val SIZE = "size_pct"          // % от короткой стороны экрана
    const val POS_X = "pos_x"            // сдвиг от центра, пиксели
    const val POS_Y = "pos_y"
    const val STEP = "pos_step"          // шаг стрелок
    const val TARGETS = "targets"        // пакеты выбранных приложений
}

object Defaults {
    const val COLOR = 0xFFFFFFFF.toInt()
    const val OPACITY = 1f
    const val ROTATION = 0f
    const val SIZE = 6f
    const val STEP = 10
    const val TARGETS = "com.mojang.minecraftpe"
    val STYLE = CrossStyle.CROSS
}

object Limits {
    const val OPACITY_MIN = 0.1f
    const val SIZE_MIN = 1f
    const val SIZE_MAX = 50f
    const val POS_MAX = 3000
}

data class AppState(
    val enabled: Boolean,
    val autostart: Boolean,
    val filter: Boolean,
    val dark: Boolean,
    val style: CrossStyle,
    val color: Int,
    val opacity: Float,
    val rotation: Float,
    val sizePct: Float,
    val posX: Int,
    val posY: Int,
    val step: Int,
    val targets: Set<String>,
) {
    companion object {
        fun load(p: SharedPreferences) = AppState(
            enabled = p.getBoolean(K.ENABLED, false),
            autostart = p.getBoolean(K.AUTOSTART, false),
            filter = p.getBoolean(K.FILTER, false),
            dark = p.getBoolean(K.DARK, true),
            style = runCatching { CrossStyle.valueOf(p.getString(K.STYLE, null) ?: "") }
                .getOrDefault(Defaults.STYLE),
            color = p.getInt(K.COLOR, Defaults.COLOR),
            opacity = p.getFloat(K.OPACITY, Defaults.OPACITY),
            rotation = p.getFloat(K.ROTATION, Defaults.ROTATION),
            sizePct = p.getFloat(K.SIZE, Defaults.SIZE),
            posX = p.getInt(K.POS_X, 0),
            posY = p.getInt(K.POS_Y, 0),
            step = p.getInt(K.STEP, Defaults.STEP),
            targets = parseTargets(p.getString(K.TARGETS, Defaults.TARGETS) ?: ""),
        )

        fun parseTargets(s: String): Set<String> =
            s.split('\n', ',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

fun Context.appPrefs(): SharedPreferences = getSharedPreferences(K.PREFS, Context.MODE_PRIVATE)
