package com.pixelsudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.engine.Difficulty
import com.pixelsudoku.engine.LEVELS_PER_DIFFICULTY

private class Ach(val title: String, val cur: Int, val goal: Int, val date: Long?)

@Composable
fun StatsScreen(vm: AppViewModel) {
    val c = LocalPixel.current
    val results by vm.results.collectAsState()
    val st by vm.settings.collectAsState()
    val wins = results.size
    val played = wins + st.losses
    val rate = if (played == 0) 0 else wins * 100 / played
    val avg = if (wins == 0) 0 else results.sumOf { it.seconds } / wins
    val best = results.minOfOrNull { it.seconds }
    val asc = results.sortedBy { it.finishedAt }

    val ach = buildList {
        listOf(1 to "Первое судоку", 5 to "5 побед", 10 to "10 побед", 25 to "25 побед", 100 to "100 побед").forEach { (n, t) ->
            add(Ach(t, minOf(wins, n), n, if (wins >= n) asc[n - 1].finishedAt else null))
        }
        val noErr = asc.firstOrNull { it.mistakes == 0 }
        add(Ach("Победа без ошибок", if (noErr != null) 1 else 0, 1, noErr?.finishedAt))
        val noHint = asc.firstOrNull { it.hints == 0 }
        add(Ach("Победа без подсказок", if (noHint != null) 1 else 0, 1, noHint?.finishedAt))
        val ex = asc.firstOrNull { it.difficulty == Difficulty.EXPERT.ordinal }
        add(Ach("Экспертный уровень", if (ex != null) 1 else 0, 1, ex?.finishedAt))
        val ma = asc.firstOrNull { it.difficulty == Difficulty.MASTER.ordinal }
        add(Ach("Мастер-уровень", if (ma != null) 1 else 0, 1, ma?.finishedAt))
        add(Ach("3 победы подряд", minOf(st.maxStreak, 3), 3, null))
    }

    ScreenFrame("Статистика", { vm.back() }) {
        PixelPanel(Modifier.fillMaxWidth()) {
            PText("Сыграно партий: $played", 14)
            PText("Побед: $wins ($rate%)", 14)
            PText("Среднее время: ${fmtTime(avg)}", 14)
            PText("Лучшее время: ${best?.let { fmtTime(it) } ?: "—"}", 14)
            PText("Ошибок всего: ${results.sumOf { it.mistakes }}", 14)
            PText("Подсказок всего: ${results.sumOf { it.hints }}", 14)
            PText("Серия побед: ${st.streak} (макс. ${st.maxStreak})", 14)
        }
        PText("Пройдено уровней", 15)
        PixelPanel(Modifier.fillMaxWidth()) {
            Difficulty.values().forEach { d ->
                val n = results.filter { it.mode == "level" && it.difficulty == d.ordinal }.map { it.level }.toSet().size
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    PText(d.title, 12, modifier = Modifier.width(84.dp))
                    Box(Modifier.weight(1f).height(16.dp).border(2.dp, c.border)) {
                        if (n > 0) Box(Modifier.fillMaxWidth(n / LEVELS_PER_DIFFICULTY.toFloat()).fillMaxHeight().background(c.accent))
                    }
                    PText(" $n", 12)
                }
            }
        }
        PText("Достижения", 15)
        ach.forEach { a ->
            val done = a.cur >= a.goal
            PixelPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PText(a.title, 13, color = if (done) c.good else c.ink)
                    PText("${a.cur}/${a.goal}", 13)
                }
                if (done) PText("Получено" + (a.date?.let { ": " + fmtDate(it) } ?: ""), 11, color = c.inkSoft)
            }
        }
    }
}
