package com.pixelsudoku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.data.Keys

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        PText(label, 13, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val c = LocalPixel.current
    val s by vm.settings.collectAsState()
    var dlg by remember { mutableIntStateOf(0) } // 1 статистика, 2 партия, 3/4 всё
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0.0" }

    ScreenFrame("Настройки", { vm.back() }) {
        PText("Тема", 15)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Системная", "Светлая", "Тёмная").forEachIndexed { i, t ->
                PixelButton(t, { vm.setPref(Keys.THEME, i) }, Modifier.weight(1f), selected = s.theme == i, size = 11, hpad = 2, color = c.inkSoft)
            }
        }
        PText("Звук", 15)
        PixelPanel(Modifier.fillMaxWidth()) {
            ToggleRow("Звуки", s.sound) { vm.setPref(Keys.SOUND, it) }
            var v by remember(s.volume) { mutableFloatStateOf(s.volume) }
            PText("Громкость эффектов", 12)
            Slider(value = v, onValueChange = { v = it }, onValueChangeFinished = { vm.setPref(Keys.VOLUME, v) })
            PixelButton("Проверить звук", { vm.previewSound() }, Modifier.fillMaxWidth(), size = 13, color = c.accent2)
            ToggleRow("Вибрация", s.vibration) { vm.setPref(Keys.VIBRATION, it) }
        }
        PText("Игра", 15)
        PixelPanel(Modifier.fillMaxWidth()) {
            ToggleRow("Автопроверка ошибок (неверная цифра не вводится)", s.autoCheck) { vm.setPref(Keys.AUTO_CHECK, it) }
            ToggleRow("Подсветка одинаковых цифр", s.hlSame) { vm.setPref(Keys.HL_SAME, it) }
            ToggleRow("Подсветка строки, столбца и квадрата", s.hlRegion) { vm.setPref(Keys.HL_REGION, it) }
            ToggleRow("Показывать таймер", s.showTimer) { vm.setPref(Keys.SHOW_TIMER, it) }
        }
        PText("Данные", 15)
        PixelButton("Сбросить статистику", { dlg = 1 }, Modifier.fillMaxWidth(), size = 13, color = c.error)
        PixelButton("Удалить сохранённую партию", { dlg = 2 }, Modifier.fillMaxWidth(), size = 13, color = c.error)
        PixelButton("Сбросить все данные", { dlg = 3 }, Modifier.fillMaxWidth(), size = 13, color = c.error)
        PixelPanel(Modifier.fillMaxWidth()) {
            PText("О приложении", 14)
            PText("Pixel Sudoku, версия $version", 12)
            PText("Работает без интернета. Реклама, регистрация и сбор данных отсутствуют: всё хранится только на устройстве.", 11, color = c.inkSoft)
        }
    }
    when (dlg) {
        1 -> ConfirmDialog("Сбросить статистику?", "Результаты, достижения и серии побед будут удалены.", "Сбросить", { vm.resetStats(); dlg = 0 }, { dlg = 0 })
        2 -> ConfirmDialog("Удалить партию?", "Незавершённая партия будет удалена.", "Удалить", { vm.deleteSaved(); dlg = 0 }, { dlg = 0 })
        3 -> ConfirmDialog("Сбросить все данные?", "Будут удалены партии, результаты и настройки.", "Далее", { dlg = 4 }, { dlg = 0 })
        4 -> ConfirmDialog("Точно удалить всё?", "Это действие нельзя отменить.", "Удалить всё", { vm.resetAll(); dlg = 0 }, { dlg = 0 })
    }
}
