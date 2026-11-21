package com.pixelsudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.engine.Difficulty
import com.pixelsudoku.engine.LEVELS_PER_DIFFICULTY

@Composable
fun LevelsScreen(vm: AppViewModel) {
    val c = LocalPixel.current
    val results by vm.results.collectAsState()
    val saved by vm.saved.collectAsState()
    var d by remember { mutableStateOf(Difficulty.EASY) }
    var filter by remember { mutableIntStateOf(-1) } // -1 все, 0 не начат, 1 в процессе, 2 пройден

    val best = remember(results, d) {
        results.filter { it.mode == "level" && it.difficulty == d.ordinal }
            .groupBy { it.level }.mapValues { e -> e.value.minOf { it.seconds } }
    }
    fun status(n: Int): Int = when {
        n in best -> 2
        saved?.let { it.mode == "level" && it.difficulty == d.ordinal && it.level == n } == true -> 1
        else -> 0
    }
    val current = vm.nextLevelFor(d)
    val levels = (1..LEVELS_PER_DIFFICULTY).filter { filter == -1 || status(it) == filter }

    ScreenFrame("Уровни", { vm.back() }, scroll = false) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.values().forEach { PixelButton(it.title, { d = it }, selected = d == it, size = 13) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-1 to "Все", 0 to "Не начат", 1 to "В процессе", 2 to "Пройден").forEach { (k, t) ->
                PixelButton(t, { filter = k }, selected = filter == k, size = 12, color = c.inkSoft)
            }
        }
        if (levels.isEmpty()) PText("Нет уровней с таким статусом", 14, color = c.inkSoft)
        LazyVerticalGrid(
            GridCells.Fixed(4), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(levels) { n ->
                val s = status(n)
                val bg = when (s) { 2 -> c.good; 1 -> c.accent; else -> c.panel }
                Column(
                    Modifier.fillMaxWidth().height(64.dp)
                        .background(bg)
                        .border(if (n == current) 5.dp else 3.dp, if (n == current) c.accent2 else c.border)
                        .clickable { vm.requestStart(d, n, "level") },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    PText("$n", 18, color = if (s == 0) c.ink else androidx.compose.ui.graphics.Color.White)
                    when (s) {
                        2 -> PText(fmtTime(best[n] ?: 0), 11, color = androidx.compose.ui.graphics.Color.White)
                        1 -> PText("идёт", 11, color = androidx.compose.ui.graphics.Color.White)
                        else -> if (n == current) PText("текущий", 10, color = c.accent2)
                    }
                }
            }
        }
    }
}
