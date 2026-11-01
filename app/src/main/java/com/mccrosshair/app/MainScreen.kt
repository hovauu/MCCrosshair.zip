package com.mccrosshair.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val RU = Locale.forLanguageTag("ru")

@Composable
fun MainScreen(store: SettingsStore, serviceOn: Boolean, openSettings: () -> Unit) {
    val ctx = LocalContext.current
    val s = store.state
    val p = LocalPalette.current

    var a11yDialog by remember { mutableStateOf(false) }
    var styleDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (CustomImage.importFrom(ctx, uri)) {
                store.edit {
                    putBoolean(K.CUSTOM, true)
                    putLong(K.CUSTOM_VERSION, System.currentTimeMillis())
                }
            } else {
                Toast.makeText(ctx, "Не удалось открыть картинку", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val customBmp = remember(s.customVersion) { CustomImage.load(ctx)?.asImageBitmap() }
    val color = Color(s.color)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // ---- верхняя панель ----
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { store.edit { putBoolean(K.DARK, !s.dark) } }) {
                Icon(Icons.Filled.Brightness6, "Тема", tint = p.text)
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = p.text)) { append("CROSSHAIR") }
                    withStyle(SpanStyle(color = p.accent)) { append("MC") }
                },
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            IconButton(onClick = openSettings) {
                Icon(Icons.Filled.Settings, "Настройки", tint = p.text)
            }
        }

        Spacer(Modifier.height(48.dp))

        // ---- START / STOP ----
        OutlinedButton(
            onClick = {
                if (!serviceOn) a11yDialog = true
                else store.edit { putBoolean(K.ENABLED, !s.enabled) }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).width(190.dp).height(68.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(3.dp, p.accent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (s.enabled) p.accent else Color.Transparent,
                contentColor = if (s.enabled) p.bg else p.text
            )
        ) {
            Text(if (s.enabled) "STOP" else "START", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Text(
            when {
                !serviceOn -> "Служба выключена — прицел не сможет появиться"
                s.enabled -> "Прицел показан поверх всех приложений"
                s.autostart -> "Прицел появится сам, когда откроешь Minecraft"
                else -> "Прицел выключен"
            },
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 36.dp),
            color = if (serviceOn) p.textDim else p.warn,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )

        // ---- служба выключена ----
        if (!serviceOn) {
            SectionCard {
                Text("Включи службу «Прицел MC»", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "Она рисует прицел поверх игры и замечает, когда ты открываешь Minecraft. " +
                        "Открой «Специальные возможности», найди «Прицел MC» и включи.",
                    Modifier.padding(top = 6.dp, bottom = 10.dp),
                    color = p.textDim, fontSize = 14.sp
                )
                Button(onClick = { a11yDialog = true }) { Text("Как включить") }
            }
        }

        // ---- переключатели ----
        SectionCard {
            SwitchRow("Мини-настройки в игре", s.mini) { v -> store.edit { putBoolean(K.MINI, v) } }
            SwitchRow("Открывать мини-настройки при старте", s.miniOnStart, enabled = s.mini) { v ->
                store.edit { putBoolean(K.MINI_ON_START, v) }
            }
            SwitchRow("Автозапуск в Minecraft", s.autostart) { v ->
                store.edit { putBoolean(K.AUTOSTART, v) }
                if (v && !serviceOn) a11yDialog = true
            }
        }

        // ---- стиль и цвет ----
        SectionCard {
            Spacer(Modifier.height(4.dp))
            Segmented("ВСТРОЕННЫЙ", "СВОЙ", rightSelected = s.custom) { right ->
                if (right && !CustomImage.exists(ctx)) picker.launch("image/*")
                else store.edit { putBoolean(K.CUSTOM, right) }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!s.custom) {
                    BigAction("Стиль", onClick = { styleDialog = true }) {
                        Box(
                            Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(p.tile),
                            contentAlignment = Alignment.Center
                        ) { CrosshairGlyph(s.style, color, Modifier.size(44.dp)) }
                    }
                } else {
                    BigAction("Картинка", onClick = { picker.launch("image/*") }) {
                        Box(
                            Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(p.tile),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBmp != null) {
                                Image(
                                    customBmp, null, Modifier.size(52.dp),
                                    colorFilter = if (s.tintCustom) ColorFilter.tint(color) else null
                                )
                            } else {
                                Icon(Icons.Filled.Image, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
                BigAction("Цвет", enabled = !s.custom || s.tintCustom, onClick = { colorDialog = true }) {
                    Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FormatColorFill, null,
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            if (s.custom) {
                SwitchRow("Перекрашивать картинку", s.tintCustom) { v -> store.edit { putBoolean(K.TINT_CUSTOM, v) } }
            }
        }

        // ---- ползунки ----
        SectionCard {
            PillSlider(
                Icons.Filled.Visibility, "Прозрачность", s.opacity,
                Limits.OPACITY_MIN..1f, 0.01f, { String.format(RU, "%.2f", it) }
            ) { v -> store.edit { putFloat(K.OPACITY, v) } }

            PillSlider(
                Icons.Filled.ZoomOutMap, "Размер", s.sizeDp,
                Limits.SIZE_MIN..Limits.SIZE_MAX, 1f, { "${it.roundToInt()} dp" }
            ) { v -> store.edit { putFloat(K.SIZE, v) } }

            PillSlider(
                Icons.Filled.SwapHoriz, "Вбок", s.offsetXDp,
                -Limits.OFFSET..Limits.OFFSET, 1f, ::signed, stepButtons = true
            ) { v -> store.edit { putFloat(K.OFF_X, v) } }

            PillSlider(
                Icons.Filled.SwapVert, "Вверх/вниз", s.offsetYDp,
                -Limits.OFFSET..Limits.OFFSET, 1f, ::signed, stepButtons = true
            ) { v -> store.edit { putFloat(K.OFF_Y, v) } }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { store.edit { putFloat(K.OFF_X, 0f); putFloat(K.OFF_Y, 0f) } }) {
                    Icon(Icons.Filled.CenterFocusStrong, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("По центру", fontSize = 16.sp)
                }
                TextButton(onClick = {
                    store.edit {
                        putInt(K.COLOR, Defaults.COLOR)
                        putFloat(K.OPACITY, Defaults.OPACITY)
                        putFloat(K.SIZE, Defaults.SIZE)
                        putFloat(K.OFF_X, 0f)
                        putFloat(K.OFF_Y, 0f)
                        putString(K.STYLE, Defaults.STYLE.name)
                    }
                }) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Сброс", fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (a11yDialog) {
        AccessibilityDialog(onDismiss = { a11yDialog = false })
    }
    if (styleDialog) {
        StyleDialog(
            current = s.style,
            color = color,
            onPick = { st -> store.edit { putString(K.STYLE, st.name); putBoolean(K.CUSTOM, false) } },
            onDismiss = { styleDialog = false }
        )
    }
    if (colorDialog) {
        ColorDialog(
            initial = s.color,
            onPick = { c -> store.edit { putInt(K.COLOR, c) } },
            onDismiss = { colorDialog = false }
        )
    }
}

private fun signed(v: Float): String {
    val i = v.roundToInt()
    return if (i > 0) "+$i" else "$i"
}
