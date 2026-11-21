package com.pixelsudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.Screen
import com.pixelsudoku.engine.Difficulty
import kotlinx.coroutines.delay

@Composable
fun Logo() {
    val c = LocalPixel.current
    var hi by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(500); hi = (hi + 1) % 9 } }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.border(4.dp, c.border)) {
            for (r in 0..2) Row {
                for (k in 0..2) {
                    val i = r * 3 + k
                    Box(
                        Modifier.size(44.dp).background(if (i == hi) c.select else c.cell).border(1.dp, c.border),
                        contentAlignment = Alignment.Center
                    ) { PText("${i + 1}", 22, color = if (i == hi) c.ink else c.accent2) }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val saved by vm.saved.collectAsState()
    val st by vm.settings.collectAsState()
    val results by vm.results.collectAsState()
    val c = LocalPixel.current
    ScreenFrame("PIXEL SUDOKU", null) {
        Logo()
        if (saved != null) {
            val s = saved!!
            PixelButton(
                "Продолжить: ${Difficulty.values()[s.difficulty].title}", { vm.continueGame() },
                Modifier.fillMaxWidth(), color = c.good
            )
        }
        PixelButton("Новая игра", { vm.screen = Screen.NEW }, Modifier.fillMaxWidth())
        PixelButton("Уровни", { vm.screen = Screen.LEVELS }, Modifier.fillMaxWidth())
        PixelButton("Мои результаты", { vm.screen = Screen.RESULTS }, Modifier.fillMaxWidth())
        PixelButton("Статистика", { vm.screen = Screen.STATS }, Modifier.fillMaxWidth())
        PixelButton("Настройки", { vm.screen = Screen.SETTINGS }, Modifier.fillMaxWidth())
        PixelPanel(Modifier.fillMaxWidth()) {
            PText("Серия побед: ${st.streak}", 15)
            PText("Решено судоку: ${results.size}", 15)
        }
    }
}

@Composable
fun NewGameScreen(vm: AppViewModel) {
    var d by remember { mutableStateOf(Difficulty.MEDIUM) }
    var rules by remember { mutableStateOf(false) }
    val results by vm.results.collectAsState()
    val dailyDone = results.any { it.mode == "daily" && it.level == vm.dailyCode() }
    ScreenFrame("Новая игра", { vm.back() }) {
        PText("Сложность", 15)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.values().forEach {
                PixelButton(it.title, { d = it }, selected = d == it, size = 13)
            }
        }
        PixelButton("Играть: уровень ${vm.nextLevelFor(d)}", { vm.requestStart(d, vm.nextLevelFor(d), "level") }, Modifier.fillMaxWidth())
        PixelButton("Случайная головоломка", { vm.requestStart(d, 0, "random") }, Modifier.fillMaxWidth(), color = LocalPixel.current.accent2)
        PixelButton(
            if (dailyDone) "Ежедневное судоку (решено)" else "Ежедневное судоку",
            { vm.requestStart(Difficulty.MEDIUM, vm.dailyCode(), "daily") },
            Modifier.fillMaxWidth(), color = LocalPixel.current.good
        )
        PixelButton("Правила", { rules = true }, Modifier.fillMaxWidth(), color = LocalPixel.current.inkSoft)
    }
    if (rules) AlertDialog(
        onDismissRequest = { rules = false },
        title = { Text("Правила судоку") },
        text = {
            Text(
                "Заполните сетку 9×9 цифрами от 1 до 9 так, чтобы в каждой строке, каждом столбце и " +
                    "каждом квадрате 3×3 цифры не повторялись.\n\nВыберите клетку и нажмите цифру. " +
                    "«Заметки» — маленькие цифры-кандидаты. «Подсказка» ставит верную цифру, но снижает очки."
            )
        },
        confirmButton = { TextButton(onClick = { rules = false }) { Text("Понятно") } }
    )
}
