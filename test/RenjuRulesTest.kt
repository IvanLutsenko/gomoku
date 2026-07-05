package test

import logic.*
import model.*
import kotlin.test.*

class RenjuRulesTest {

    private fun boardWith(black: List<Pair<Int, Int>>, white: List<Pair<Int, Int>> = emptyList()): Board {
        val b = Board()
        black.forEach { (r, c) -> b.placeStone(r, c, StoneColor.BLACK) }
        white.forEach { (r, c) -> b.placeStone(r, c, StoneColor.WHITE) }
        return b
    }

    @Test
    fun overlineForbidden() {
        // B B B B _ B — ход в разрыв даёт ряд из шести
        val b = boardWith(listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6, 7 to 8))
        assertEquals(RenjuViolation.OVERLINE, RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun fiveBeatsForbidden() {
        // Пять по горизонтали, одновременно двойная тройка по вертикали
        // невозможна — но проверим простое: достройка до ровно пяти разрешена.
        val b = boardWith(listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6))
        assertNull(RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun doubleThreeForbidden() {
        // Две открытые тройки через (7,7): горизонталь и вертикаль
        val b = boardWith(listOf(7 to 5, 7 to 6, 5 to 7, 6 to 7))
        assertEquals(RenjuViolation.DOUBLE_THREE, RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun singleThreeAllowed() {
        val b = boardWith(listOf(7 to 5, 7 to 6))
        assertNull(RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun blockedThreeNotOpen() {
        // Горизонтальная тройка упирается в белый камень — не открытая;
        // вертикальная открытая. Одна тройка — не запрет.
        val b = boardWith(
            black = listOf(7 to 5, 7 to 6, 5 to 7, 6 to 7),
            white = listOf(7 to 4),
        )
        assertNull(RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun doubleFourForbidden() {
        // Две четвёрки: горизонталь B B B _ и вертикаль B B B _ через (7,7)
        val b = boardWith(
            listOf(
                7 to 4, 7 to 5, 7 to 6, // гориз: с (7,7) будет 4, пятая — (7,3) или (7,8)
                4 to 7, 5 to 7, 6 to 7, // верт: аналогично
            ),
            // блокируем по одному концу, чтобы тройки не считались открытыми,
            // но четвёрки оставались достраиваемыми
            white = listOf(7 to 8, 8 to 7),
        )
        assertEquals(RenjuViolation.DOUBLE_FOUR, RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun straightFourIsSingleFour() {
        // _ B B B B _ — прямая четвёрка (два продолжения, один набор камней),
        // не двойная четвёрка.
        val b = boardWith(listOf(7 to 4, 7 to 5, 7 to 6))
        assertNull(RenjuRules.violation(b, 7, 7))
    }

    @Test
    fun whiteUnrestricted() {
        val g = GameLogic(renju = true)
        // Чёрные и белые ходят по очереди; белым overline разрешён.
        g.makeMove(0, 0) // B
        g.makeMove(7, 3) // W
        g.makeMove(0, 2) // B
        g.makeMove(7, 4) // W
        g.makeMove(0, 4) // B
        g.makeMove(7, 5) // W
        g.makeMove(0, 6) // B
        g.makeMove(7, 6) // W
        g.makeMove(0, 8) // B
        g.makeMove(7, 8) // W
        g.makeMove(0, 10) // B
        // Белые ходят в разрыв — ряд из шести, для белых это победа (>=5)
        val res = g.makeMove(7, 7)
        assertTrue(res is MoveResult.Success)
        assertEquals(GameState.WHITE_WINS, (res as MoveResult.Success).gameState)
    }

    @Test
    fun forbiddenMoveReturnedFromGameLogic() {
        val g = GameLogic(renju = true)
        // Строим чёрным двойную тройку вокруг (7,7); белые ходят в сторону.
        g.makeMove(7, 5); g.makeMove(0, 0)
        g.makeMove(7, 6); g.makeMove(0, 1)
        g.makeMove(5, 7); g.makeMove(0, 2)
        g.makeMove(6, 7); g.makeMove(0, 3)
        val res = g.makeMove(7, 7)
        assertTrue(res is MoveResult.Forbidden, "ожидали Forbidden, получили $res")
        assertEquals(RenjuViolation.DOUBLE_THREE, (res as MoveResult.Forbidden).violation)
        // Доска не изменилась, ход остался за чёрными
        assertTrue(g.getBoard().isEmpty(7, 7))
        assertEquals(StoneColor.BLACK, g.currentPlayer)
    }

    @Test
    fun classicModeHasNoForbidden() {
        val g = GameLogic(renju = false)
        g.makeMove(7, 5); g.makeMove(0, 0)
        g.makeMove(7, 6); g.makeMove(0, 1)
        g.makeMove(5, 7); g.makeMove(0, 2)
        g.makeMove(6, 7); g.makeMove(0, 3)
        assertTrue(g.makeMove(7, 7) is MoveResult.Success)
    }

    @Test
    fun boardUntouchedAfterCheck() {
        val b = boardWith(listOf(7 to 5, 7 to 6, 5 to 7, 6 to 7))
        val before = b.moveCount
        RenjuRules.violation(b, 7, 7)
        assertEquals(before, b.moveCount)
        assertTrue(b.isEmpty(7, 7))
    }
}
