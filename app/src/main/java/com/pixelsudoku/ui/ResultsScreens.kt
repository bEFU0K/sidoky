package com.pixelsudoku.ui

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.Screen
import com.pixelsudoku.data.GameResult
import com.pixelsudoku.engine.Difficulty
import com.pixelsudoku.engine.LEVELS_PER_DIFFICULTY
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun fmtDate(ms: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date(ms))
fun stars(n: Int) = "★".repeat(n) + "☆".repeat(3 - n)

@Composable
fun WinScreen(vm: AppViewModel) {
    val r = vm.lastResult
    val ctx = LocalContext.current
    ScreenFrame("Судоку решено!", null) {
        if (r == null) { PixelButton("На главную", { vm.screen = Screen.HOME }, Modifier.fillMaxWidth()); return@ScreenFrame }
        val d = Difficulty.values()[r.difficulty]
        PixelPanel(Modifier.fillMaxWidth()) {
            PText(stars(r.stars), 32, color = LocalPixel.current.select, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PText("${d.title}" + if (r.mode == "level") " · уровень ${r.level}" else "", 15)
            PText("Время: ${fmtTime(r.seconds)}", 15)
            PText("Ошибки: ${r.mistakes}", 15)
            PText("Подсказки: ${r.hints}", 15)
            PText("Очки: ${r.score}", 18, color = LocalPixel.current.accent)
        }
        if (r.mode == "level" && r.level < LEVELS_PER_DIFFICULTY)
            PixelButton("Следующий уровень", { vm.requestStart(d, r.level + 1, "level") }, Modifier.fillMaxWidth(), color = LocalPixel.current.good)
        PixelButton("Повторить", { vm.requestStart(d, r.level, r.mode) }, Modifier.fillMaxWidth(), color = LocalPixel.current.accent2)
        PixelButton("Поделиться", {
            val text = "Pixel Sudoku: ${d.title} решено за ${fmtTime(r.seconds)}, ошибок ${r.mistakes}, подсказок ${r.hints}, ${stars(r.stars)}, очки ${r.score}"
            val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
            ctx.startActivity(Intent.createChooser(i, "Поделиться"))
        }, Modifier.fillMaxWidth(), color = LocalPixel.current.inkSoft)
        PixelButton("На главную", { vm.screen = Screen.HOME }, Modifier.fillMaxWidth())
    }
}

@Composable
fun ResultsScreen(vm: AppViewModel) {
    val c = LocalPixel.current
    val results by vm.results.collectAsState()
    var dFilter by remember { mutableIntStateOf(-1) }
    var dateFilter by remember { mutableIntStateOf(0) } // 0 всё, 1 сегодня, 2 7 дней, 3 30 дней
    val now = System.currentTimeMillis()
    val day = 86_400_000L
    val list = results.filter {
        (dFilter == -1 || it.difficulty == dFilter) && when (dateFilter) {
            1 -> now - it.finishedAt < day
            2 -> now - it.finishedAt < 7 * day
            3 -> now - it.finishedAt < 30 * day
            else -> true
        }
    }
    val bestScore = results.groupBy { Triple(it.difficulty, it.level, it.mode) }
        .filterKeys { it.third == "level" }.mapValues { e -> e.value.maxOf { it.score } }

    ScreenFrame("Мои результаты", { vm.back() }) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton("Все", { dFilter = -1 }, selected = dFilter == -1, size = 12, color = c.inkSoft)
            Difficulty.values().forEach { PixelButton(it.title, { dFilter = it.ordinal }, selected = dFilter == it.ordinal, size = 12, color = c.inkSoft) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Всё время", "Сегодня", "7 дней", "30 дней").forEachIndexed { i, t ->
                PixelButton(t, { dateFilter = i }, selected = dateFilter == i, size = 12, color = c.inkSoft)
            }
        }
        if (list.isEmpty()) {
            PixelPanel(Modifier.fillMaxWidth()) {
                PText(if (results.isEmpty()) "Вы ещё ничего не прошли. Сыграйте первую партию!" else "Нет результатов по выбранным фильтрам", 14, color = c.inkSoft)
            }
        }
        list.forEach { r -> ResultCard(r, r.mode == "level" && bestScore[Triple(r.difficulty, r.level, r.mode)] == r.score) }
    }
}

@Composable
fun ResultCard(r: GameResult, isBest: Boolean) {
    val c = LocalPixel.current
    val d = Difficulty.values()[r.difficulty]
    val name = when (r.mode) { "daily" -> "Ежедневное"; "random" -> "${d.title} · случайная"; else -> "${d.title} · уровень ${r.level}" }
    PixelPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PText(name, 14)
            PText(stars(r.stars), 14, color = c.accent)
        }
        PText(fmtDate(r.finishedAt), 11, color = c.inkSoft)
        PText("Время ${fmtTime(r.seconds)} · ошибки ${r.mistakes} · подсказки ${r.hints}", 12)
        PText("Очки: ${r.score}" + if (isBest) "  ★ рекорд уровня" else "", 13, color = if (isBest) c.good else c.ink)
    }
}
