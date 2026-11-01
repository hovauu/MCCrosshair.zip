package com.mccrosshair.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- переходы в системные настройки ----------

object SystemScreens {
    private fun Context.go(i: Intent) {
        runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun accessibility(ctx: Context) = ctx.go(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun appDetails(ctx: Context) = ctx.go(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    )

    fun battery(ctx: Context) = ctx.go(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

// ---------- прицел для превью ----------

@Composable
fun CrosshairGlyph(style: CrossStyle, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = center
        when (style) {
            CrossStyle.DOT -> drawCircle(color, radius = s * 0.15f, center = c)
            CrossStyle.CROSS -> {
                val t = max(1.5.dp.toPx(), s * 0.07f)
                drawRect(color, topLeft = Offset(c.x - s / 2f, c.y - t / 2f), size = Size(s, t))
                drawRect(color, topLeft = Offset(c.x - t / 2f, c.y - s / 2f), size = Size(t, s))
            }
        }
    }
}

// ---------- блоки интерфейса ----------

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(p.card)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f).padding(end = 12.dp), color = p.text, fontSize = 18.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = p.bg)
        )
    }
}

@Composable
fun BigAction(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(10.dp))
        Text(label, color = p.text, fontSize = 18.sp)
    }
}

/** Кнопка-«таблетка» как на скриншотах: иконка + текст акцентного цвета. */
@Composable
fun PillButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(p.btn)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = p.accent, fontSize = 17.sp)
    }
}

/** Кнопка «Сброс», прижатая вправо. */
@Composable
fun ResetRow(text: String = "Сброс", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Spacer(Modifier.weight(1f))
        PillButton(text, Icons.Filled.Refresh, onClick = onClick)
    }
}

/**
 * Ползунок-плашка: вся плашка — шкала, заливка показывает значение.
 * Тянуть пальцем или нажимать в нужное место.
 */
@Composable
fun PillSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    val p = LocalPalette.current
    val cb by rememberUpdatedState(onChange)
    val span = range.endInclusive - range.start

    fun snap(v: Float) = ((v / step).roundToInt() * step).coerceIn(range.start, range.endInclusive)
    fun fromX(x: Float, width: Int) = snap(range.start + (x / width).coerceIn(0f, 1f) * span)

    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.pill)
            .pointerInput(range) {
                detectTapGestures { o -> cb(fromX(o.x, size.width)) }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    cb(fromX(change.position.x, size.width))
                }
            }
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(p.pillFill))
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = p.text, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(format(value), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Плашка «X | 1» как на скриншоте. */
@Composable
fun ValueChip(label: String, value: Int) {
    val p = LocalPalette.current
    Row(
        Modifier.padding(vertical = 5.dp).height(40.dp).clip(RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.fillMaxHeight().background(p.accent).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) { Text(label, color = p.bg, fontSize = 17.sp) }
        Box(
            Modifier.fillMaxHeight().background(p.btn).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) { Text(value.toString(), color = p.text, fontSize = 17.sp) }
    }
}

/** Круглая кнопка-стрелка. */
@Composable
fun ArrowButton(icon: ImageVector, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(p.btn)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = p.accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun Gap(size: Int) = Spacer(Modifier.size(size.dp))

@Suppress("unused")
private fun Modifier.dim(enabled: Boolean) = this.alpha(if (enabled) 1f else 0.4f)
