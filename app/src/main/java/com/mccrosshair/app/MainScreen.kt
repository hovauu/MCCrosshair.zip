package com.mccrosshair.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val RU = Locale.forLanguageTag("ru")

@Composable
fun MainScreen(store: SettingsStore, serviceOn: Boolean, openSettings: () -> Unit) {
    val s = store.state
    val p = LocalPalette.current
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var a11yDialog by remember { mutableStateOf(false) }
    var styleDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var appsDialog by remember { mutableStateOf(false) }

    val color = Color(s.color)

    fun move(dx: Int, dy: Int) = store.edit {
        putInt(K.POS_X, (s.posX + dx).coerceIn(-Limits.POS_MAX, Limits.POS_MAX))
        putInt(K.POS_Y, (s.posY + dy).coerceIn(-Limits.POS_MAX, Limits.POS_MAX))
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // ---- верхняя панель (без названия) ----
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { store.edit { putBoolean(K.DARK, !s.dark) } }) {
                Icon(Icons.Filled.Brightness6, "Тема", tint = p.text)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = openSettings) {
                Icon(Icons.Filled.Settings, "Настройки", tint = p.text)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- START / STOP ----
        OutlinedButton(
            onClick = {
                if (!serviceOn) a11yDialog = true
                else {
                    store.prefs.edit().putBoolean(K.ENABLED, !s.enabled).commit()
                    CrosshairService.instance?.refresh()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).width(140.dp).height(52.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(2.5.dp, p.accent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (s.enabled) p.accent else Color.Transparent,
                contentColor = if (s.enabled) p.bg else p.text
            )
        ) {
            Text(if (s.enabled) "STOP" else "START", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(28.dp))

        // ---- служба выключена ----
        if (!serviceOn) {
            SectionCard {
                Text("Включи службу «Прицел MC»", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "Без неё прицел не сможет появиться поверх игры.",
                    Modifier.padding(top = 6.dp, bottom = 10.dp),
                    color = p.textDim, fontSize = 14.sp
                )
                Button(onClick = { a11yDialog = true }) { Text("Как включить") }
            }
        } else if (!CrosshairService.running) {
            // Служба включена в настройках, но система её не запустила (частая история на Xiaomi)
            SectionCard {
                Text("Служба включена, но не работает", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = p.warn)
                Text(
                    "Телефон не запустил службу «Прицел MC». Так бывает после переустановки.\n\n" +
                        "1. Открой «Спец. возможности», выключи «Прицел MC» и включи снова.\n" +
                        "2. На Xiaomi: разреши приложению «Автозапуск».\n" +
                        "3. Если не помогло — перезагрузи телефон.",
                    Modifier.padding(top = 6.dp, bottom = 10.dp),
                    color = p.textDim, fontSize = 14.sp
                )
                Row {
                    Button(onClick = { SystemScreens.accessibility(ctx) }) { Text("Спец. возможности") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { SystemScreens.miuiAutostart(ctx) }) { Text("Автозапуск") }
                }
            }
        } else if (CrosshairService.lastError != null && !CrosshairService.visible && (s.enabled || s.autostart)) {
            SectionCard {
                Text("Прицел не смог появиться", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = p.warn)
                Text(
                    "Разреши «Поверх других приложений» — это запасной способ показать прицел.",
                    Modifier.padding(top = 6.dp, bottom = 6.dp),
                    color = p.textDim, fontSize = 14.sp
                )
                Text(CrosshairService.lastError ?: "", color = p.textDim, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { SystemScreens.overlay(ctx) }) { Text("Разрешить") }
            }
        } else if (s.enabled && s.filter && !CrosshairService.visible) {
            SectionCard {
                Text(
                    "Включён «Фильтр приложений»: прицел виден только в выбранных приложениях " +
                        "(например, в Minecraft), а здесь его не будет.",
                    color = p.textDim, fontSize = 14.sp
                )
            }
        }

        // ---- автозапуск ----
        SectionCard {
            SwitchRow("Автозапуск", s.autostart) { v ->
                store.edit { putBoolean(K.AUTOSTART, v) }
                if (v && !serviceOn) a11yDialog = true
            }
        }

        // ---- стиль и цвет ----
        SectionCard {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigAction("Стиль", onClick = { styleDialog = true }) {
                    Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(p.tile),
                        contentAlignment = Alignment.Center
                    ) { CrosshairGlyph(s.style, color, Modifier.size(44.dp).rotate(s.rotation)) }
                }
                BigAction("Цвет", onClick = { colorDialog = true }) {
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
        }

        // ---- прозрачность, поворот, размер ----
        SectionCard {
            PillSlider(
                Icons.Filled.Visibility, "Прозрачность", s.opacity,
                Limits.OPACITY_MIN..1f, 0.01f, { String.format(RU, "%.2f", it) }
            ) { v -> store.edit { putFloat(K.OPACITY, v) } }
            ResetRow { store.edit { putFloat(K.OPACITY, Defaults.OPACITY) } }

            PillSlider(
                Icons.Filled.RotateRight, "Поворот", s.rotation,
                0f..360f, 1f, { "${it.roundToInt()}°" }
            ) { v -> store.edit { putFloat(K.ROTATION, v) } }
            ResetRow { store.edit { putFloat(K.ROTATION, Defaults.ROTATION) } }

            PillSlider(
                Icons.Filled.PhotoSizeSelectSmall, "Размер", s.sizePct,
                Limits.SIZE_MIN..Limits.SIZE_MAX, 1f, { "${it.roundToInt()}%" }
            ) { v -> store.edit { putFloat(K.SIZE, v) } }
            ResetRow { store.edit { putFloat(K.SIZE, Defaults.SIZE) } }
        }

        // ---- фильтр приложений ----
        SectionCard {
            SwitchRow("Фильтр приложений", s.filter) { v -> store.edit { putBoolean(K.FILTER, v) } }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                Spacer(Modifier.weight(1f))
                PillButton("Выбрать приложения", Icons.Filled.Apps) { appsDialog = true }
            }
        }

        // ---- положение ----
        SectionCard {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    ValueChip("X", s.posX)
                    ValueChip("Y", s.posY)
                    Spacer(Modifier.height(8.dp))
                    StepField(s.step) { v -> store.edit { putInt(K.STEP, v) } }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ArrowButton(Icons.Filled.ArrowUpward) { move(0, -s.step) }
                    Row {
                        ArrowButton(Icons.AutoMirrored.Filled.ArrowBack) { move(-s.step, 0) }
                        Spacer(Modifier.width(56.dp))
                        ArrowButton(Icons.AutoMirrored.Filled.ArrowForward) { move(s.step, 0) }
                    }
                    ArrowButton(Icons.Filled.ArrowDownward) { move(0, s.step) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                PillButton("Сброс X", Icons.Filled.Refresh) { store.edit { putInt(K.POS_X, 0) } }
                Spacer(Modifier.width(8.dp))
                PillButton("Сброс Y", Icons.Filled.Refresh) { store.edit { putInt(K.POS_Y, 0) } }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (a11yDialog) AccessibilityDialog(onDismiss = { a11yDialog = false })
    if (styleDialog) {
        StyleDialog(
            current = s.style,
            color = color,
            onPick = { st -> store.edit { putString(K.STYLE, st.name) } },
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
    if (appsDialog) {
        AppPickerDialog(
            selected = s.targets,
            onSave = { set -> store.edit { putString(K.TARGETS, set.joinToString("\n")) } },
            onDismiss = { appsDialog = false }
        )
    }
}

/** Поле «Шаг»: на сколько пикселей сдвигает одно нажатие стрелки. */
@Composable
private fun StepField(step: Int, onChange: (Int) -> Unit) {
    val p = LocalPalette.current
    var text by remember { mutableStateOf(step.toString()) }
    Column(
        Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(p.btn)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text("Шаг", color = p.textDim, fontSize = 14.sp)
        BasicTextField(
            value = text,
            onValueChange = { t ->
                val digits = t.filter { it.isDigit() }.take(4)
                text = digits
                digits.toIntOrNull()?.takeIf { it > 0 }?.let(onChange)
            },
            singleLine = true,
            textStyle = TextStyle(color = p.text, fontSize = 20.sp),
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}
