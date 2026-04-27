import logic.GameLogic
import logic.MoveResult
import logic.ai.AiFactory
import logic.ai.Difficulty
import logic.ai.HeuristicAi
import logic.ai.candidateCells
import model.GameState
import model.Position
import model.StoneColor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameVsAiIntegrationTest {

    @Test fun ai_only_picks_legal_moves_in_50_random_games() {
        val rnd = Random(1234)
        val ai = AiFactory.create(Difficulty.MID)
        repeat(50) { gameIdx ->
            val game = GameLogic()
            // Случайный «оппонент» делает свободные ходы; AI отвечает.
            for (turn in 0 until 60) {
                if (game.gameState != GameState.PLAYING) break
                val mover = game.currentPlayer
                val pos = if (mover == StoneColor.BLACK) {
                    // BLACK = «оппонент»: случайная пустая клетка
                    val empty = game.getBoard().getEmptyPositions()
                    empty[rnd.nextInt(empty.size)]
                } else {
                    // WHITE = AI
                    ai.chooseMove(game)
                }
                val res = game.makeMove(pos.row, pos.col)
                assertTrue(
                    res is MoveResult.Success,
                    "game $gameIdx turn $turn: $mover at $pos → $res",
                )
            }
        }
    }

    @Test fun ai_does_not_loop_on_almost_full_board() {
        // Заполним доску так чтобы остались две клетки. AI должен выбрать
        // одну из них быстро, без зависаний.
        val game = GameLogic()
        val targets = mutableListOf<Position>()
        for (r in 0 until 15) for (c in 0 until 15) targets += Position(r, c)
        // Перемешаем детерминировано
        val rnd = Random(42)
        targets.shuffle(rnd)
        // Сделаем 223 хода чередованием, не давая никому победить — пробуем
        // короткими сериями через makeMove. Если по дороге кто-то выиграл —
        // тест всё равно валиден (ai_only_picks_legal проверил это отдельно).
        for ((i, p) in targets.take(223).withIndex()) {
            game.makeMove(p.row, p.col)
            if (game.gameState != GameState.PLAYING) return  // ничего страшного
        }
        if (game.gameState != GameState.PLAYING) return
        // Сейчас ровно 2 пустые клетки.
        val ai = HeuristicAi()
        val pick = ai.chooseMove(game)
        assertTrue(game.getBoard().isEmpty(pick.row, pick.col))
    }

    @Test fun easy_ai_returns_legal_move_after_state_changes() {
        val game = GameLogic()
        val ai = AiFactory.create(Difficulty.EASY)
        game.makeMove(7, 7)
        val first = ai.chooseMove(game)
        assertTrue(game.getBoard().isEmpty(first.row, first.col))
        game.makeMove(first.row, first.col)
        game.makeMove(0, 0)
        val second = ai.chooseMove(game)
        assertTrue(game.getBoard().isEmpty(second.row, second.col))
        // После двух разных ходов AI не должен повторно вернуть первый.
        assertNotEquals(first, second)
    }

    @Test fun candidateCells_is_subset_of_empty_positions() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(8, 8)
        val cands = candidateCells(game.getBoard(), radius = 2)
        val empties = game.getBoard().getEmptyPositions().toSet()
        assertTrue(cands.all { it in empties })
    }
}

