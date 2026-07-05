package logic

import model.*

// Запреты рэндзю для чёрных: overline (ряд длиннее пяти), двойная четвёрка,
// двойная тройка. Белые без ограничений. Пять в ряд имеет приоритет над
// запретом — победный ход всегда разрешён.
//
// ponytail: «наивная» реализация без рекурсивного исключения запрещённых
// точек при определении открытых троек (полное правило RIF учитывает, что
// продолжение тройки само может быть запрещённой точкой). Для казуальной
// партии этого достаточно; апгрейд — рекурсивная проверка в isOpenThree.

enum class RenjuViolation { OVERLINE, DOUBLE_FOUR, DOUBLE_THREE }

object RenjuRules {

    private val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    /** null — ход разрешён; иначе тип нарушения. Клетка (r,c) должна быть пустой. */
    fun violation(board: Board, r: Int, c: Int): RenjuViolation? {
        if (!board.placeStone(r, c, StoneColor.BLACK)) return null
        try {
            val runs = directions.map { (dr, dc) -> runLength(board, r, c, dr, dc) }
            if (runs.any { it == 5 }) return null // пять — победа, приоритетнее запретов
            if (runs.any { it >= 6 }) return RenjuViolation.OVERLINE

            var fours = 0
            var openThrees = 0
            for ((dr, dc) in directions) {
                val f = countFours(board, r, c, dr, dc)
                fours += f
                // Направление с четвёркой — не тройка: иначе легальная
                // комбинация «четыре-три» (四三) ложно читалась бы как 3×3.
                if (f == 0 && hasOpenThree(board, r, c, dr, dc)) openThrees++
            }
            if (fours >= 2) return RenjuViolation.DOUBLE_FOUR
            if (openThrees >= 2) return RenjuViolation.DOUBLE_THREE
            return null
        } finally {
            board.undoLastMove()
        }
    }

    private fun runLength(board: Board, r: Int, c: Int, dr: Int, dc: Int): Int {
        var len = 1
        var rr = r + dr; var cc = c + dc
        while (board.getStone(rr, cc) == StoneColor.BLACK) { len++; rr += dr; cc += dc }
        rr = r - dr; cc = c - dc
        while (board.getStone(rr, cc) == StoneColor.BLACK) { len++; rr -= dr; cc -= dc }
        return len
    }

    // Четвёрка вдоль направления: окно из 5 клеток с 4 чёрными и 1 пустой,
    // где пустая достраивает ровно пять (не overline). Прямая четвёрка
    // (_BBBB_) даёт два окна с одним и тем же набором камней — дедупим по
    // набору чёрных клеток, чтобы она считалась одной четвёркой.
    private fun countFours(board: Board, r: Int, c: Int, dr: Int, dc: Int): Int {
        val stoneSets = mutableSetOf<List<Int>>()
        for (offset in -4..0) {
            val blacks = mutableListOf<Int>()
            var empty: Pair<Int, Int>? = null
            var ok = true
            for (k in 0..4) {
                val rr = r + (offset + k) * dr
                val cc = c + (offset + k) * dc
                if (!board.isValidPosition(rr, cc)) { ok = false; break }
                when (board.getStone(rr, cc)) {
                    StoneColor.BLACK -> blacks += rr * 15 + cc
                    null -> if (empty == null) empty = rr to cc else { ok = false; break }
                    else -> { ok = false; break }
                }
            }
            if (!ok || blacks.size != 4) continue
            val e = empty ?: continue
            // достроенная пятёрка не должна быть overline
            if (completesExactlyFive(board, e.first, e.second, dr, dc)) {
                stoneSets += blacks.sorted()
            }
        }
        return stoneSets.size
    }

    private fun completesExactlyFive(board: Board, r: Int, c: Int, dr: Int, dc: Int): Boolean {
        if (!board.placeStone(r, c, StoneColor.BLACK)) return false
        val len = runLength(board, r, c, dr, dc)
        board.undoLastMove()
        return len == 5
    }

    // Открытая тройка: существует пустая клетка вдоль направления, ход в
    // которую создаёт прямую четвёрку (_BBBB_ — окно из 6 с пустыми краями).
    private fun hasOpenThree(board: Board, r: Int, c: Int, dr: Int, dc: Int): Boolean {
        for (step in -4..4) {
            if (step == 0) continue
            val er = r + step * dr
            val ec = c + step * dc
            if (!board.isEmpty(er, ec)) continue
            if (!board.placeStone(er, ec, StoneColor.BLACK)) continue
            val straight = hasStraightFour(board, r, c, dr, dc)
            board.undoLastMove()
            if (straight) return true
        }
        return false
    }

    // Прямая четвёрка через (r,c): 6 подряд клеток «пусто + BBBB + пусто».
    private fun hasStraightFour(board: Board, r: Int, c: Int, dr: Int, dc: Int): Boolean {
        for (offset in -4..-1) {
            var blacks = 0
            var ok = true
            // окно из 6: края (0 и 5) пустые, середина (1..4) чёрная
            for (k in 0..5) {
                val rr = r + (offset + k) * dr
                val cc = c + (offset + k) * dc
                if (!board.isValidPosition(rr, cc)) { ok = false; break }
                val s = board.getStone(rr, cc)
                if (k == 0 || k == 5) {
                    if (s != null) { ok = false; break }
                } else {
                    if (s != StoneColor.BLACK) { ok = false; break }
                    blacks++
                }
            }
            if (ok && blacks == 4) return true
        }
        return false
    }
}
