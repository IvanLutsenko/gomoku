import logic.GameLogic
import logic.MoveResult
import logic.UndoResult
import model.GameState
import model.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameLogicEdgeCasesTest {

    @Test fun success_fields_populated_correctly() {
        val game = GameLogic()
        val res = game.makeMove(7, 7)
        assertTrue(res is MoveResult.Success)
        assertEquals(7, res.move.row)
        assertEquals(7, res.move.col)
        assertEquals(StoneColor.BLACK, res.move.player)
        assertEquals(1, res.move.moveNumber)
        assertEquals(GameState.PLAYING, res.gameState)
        assertNull(res.winner)
        assertNull(res.winningLine)
        assertEquals(StoneColor.WHITE, res.nextPlayer)
    }

    @Test fun success_fields_at_winning_move() {
        val game = GameLogic()
        for (i in 0 until 4) { game.makeMove(7, i); game.makeMove(8, i) }
        val win = game.makeMove(7, 4)
        assertTrue(win is MoveResult.Success)
        assertEquals(GameState.BLACK_WINS, win.gameState)
        assertEquals(StoneColor.BLACK, win.winner)
        assertNotNull(win.winningLine)
        // После победы nextPlayer не должен переключаться
        assertEquals(StoneColor.BLACK, win.nextPlayer)
    }

    @Test fun multi_undo_returns_to_initial_state() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(7, 8)
        game.makeMove(8, 7)
        repeat(3) {
            val r = game.undoMove()
            assertTrue(r is UndoResult.Success)
        }
        assertEquals(0, game.getMoveCount())
        assertEquals(StoneColor.BLACK, game.currentPlayer)
        assertEquals(GameState.PLAYING, game.gameState)
        // Ещё один undo — уже nothing
        assertEquals(UndoResult.NoMovesToUndo, game.undoMove())
    }

    @Test fun undo_then_replay_same_position() {
        val game = GameLogic()
        game.makeMove(7, 7)
        game.makeMove(7, 8)
        game.undoMove()
        // Очередь WHITE → BLACK после undo. После undo следующего хода
        // значит играет тот, чей был отменённый ход. Откатили WHITE-ход
        // (7,8) → теперь снова ход WHITE.
        assertEquals(StoneColor.WHITE, game.currentPlayer)
        // Можем ещё раз сыграть (7,8) — то же самое.
        val res = game.makeMove(7, 8)
        assertTrue(res is MoveResult.Success)
        assertEquals(StoneColor.WHITE, res.move.player)
    }

    @Test fun undo_after_full_game_clears_winner() {
        val game = GameLogic()
        for (i in 0 until 4) { game.makeMove(7, i); game.makeMove(8, i) }
        game.makeMove(7, 4) // win
        assertEquals(GameState.BLACK_WINS, game.gameState)
        game.undoMove()
        // Теперь снова PLAYING, ход BLACK (тот кто сделал победу)
        assertEquals(GameState.PLAYING, game.gameState)
        assertEquals(StoneColor.BLACK, game.currentPlayer)
        assertNull(game.winner)
        assertNull(game.winningLine)
    }

    @Test fun moveHistory_grows_and_shrinks() {
        val game = GameLogic()
        assertEquals(0, game.getMoveHistory().size)
        game.makeMove(7, 7)
        assertEquals(1, game.getMoveHistory().size)
        game.makeMove(7, 8)
        assertEquals(2, game.getMoveHistory().size)
        game.undoMove()
        assertEquals(1, game.getMoveHistory().size)
        game.resetGame()
        assertEquals(0, game.getMoveHistory().size)
    }

    @Test fun reset_after_win_re_enables_moves() {
        val game = GameLogic()
        for (i in 0 until 4) { game.makeMove(7, i); game.makeMove(8, i) }
        game.makeMove(7, 4) // win
        game.resetGame()
        // После reset снова можно ходить
        val r = game.makeMove(7, 7)
        assertTrue(r is MoveResult.Success)
    }

    @Test fun current_player_consistent_with_move_count_parity() {
        val game = GameLogic()
        for (i in 0 until 10) {
            // BLACK ходит на чётных, WHITE на нечётных номерах хода
            val expected = if (i % 2 == 0) StoneColor.BLACK else StoneColor.WHITE
            assertEquals(expected, game.currentPlayer, "before move $i")
            game.makeMove(i / 15, i % 15)
        }
    }
}
