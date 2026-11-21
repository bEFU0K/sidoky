package com.pixelsudoku

import com.pixelsudoku.engine.Difficulty
import com.pixelsudoku.engine.Sudoku
import org.junit.Assert.*
import org.junit.Test

class SudokuTest {
    @Test
    fun puzzlesHaveUniqueCorrectSolution() {
        for (d in Difficulty.values()) {
            for (seed in 1L..3L) {
                val p = Sudoku.generate(seed * 101 + d.ordinal, d.clues)
                assertTrue("solution valid", Sudoku.isValidSolution(p.solution))
                assertEquals("unique", 1, Sudoku.countSolutions(p.puzzle))
                for (i in 0 until 81) if (p.puzzle[i] != 0) assertEquals(p.solution[i], p.puzzle[i])
                assertTrue(p.puzzle.count { it != 0 } >= 17)
            }
        }
    }

    @Test
    fun invalidGridDetected() {
        val p = Sudoku.generate(5, 40)
        val bad = p.solution.copyOf(); bad[0] = bad[1]
        assertFalse(Sudoku.isValidSolution(bad))
    }
}
