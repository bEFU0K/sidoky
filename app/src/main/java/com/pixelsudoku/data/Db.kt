package com.pixelsudoku.data

import android.content.Context
import androidx.room.*
import com.pixelsudoku.engine.Difficulty
import com.pixelsudoku.engine.GameState
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_game")
data class SavedGame(
    @PrimaryKey val id: Int = 0,
    val difficulty: Int,
    val level: Int,
    val mode: String,
    val puzzle: String,
    val solution: String,
    val cells: String,
    val notes: String,
    val seconds: Int,
    val mistakes: Int,
    val hints: Int
)

@Entity(tableName = "results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val difficulty: Int,
    val level: Int,
    val mode: String,
    val finishedAt: Long,
    val seconds: Int,
    val mistakes: Int,
    val hints: Int,
    val score: Int,
    val stars: Int
)

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_game WHERE id = 0") fun observeSaved(): Flow<SavedGame?>
    @Upsert suspend fun save(g: SavedGame)
    @Query("DELETE FROM saved_game") suspend fun clearSaved()
    @Insert suspend fun addResult(r: GameResult)
    @Query("SELECT * FROM results ORDER BY finishedAt DESC") fun observeResults(): Flow<List<GameResult>>
    @Query("DELETE FROM results") suspend fun clearResults()
}

@Database(entities = [SavedGame::class, GameResult::class], version = 1, exportSchema = false)
abstract class GameDb : RoomDatabase() {
    abstract fun dao(): GameDao

    companion object {
        @Volatile private var inst: GameDb? = null
        fun get(c: Context): GameDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, GameDb::class.java, "pixel_sudoku.db")
                .build().also { inst = it }
        }
    }
}

fun GameState.toSaved() = SavedGame(
    0, difficulty.ordinal, level, mode,
    puzzle.joinToString(""), solution.joinToString(""), cells.joinToString(""),
    notes.joinToString(","), seconds, mistakes, hints
)

fun SavedGame.toState(): GameState {
    fun d(s: String) = IntArray(81) { s[it] - '0' }
    return GameState(
        Difficulty.values()[difficulty], level, mode, d(puzzle), d(solution), d(cells),
        notes.split(",").map { it.toInt() }.toIntArray(), seconds, mistakes, hints
    )
}
