package com.mccrosshair.app

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Держит настройки в Compose-состоянии и обновляется при любом изменении. */
class SettingsStore(val prefs: SharedPreferences) {
    var state by mutableStateOf(AppState.load(prefs))
        private set

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        state = AppState.load(prefs)
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun close() = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var store: SettingsStore
    private var serviceOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(appPrefs())
        serviceOn = CrosshairService.isEnabled(this)

        setContent {
            val dark = store.state.dark
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AppTheme(dark) {
                AppRoot(store, serviceOn)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceOn = CrosshairService.isEnabled(this)
    }

    override fun onDestroy() {
        store.close()
        super.onDestroy()
    }
}

@Composable
private fun AppRoot(store: SettingsStore, serviceOn: Boolean) {
    var screen by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = screen == 1) { screen = 0 }

    val p = LocalPalette.current
    Surface(Modifier.fillMaxSize(), color = p.bg, contentColor = p.text) {
        if (screen == 0) {
            MainScreen(store, serviceOn, openSettings = { screen = 1 })
        } else {
            SettingsScreen(serviceOn, onBack = { screen = 0 })
        }
    }
}
