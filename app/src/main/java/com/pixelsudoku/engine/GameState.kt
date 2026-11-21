package com.pixelsudoku.engine

class Snap(val cells: IntArray, val notes: IntArray)

data class GameState(
    val difficulty: Difficulty,
    val level: Int,
    val mode: String, // level | daily | random
    val puzzle: IntArray,
    val solution: IntArray,
    val cells: IntArray,
    val notes: IntArray,
    val seconds: Int,
    val mistakes: Int,
    val hints: Int,
    val selected: Int = -1,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    val undo: List<Snap> = emptyList(),
    val redo: List<Snap> = emptyList()
) {
    val empty: Int get() = cells.count { it == 0 }
}
