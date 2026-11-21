package com.pixelsudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelsudoku.AppViewModel
import com.pixelsudoku.data.Settings
import com.pixelsudoku.engine.GameState
import com.pixelsudoku.engine.Sudoku
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(vm: AppViewModel) {
    val g by vm.game.collectAsState()
    val s by vm.settings.collectAsState()
    val game = g ?: return
    val c = LocalPixel.current
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmRestart by remember { mutableStateOf(false) }

    LaunchedEffect(game.paused) {
        if (!game.paused) while (isActive) { delay(1000); vm.tick() }
    }

    val title = when (game.mode) {
        "daily" -> "Ежедневное"
        "random" -> "${game.difficulty.title} · случайная"
        else -> "${game.difficulty.title} · №${game.level}"
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        val side = maxOf(200.dp, minOf(maxWidth - 20.dp, maxHeight - 290.dp))
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PText(title, 15)
                if (s.showTimer) PText(fmtTime(game.seconds), 18)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PText("Ошибки: ${game.mistakes}", 12, color = c.error)
                PText("Пусто: ${game.empty}", 12)
                PText("Подсказки: ${game.hints}", 12, color = c.accent2)
            }
            if (game.paused) {
                Box(Modifier.size(side).background(c.panel).border(3.dp, c.border), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PText("ПАУЗА", 24)
                        PixelButton("Продолжить", { vm.togglePause() }, Modifier.width(180.dp))
                    }
                }
            } else {
                Board(game, s, side) { vm.select(it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelButton("Заметки", { vm.toggleNotes() }, Modifier.weight(1f), selected = game.notesMode, size = 11, hpad = 2)
                PixelButton("Стереть", { vm.erase() }, Modifier.weight(1f), size = 11, hpad = 2, color = c.inkSoft)
                PixelButton("Отмена", { vm.undo() }, Modifier.weight(1f), enabled = game.undo.isNotEmpty(), size = 11, hpad = 2, color = c.inkSoft)
                PixelButton("Повтор", { vm.redo() }, Modifier.weight(1f), enabled = game.redo.isNotEmpty(), size = 11, hpad = 2, color = c.inkSoft)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelButton("Подсказка", { vm.hint() }, Modifier.weight(1f), size = 11, hpad = 2, color = c.accent2)
                PixelButton("Проверить", { vm.check() }, Modifier.weight(1f), size = 11, hpad = 2, color = c.good)
                PixelButton("Пауза", { vm.togglePause() }, Modifier.weight(1f), size = 11, hpad = 2, color = c.inkSoft)
                PixelButton("Выход", { confirmFinish = true }, Modifier.weight(1f), size = 11, hpad = 2, color = c.error)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (n in 1..9) PixelButton("$n", { vm.input(n) }, Modifier.weight(1f), size = 18, hpad = 0)
            }
            PixelButton("Начать партию заново", { confirmRestart = true }, Modifier.fillMaxWidth(), size = 11, color = c.inkSoft)
        }
    }

    if (confirmFinish) ConfirmDialog(
        "Завершить игру?", "Партия будет засчитана как незавершённая, серия побед сбросится.",
        "Завершить", { confirmFinish = false; vm.finishGame() }, { confirmFinish = false }
    )
    if (confirmRestart) ConfirmDialog(
        "Начать заново?", "Весь прогресс этой партии будет сброшен.",
        "Заново", { confirmRestart = false; vm.restart() }, { confirmRestart = false }
    )
}

@Composable
fun Board(game: GameState, s: Settings, side: Dp, onSelect: (Int) -> Unit) {
    val c = LocalPixel.current
    val cell = side / 9
    val sel = game.selected
    val selV = if (sel >= 0) game.cells[sel] else 0
    Box(Modifier.size(side).border(3.dp, c.border)) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until 9) {
                        val i = r * 9 + col
                        val v = game.cells[i]
                        val fixed = game.puzzle[i] != 0
                        val inRegion = sel >= 0 && s.hlRegion && (i == sel || Sudoku.PEERS[sel].contains(i))
                        val bg = when {
                            i == sel -> c.select
                            s.hlSame && selV != 0 && v == selV -> c.same
                            inRegion -> c.region
                            fixed -> c.cellFixed
                            else -> c.cell
                        }
                        val bad = v != 0 && Sudoku.PEERS[i].any { game.cells[it] == v }
                        Box(
                            Modifier.weight(1f).fillMaxHeight().background(bg)
                                .border(0.5.dp, c.border.copy(alpha = 0.4f))
                                .clickable { onSelect(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (v != 0) {
                                PText("$v", (cell.value * 0.55f).toInt(), color = if (bad) c.error else if (fixed) c.ink else c.accent2)
                            } else if (game.notes[i] != 0) {
                                Notes(game.notes[i], cell)
                            }
                        }
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val st = 3.dp.toPx()
            for (k in 1..2) {
                val p = size.width * k / 3
                drawRect(c.border, Offset(p - st / 2, 0f), Size(st, size.height))
                drawRect(c.border, Offset(0f, p - st / 2), Size(size.width, st))
            }
        }
    }
}

@Composable
fun Notes(mask: Int, cell: Dp) {
    val c = LocalPixel.current
    Column(Modifier.fillMaxSize()) {
        for (r in 0..2) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (k in 0..2) {
                    val n = r * 3 + k + 1
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if ((mask and (1 shl n)) != 0) {
                            PText("$n", (cell.value * 0.26f).toInt().coerceAtLeast(6), color = c.inkSoft)
                        }
                    }
                }
            }
        }
    }
}
