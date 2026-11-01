package com.mccrosshair.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AccessibilityDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Включи службу «Прицел MC»") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "1. Нажми «Открыть» → «Специальные возможности».\n" +
                        "2. Найди «Прицел MC» (иногда в разделе «Скачанные приложения») и включи.\n\n" +
                        "Если переключатель серый или пишет «Ограниченная настройка»:\n" +
                        "• нажми «О приложении»;\n" +
                        "• вверху справа ⋮ → «Разрешить ограниченные настройки»;\n" +
                        "• вернись и включи службу ещё раз."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { SystemScreens.accessibility(ctx); onDismiss() }) { Text("Открыть") }
        },
        dismissButton = {
            TextButton(onClick = { SystemScreens.appDetails(ctx) }) { Text("О приложении") }
        }
    )
}

@Composable
fun StyleDialog(current: CrossStyle, color: Color, onPick: (CrossStyle) -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Стиль прицела") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CrossStyle.entries.forEach { st ->
                    val shape = RoundedCornerShape(24.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .clip(shape)
                                .background(p.tile)
                                .then(if (st == current) Modifier.border(3.dp, p.accent, shape) else Modifier)
                                .clickable { onPick(st) },
                            contentAlignment = Alignment.Center
                        ) { CrosshairGlyph(st, color, Modifier.size(48.dp)) }
                        Spacer(Modifier.height(8.dp))
                        Text(if (st == CrossStyle.DOT) "Точка" else "Крестик")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

// ---------- цвет ----------

private val PRESETS = longArrayOf(
    0xFFFFFFFF, 0xFFFF3B30, 0xFF00FF66, 0xFF00E5FF, 0xFFFFEB3B,
    0xFFE92FAA, 0xFFFF9500, 0xFFB388FF, 0xFF2979FF, 0xFF000000,
).map { it.toInt() }

private fun toHex(c: Int) = String.format("#%06X", c and 0xFFFFFF)

@Composable
fun ColorDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(toHex(initial)) }

    fun hsv(hh: Float = h, ss: Float = sat, vv: Float = v) =
        android.graphics.Color.HSVToColor(floatArrayOf(hh, ss, vv))

    fun setColor(c: Int, updateText: Boolean = true) {
        val a = FloatArray(3)
        android.graphics.Color.colorToHSV(c, a)
        h = a[0]; sat = a[1]; v = a[2]
        if (updateText) hex = toHex(c)
    }

    val current = hsv()
    val sliderColors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Цвет прицела") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(current))
                )
                Spacer(Modifier.height(12.dp))
                PRESETS.chunked(5).forEach { rowColors ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowColors.forEach { c ->
                            val sel = (c and 0xFFFFFF) == (current and 0xFFFFFF)
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(if (sel) 3.dp else 1.dp, if (sel) p.accent else p.textDim, CircleShape)
                                    .clickable { setColor(c) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Оттенок", fontSize = 13.sp, color = p.textDim)
                Slider(h, { h = it; hex = toHex(hsv(hh = it)) }, valueRange = 0f..360f, colors = sliderColors)
                Text("Насыщенность", fontSize = 13.sp, color = p.textDim)
                Slider(sat, { sat = it; hex = toHex(hsv(ss = it)) }, colors = sliderColors)
                Text("Яркость", fontSize = 13.sp, color = p.textDim)
                Slider(v, { v = it; hex = toHex(hsv(vv = it)) }, colors = sliderColors)
                OutlinedTextField(
                    value = hex,
                    onValueChange = { t ->
                        hex = t.take(7)
                        val clean = if (t.startsWith("#")) t else "#$t"
                        if (clean.length == 7) {
                            runCatching { android.graphics.Color.parseColor(clean) }
                                .getOrNull()?.let { setColor(it, updateText = false) }
                        }
                    },
                    label = { Text("HEX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current or 0xFF000000.toInt()); onDismiss() }) { Text("Готово") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ---------- выбор приложений ----------

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

@Suppress("DEPRECATION")
private fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .asSequence()
        .filter { it.activityInfo.packageName != ctx.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { ri ->
            AppEntry(
                pkg = ri.activityInfo.packageName,
                label = ri.loadLabel(pm).toString(),
                icon = runCatching { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull()
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}

@Composable
fun AppPickerDialog(selected: Set<String>, onSave: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var chosen by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(ctx) }
    }

    fun toggle(pkg: String) {
        chosen = if (pkg in chosen) chosen - pkg else chosen + pkg
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Приложения") },
        text = {
            Column {
                Text(
                    "Прицел включится сам (Автозапуск) или будет виден (Фильтр) только в отмеченных приложениях.",
                    color = p.textDim, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val list = apps
                if (list == null) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val shown = list
                        .filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
                        .sortedByDescending { it.pkg in selected }
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(shown, key = { it.pkg }) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { toggle(app.pkg) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(app.icon, null, Modifier.size(36.dp))
                                } else {
                                    Spacer(Modifier.size(36.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        app.pkg, color = p.textDim, fontSize = 11.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = app.pkg in chosen,
                                    onCheckedChange = { toggle(app.pkg) },
                                    colors = CheckboxDefaults.colors(checkedColor = p.accent)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(chosen); onDismiss() }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
