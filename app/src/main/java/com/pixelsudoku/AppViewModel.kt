package com.pixelsudoku

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelsudoku.data.*
import com.pixelsudoku.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class Screen { HOME, NEW, LEVELS, GAME, WIN, RESULTS, STATS, SETTINGS }

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = GameDb.get(app).dao()
    private val store = SettingsStore(app)

    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val saved: StateFlow<SavedGame?> = dao.observeSaved().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val results: StateFlow<List<GameResult>> =
        dao.observeResults().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val game = MutableStateFlow<GameState?>(null)

    var screen by mutableStateOf(Screen.HOME)
    var loading by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    var lastResult by mutableStateOf<GameResult?>(null)
    var confirmStart by mutableStateOf<(() -> Unit)?>(null)

    // ---------- навигация ----------
    fun back() {
        if (screen == Screen.GAME) persist()
        screen = Screen.HOME
    }

    // ---------- запуск партий ----------
    fun dailyCode(): Int = LocalDate.now().let { it.year * 10000 + it.monthValue * 100 + it.dayOfMonth }

    private fun seedFor(d: Difficulty, level: Int, mode: String): Long = when (mode) {
        "level" -> d.ordinal * 100000L + level + 7
        "daily" -> level.toLong() * 31
        else -> System.nanoTime()
    }

    fun nextLevelFor(d: Difficulty): Int {
        val done = results.value.filter { it.mode == "level" && it.difficulty == d.ordinal }.map { it.level }.toSet()
        return (1..LEVELS_PER_DIFFICULTY).firstOrNull { it !in done } ?: 1
    }

    fun requestStart(d: Difficulty, level: Int, mode: String) {
        val s = saved.value
        if (s != null && s.difficulty == d.ordinal && s.level == level && s.mode == mode && mode != "random") {
            continueGame(); return
        }
        if (s != null) confirmStart = { start(d, level, mode) } else start(d, level, mode)
    }

    fun start(d: Difficulty, level: Int, mode: String) {
        confirmStart = null
        viewModelScope.launch {
            loading = true
            val p = withContext(Dispatchers.Default) { Sudoku.generate(seedFor(d, level, mode), d.clues) }
            game.value = GameState(d, level, mode, p.puzzle, p.solution, p.puzzle.copyOf(), IntArray(81), 0, 0, 0)
            loading = false
            screen = Screen.GAME
            persist()
        }
    }

    fun continueGame() {
        val s = saved.value ?: return
        game.value = s.toState()
        screen = Screen.GAME
    }

    fun restart() {
        val g = game.value ?: return
        game.value = g.copy(
            cells = g.puzzle.copyOf(), notes = IntArray(81), seconds = 0, mistakes = 0, hints = 0,
            undo = emptyList(), redo = emptyList(), selected = -1, paused = false
        )
        persist()
    }

    fun persist() {
        val g = game.value ?: return
        viewModelScope.launch { dao.save(g.toSaved()) }
    }

    fun finishGame() {
        game.value = null
        viewModelScope.launch {
            dao.clearSaved()
            store.recordLoss()
        }
        screen = Screen.HOME
    }

    // ---------- игровые действия ----------
    private fun sfx(t: Sfx.T) = Sfx.play(settings.value, t)

    fun select(i: Int) {
        game.value = game.value?.copy(selected = i)
        sfx(Sfx.T.TAP)
    }

    private fun commit(
        g: GameState, cells: IntArray, notes: IntArray,
        mistakes: Int = g.mistakes, hints: Int = g.hints
    ) {
        val ng = g.copy(
            cells = cells, notes = notes, mistakes = mistakes, hints = hints,
            undo = (g.undo + Snap(g.cells, g.notes)).takeLast(200), redo = emptyList()
        )
        game.value = ng
        if (cells.contentEquals(ng.solution)) win(ng) else persist()
    }

    private fun placeDigit(g: GameState, i: Int, n: Int): Pair<IntArray, IntArray> {
        val cells = g.cells.copyOf(); cells[i] = n
        val notes = g.notes.copyOf(); notes[i] = 0
        for (j in Sudoku.PEERS[i]) notes[j] = notes[j] and (1 shl n).inv()
        return cells to notes
    }

    fun input(n: Int) {
        val g = game.value ?: return
        val i = g.selected
        if (i < 0 || g.paused || g.puzzle[i] != 0) return
        if (g.notesMode) {
            if (g.cells[i] != 0) return
            val notes = g.notes.copyOf(); notes[i] = notes[i] xor (1 shl n)
            commit(g, g.cells, notes); sfx(Sfx.T.TAP); return
        }
        if (g.cells[i] == n) return
        val wrong = g.solution[i] != n
        if (wrong && settings.value.autoCheck) {
            game.value = g.copy(mistakes = g.mistakes + 1)
            sfx(Sfx.T.ERROR); Sfx.vibrate(app, settings.value); persist(); return
        }
        val (cells, notes) = placeDigit(g, i, n)
        commit(g, cells, notes, mistakes = g.mistakes + if (wrong) 1 else 0)
        if (game.value != null) {
            if (wrong) sfx(Sfx.T.ERROR) else sfx(Sfx.T.PLACE)
            Sfx.vibrate(app, settings.value)
        }
    }

    fun erase() {
        val g = game.value ?: return
        val i = g.selected
        if (i < 0 || g.paused || g.puzzle[i] != 0 || (g.cells[i] == 0 && g.notes[i] == 0)) return
        val cells = g.cells.copyOf(); cells[i] = 0
        val notes = g.notes.copyOf(); notes[i] = 0
        commit(g, cells, notes); sfx(Sfx.T.TAP)
    }

    fun undo() {
        val g = game.value ?: return
        val s = g.undo.lastOrNull() ?: return
        game.value = g.copy(cells = s.cells, notes = s.notes, undo = g.undo.dropLast(1), redo = g.redo + Snap(g.cells, g.notes))
        persist(); sfx(Sfx.T.TAP)
    }

    fun redo() {
        val g = game.value ?: return
        val s = g.redo.lastOrNull() ?: return
        game.value = g.copy(cells = s.cells, notes = s.notes, redo = g.redo.dropLast(1), undo = g.undo + Snap(g.cells, g.notes))
        persist(); sfx(Sfx.T.TAP)
    }

    fun hint() {
        val g = game.value ?: return
        if (g.paused) return
        var t = g.selected
        if (t < 0 || g.puzzle[t] != 0 || g.cells[t] == g.solution[t]) {
            t = (0 until 81).firstOrNull { g.cells[it] != g.solution[it] } ?: return
        }
        val v = g.solution[t]
        val (cells, notes) = placeDigit(g, t, v)
        toast = "Подсказка: в клетку (${t / 9 + 1}, ${t % 9 + 1}) ставим $v"
        commit(g.copy(selected = t), cells, notes, hints = g.hints + 1)
        if (game.value != null) sfx(Sfx.T.PLACE)
    }

    fun check() {
        val g = game.value ?: return
        val bad = (0 until 81).count { g.cells[it] != 0 && g.cells[it] != g.solution[it] }
        toast = if (bad == 0) "Ошибок нет" else "Неверных клеток: $bad"
    }

    fun toggleNotes() { game.value = game.value?.let { it.copy(notesMode = !it.notesMode) } }

    fun togglePause() { game.value = game.value?.let { it.copy(paused = !it.paused) }; persist() }

    fun tick() {
        val g = game.value ?: return
        if (g.paused) return
        game.value = g.copy(seconds = g.seconds + 1)
        if ((g.seconds + 1) % 5 == 0) persist()
    }

    // ---------- победа ----------
    private fun win(g: GameState) {
        game.value = null
        val o = g.difficulty.ordinal + 1
        val score = maxOf(
            100,
            o * 1000 + maxOf(0, 1200 - g.seconds) + (if (g.mistakes == 0) 300 else 0) - g.mistakes * 100 - g.hints * 150
        )
        val stars = when {
            g.mistakes == 0 && g.hints == 0 -> 3
            g.mistakes <= 2 && g.hints <= 2 -> 2
            else -> 1
        }
        val r = GameResult(
            difficulty = g.difficulty.ordinal, level = g.level, mode = g.mode,
            finishedAt = System.currentTimeMillis(), seconds = g.seconds,
            mistakes = g.mistakes, hints = g.hints, score = score, stars = stars
        )
        lastResult = r
        screen = Screen.WIN
        sfx(Sfx.T.WIN)
        viewModelScope.launch {
            dao.clearSaved()
            dao.addResult(r)
            store.recordWin()
        }
    }

    // ---------- настройки и сброс ----------
    fun <T> setPref(key: Preferences.Key<T>, v: T) { viewModelScope.launch { store.set(key, v) } }

    fun previewSound() = sfx(Sfx.T.WIN)

    fun resetStats() = viewModelScope.launch { dao.clearResults(); store.resetStats() }

    fun deleteSaved() {
        game.value = null
        viewModelScope.launch { dao.clearSaved() }
    }

    fun resetAll() {
        game.value = null
        viewModelScope.launch { dao.clearSaved(); dao.clearResults(); store.resetAll() }
    }
}
