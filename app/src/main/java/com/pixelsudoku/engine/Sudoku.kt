package com.pixelsudoku.engine

import kotlin.random.Random

class Puzzle(val puzzle: IntArray, val solution: IntArray)

enum class Difficulty(val title: String, val clues: Int) {
    EASY("Лёгкий", 40),
    MEDIUM("Средний", 34),
    HARD("Сложный", 30),
    EXPERT("Эксперт", 26),
    MASTER("Мастер", 23)
}

const val LEVELS_PER_DIFFICULTY = 100

object Sudoku {
    val PEERS: Array<IntArray> = Array(81) { i ->
        val r = i / 9
        val c = i % 9
        val s = LinkedHashSet<Int>()
        for (k in 0 until 9) { s.add(r * 9 + k); s.add(k * 9 + c) }
        for (dr in 0..2) for (dc in 0..2) s.add((r / 3 * 3 + dr) * 9 + c / 3 * 3 + dc)
        s.remove(i)
        s.toIntArray()
    }

    private fun bit(v: Int) = if (v == 0) 0 else 1 shl v

    private fun candidates(g: IntArray, i: Int): Int {
        var used = 0
        for (p in PEERS[i]) used = used or bit(g[p])
        return used.inv() and 0x3FE
    }

    /** Считает решения (не больше limit). */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int = solve(grid.copyOf(), limit, null)

    private fun solve(g: IntArray, limit: Int, sol: IntArray?): Int {
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (i in 0 until 81) {
            if (g[i] == 0) {
                val m = candidates(g, i)
                val n = Integer.bitCount(m)
                if (n == 0) return 0
                if (n < bestCount) { best = i; bestMask = m; bestCount = n; if (n == 1) break }
            }
        }
        if (best == -1) { sol?.let { g.copyInto(it) }; return 1 }
        var total = 0
        for (v in 1..9) {
            if ((bestMask and (1 shl v)) != 0) {
                g[best] = v
                total += solve(g, limit - total, sol)
                g[best] = 0
                if (total >= limit) break
            }
        }
        return total
    }

    private fun fill(g: IntArray, rnd: Random): Boolean {
        val i = g.indexOf(0)
        if (i < 0) return true
        val m = candidates(g, i)
        val vals = (1..9).filter { (m and (1 shl it)) != 0 }.shuffled(rnd)
        for (v in vals) {
            g[i] = v
            if (fill(g, rnd)) return true
        }
        g[i] = 0
        return false
    }

    fun isValidSolution(g: IntArray): Boolean {
        if (g.size != 81) return false
        for (k in 0 until 9) {
            var row = 0; var col = 0; var box = 0
            for (j in 0 until 9) {
                row = row or bit(g[k * 9 + j])
                col = col or bit(g[j * 9 + k])
                box = box or bit(g[(k / 3 * 3 + j / 3) * 9 + k % 3 * 3 + j % 3])
            }
            if (row != 0x3FE || col != 0x3FE || box != 0x3FE) return false
        }
        return true
    }

    /** Детерминированная генерация по seed; единственность решения проверяется на каждом удалении. */
    fun generate(seed: Long, clues: Int, budgetMs: Long = 2500): Puzzle {
        val rnd = Random(seed)
        val sol = IntArray(81)
        fill(sol, rnd)
        val p = sol.copyOf()
        val start = System.currentTimeMillis()
        var count = 81
        for (i in (0 until 81).shuffled(rnd)) {
            if (count <= clues) break
            if (System.currentTimeMillis() - start > budgetMs) break
            val keep = p[i]
            p[i] = 0
            if (countSolutions(p) != 1) p[i] = keep else count--
        }
        return Puzzle(p, sol)
    }
}
