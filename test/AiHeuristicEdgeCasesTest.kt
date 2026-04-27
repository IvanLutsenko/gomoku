import logic.GameLogic
import logic.ai.candidateCells
import logic.ai.scoreMove
import logic.ai.topMoves
import model.Position
import model.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AiHeuristicEdgeCasesTest {

    @Test fun candidateCells_radius2_around_one_stone_yields_24() {
        val game = GameLogic()
        game.makeMove(7, 7)
        val cands = candidateCells(game.getBoard(), radius = 2)
        // Прямоугольник 5×5 минус центр (занят) = 24
        assertEquals(24, cands.size)
        assertTrue(cands.all { it.row in 5..9 && it.col in 5..9 })
        assertTrue(cands.none { it == Position(7, 7) })
    }

    @Test fun candidateCells_radius0_on_empty_board_returns_center() {
        val board = GameLogic().getBoard()
        val cands = candidateCells(board, radius = 0)
        assertEquals(listOf(Position(7, 7)), cands)
    }

    @Test fun candidateCells_corner_stone_clamped_to_board() {
        val game = GameLogic()
        game.makeMove(0, 0)
        val cands = candidateCells(game.getBoard(), radius = 1)
        // Радиус 1 от (0,0): 4 валидные клетки (0,1), (1,0), (1,1) (трёхкратно: 2+2−1−1 occupied)
        // (0,0) занят. Возможные: (0,1), (1,0), (1,1) → 3 клетки.
        assertEquals(3, cands.size)
        assertTrue(Position(0, 1) in cands)
        assertTrue(Position(1, 0) in cands)
        assertTrue(Position(1, 1) in cands)
    }

    @Test fun candidateCells_excludes_occupied_cells() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(7, 8)
        val cands = candidateCells(game.getBoard(), radius = 1)
        assertTrue(Position(7, 7) !in cands)
        assertTrue(Position(7, 8) !in cands)
    }

    @Test fun scoreMove_center_bonus_decreases_outward() {
        val board = GameLogic().getBoard()
        val center = scoreMove(board, 7, 7, StoneColor.BLACK, StoneColor.WHITE)
        val nearCenter = scoreMove(board, 6, 6, StoneColor.BLACK, StoneColor.WHITE)
        val corner = scoreMove(board, 0, 0, StoneColor.BLACK, StoneColor.WHITE)
        // На пустой доске разница только в center bonus.
        assertTrue(center > nearCenter, "center $center should beat nearCenter $nearCenter")
        assertTrue(nearCenter > corner, "nearCenter $nearCenter should beat corner $corner")
    }

    @Test fun scoreMove_my_three_in_row_higher_than_random_spot() {
        val game = GameLogic()
        // BLACK: (7,3),(7,4),(7,5) — открытая тройка. Защитимся в стороне.
        game.makeMove(7, 3); game.makeMove(0, 0)
        game.makeMove(7, 4); game.makeMove(14, 14)
        game.makeMove(7, 5); game.makeMove(0, 14)
        // Сейчас ход BLACK. Score (7,6) (продолжение тройки) >> (10,10).
        val board = game.getBoard()
        val extend = scoreMove(board, 7, 6, StoneColor.BLACK, StoneColor.WHITE)
        val random = scoreMove(board, 10, 10, StoneColor.BLACK, StoneColor.WHITE)
        assertTrue(extend > random)
    }

    @Test fun topMoves_count_zero_returns_empty() {
        val game = GameLogic()
        game.makeMove(7, 7)
        val hints = topMoves(game, count = 0)
        assertEquals(emptyList(), hints)
    }

    @Test fun topMoves_count_larger_than_candidates_capped() {
        val game = GameLogic()
        game.makeMove(7, 7)
        // Запросить больше чем есть кандидатов в радиусе 1 (8 штук).
        val hints = topMoves(game, count = 100)
        assertTrue(hints.size <= 8, "got ${hints.size}, expected <= 8")
        assertTrue(hints.isNotEmpty())
    }

    @Test fun topMoves_does_not_include_occupied_cells() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(7, 8)
        val hints = topMoves(game, count = 5)
        assertTrue(hints.none { it.pos == Position(7, 7) })
        assertTrue(hints.none { it.pos == Position(7, 8) })
    }

    @Test fun scoreMove_attack_and_defense_use_correct_colors() {
        val game = GameLogic()
        // BLACK: 4 в ряд (7,3..6).
        val blacks = listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6)
        val whites = listOf(0 to 0, 14 to 14, 0 to 14, 14 to 0)
        for (i in 0 until 4) {
            game.makeMove(blacks[i].first, blacks[i].second)
            game.makeMove(whites[i].first, whites[i].second)
        }
        val board = game.getBoard()
        // С точки зрения BLACK: (7,7) — winning attack.
        val asBlack = scoreMove(board, 7, 7, StoneColor.BLACK, StoneColor.WHITE)
        // С точки зрения WHITE: (7,7) — блок BLACK-четвёрки → defense часть.
        val asWhite = scoreMove(board, 7, 7, StoneColor.WHITE, StoneColor.BLACK)
        // Оба score должны быть очень высокими (миллионы) — кто бы ни играл.
        assertTrue(asBlack > 1_000_000, "asBlack $asBlack")
        assertTrue(asWhite > 1_000_000, "asWhite $asWhite")
        // Но не одинаковы — opponentBias × center и т.д.
        assertNotEquals(asBlack, asWhite)
    }
}
