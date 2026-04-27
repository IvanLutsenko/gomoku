import logic.GameLogic
import logic.ai.AiFactory
import logic.ai.Difficulty
import logic.ai.HeuristicAi
import logic.ai.candidateCells
import logic.ai.scoreMove
import logic.ai.topMoves
import model.Position
import model.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiPlayerTest {

    @Test fun first_move_goes_to_center() {
        val game = GameLogic()
        val ai = AiFactory.create(Difficulty.MID)
        assertEquals(Position(7, 7), ai.chooseMove(game))
    }

    @Test fun random_ai_returns_neighbor_of_existing_stones() {
        val game = GameLogic()
        game.makeMove(7, 7)        // BLACK at center
        val ai = AiFactory.create(Difficulty.EASY)
        val choice = ai.chooseMove(game)
        // Должен лежать в радиусе 2 от занятой клетки и быть пустым
        assertTrue(choice.row in 5..9 && choice.col in 5..9, "choice was $choice")
        assertTrue(game.getBoard().isEmpty(choice.row, choice.col))
    }

    @Test fun heuristic_blocks_open_four_threat() {
        val game = GameLogic()
        // WHITE собирает открытую четвёрку (7,3..6). BLACK играет в углах
        // — рассеянными камнями, чтобы не накопить собственных linий.
        val whites = listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6)
        val blackFiller = listOf(0 to 0, 14 to 14, 0 to 14, 14 to 0)
        for (i in 0 until 4) {
            game.makeMove(blackFiller[i].first, blackFiller[i].second)
            game.makeMove(whites[i].first, whites[i].second)
        }
        // Сейчас ход BLACK. У WHITE открытая четвёрка по (7,3..6).
        // У BLACK нет своих threats — единственный разумный ход это блок.
        val ai = AiFactory.create(Difficulty.MID)
        val choice = ai.chooseMove(game)
        assertTrue(
            choice == Position(7, 2) || choice == Position(7, 7),
            "expected block at (7,2) or (7,7), got $choice",
        )
    }

    @Test fun heuristic_completes_own_four_to_win() {
        val game = GameLogic()
        // BLACK имеет open four (7,3..6). WHITE filler — одиночные ходы
        // в углах, без собственных linий и угроз.
        val blacks = listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6)
        val whiteFiller = listOf(0 to 0, 14 to 14, 0 to 14, 14 to 0)
        for (i in 0 until 4) {
            game.makeMove(blacks[i].first, blacks[i].second)
            game.makeMove(whiteFiller[i].first, whiteFiller[i].second)
        }
        // Ход BLACK. AI обязан добить на (7,2) или (7,7).
        val ai = AiFactory.create(Difficulty.MID)
        val choice = ai.chooseMove(game)
        assertTrue(
            choice == Position(7, 2) || choice == Position(7, 7),
            "expected winning move (7,2)/(7,7), got $choice",
        )
        val r = game.makeMove(choice.row, choice.col)
        assertTrue(r is logic.MoveResult.Success && r.winner == StoneColor.BLACK)
    }

    @Test fun candidateCells_starts_at_center_when_board_empty() {
        val board = GameLogic().getBoard()
        val cands = candidateCells(board, radius = 2)
        assertEquals(listOf(Position(7, 7)), cands)
    }

    @Test fun candidateCells_returns_empty_neighbors_within_radius() {
        val game = GameLogic()
        game.makeMove(7, 7)  // BLACK
        val cands = candidateCells(game.getBoard(), radius = 1)
        // Соседи (7,7) в радиусе 1 — 8 клеток (3×3 минус центр-занят).
        assertEquals(8, cands.size)
        assertTrue(cands.all { it.row in 6..8 && it.col in 6..8 })
        assertTrue(cands.none { it == Position(7, 7) })
    }

    @Test fun scoreMove_winning_move_dominates_others() {
        val game = GameLogic()
        // Положение: BLACK имеет (7,3..6); ход BLACK. (7,7) — win.
        val blacks = listOf(7 to 3, 7 to 4, 7 to 5, 7 to 6)
        val whites = listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)
        for (i in 0 until 4) {
            game.makeMove(blacks[i].first, blacks[i].second)
            game.makeMove(whites[i].first, whites[i].second)
        }
        val board = game.getBoard()
        val winningScore = scoreMove(board, 7, 7, StoneColor.BLACK, StoneColor.WHITE)
        val randomScore = scoreMove(board, 14, 14, StoneColor.BLACK, StoneColor.WHITE)
        assertTrue(
            winningScore > randomScore * 100,
            "winning move score $winningScore should dominate $randomScore",
        )
    }

    @Test fun topMoves_returns_count_distinct_sorted() {
        val game = GameLogic()
        game.makeMove(7, 7)
        val hints = topMoves(game, count = 3)
        assertEquals(3, hints.size)
        // отсортированы по score убыв.
        assertTrue(hints[0].score >= hints[1].score)
        assertTrue(hints[1].score >= hints[2].score)
        // позиции уникальные
        assertEquals(3, hints.map { it.pos }.toSet().size)
    }

    @Test fun heuristicAi_returns_legal_move() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(8, 8)
        val ai = HeuristicAi()
        val pos = ai.chooseMove(game)
        assertTrue(game.getBoard().isEmpty(pos.row, pos.col))
    }
}
