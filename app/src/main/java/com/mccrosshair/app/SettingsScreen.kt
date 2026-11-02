package com.mccrosshair.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(store: SettingsStore, serviceOn: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = p.text) }
            Spacer(Modifier.width(8.dp))
            Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }

        SectionCard {
            Text("Служба прицела", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                if (serviceOn) "Включена ✓" else "Выключена",
                Modifier.padding(top = 4.dp, bottom = 10.dp),
                color = if (serviceOn) p.accent else p.warn
            )
            Button(onClick = { SystemScreens.accessibility(ctx) }) { Text("Специальные возможности") }
            Text(
                "Не включается («Ограниченная настройка»)? Открой «О приложении» → ⋮ → " +
                    "«Разрешить ограниченные настройки» и попробуй снова.",
                Modifier.padding(top = 10.dp, bottom = 8.dp),
                color = p.textDim, fontSize = 14.sp
            )
            OutlinedButton(onClick = { SystemScreens.appDetails(ctx) }) { Text("О приложении") }
        }

        SectionCard {
            Text("Проверка автозапуска", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            val last = CrosshairService.lastApp
            val inList = last != null && last in store.state.targets
            Text(
                when {
                    !serviceOn -> "Сначала включи службу."
                    last == null -> "Открой игру, потом вернись сюда — здесь появится её пакет."
                    else -> "Последнее открытое приложение:\n$last" +
                        if (inList) "\n✓ уже в списке" else "\n✗ не в списке — прицел в нём не включится"
                },
                Modifier.padding(top = 4.dp, bottom = 10.dp),
                color = p.textDim, fontSize = 14.sp
            )
            if (last != null && !inList) {
                Button(onClick = {
                    val set = store.state.targets + last
                    store.edit { putString(K.TARGETS, set.joinToString("\n")) }
                }) { Text("Добавить в список") }
            }
        }

        SectionCard {
            Text("Чтобы телефон не выключал прицел", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "На Xiaomi, Huawei, Samsung и других система может отключать службу ради экономии " +
                    "батареи. Найди «Прицел MC» в списке и выбери «Без ограничений».",
                Modifier.padding(top = 4.dp, bottom = 10.dp),
                color = p.textDim, fontSize = 14.sp
            )
            OutlinedButton(onClick = { SystemScreens.battery(ctx) }) { Text("Экономия батареи") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { SystemScreens.miuiAutostart(ctx) }) { Text("Автозапуск (Xiaomi)") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { SystemScreens.overlay(ctx) }) { Text("Поверх других приложений") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
