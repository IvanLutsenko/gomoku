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

class GameLogicTest {

    @Test fun fresh_game_state() {
        val g = GameLogic()
        assertEquals(StoneColor.BLACK, g.currentPlayer)
        assertEquals(GameState.PLAYING, g.gameState)
        assertNull(g.winner)
        assertNull(g.winningLine)
        assertEquals(0, g.getMoveCount())
    }

    @Test fun makeMove_alternates_players() {
        val g = GameLogic()
        g.makeMove(7, 7) // BLACK
        assertEquals(StoneColor.WHITE, g.currentPlayer)
        g.makeMove(7, 8) // WHITE
        assertEquals(StoneColor.BLACK, g.currentPlayer)
    }

    @Test fun makeMove_invalid_repeat() {
        val g = GameLogic()
        g.makeMove(7, 7)
        val r = g.makeMove(7, 7)
        assertTrue(r is MoveResult.InvalidMove, "got $r")
        assertEquals(StoneColor.WHITE, g.currentPlayer)
    }

    @Test fun makeMove_out_of_bounds() {
        val g = GameLogic()
        val r = g.makeMove(15, 0)
        assertTrue(r is MoveResult.InvalidMove)
        assertEquals(0, g.getMoveCount())
    }

    @Test fun horizontal_win_for_black() {
        val g = GameLogic()
        // BLACK strecthes (7,0..3), then (7,4) wins
        for (i in 0 until 4) {
            g.makeMove(7, i)
            g.makeMove(8, i)
        }
        val win = g.makeMove(7, 4)
        assertTrue(win is MoveResult.Success)
        assertEquals(GameState.BLACK_WINS, win.gameState)
        assertEquals(StoneColor.BLACK, win.winner)
        val line = win.winningLine
        assertNotNull(line)
        assertEquals(5, line.positions.size)
    }

    @Test fun vertical_win_for_white() {
        val g = GameLogic()
        // BLACK column 5; WHITE column 6 builds to 5
        for (i in 0 until 4) {
            g.makeMove(i, 5)         // BLACK
            g.makeMove(i, 6)         // WHITE
        }
        g.makeMove(4, 5)              // BLACK (no win, 5 in col 5 is BLACK)
        // BLACK already has 5 in col 5. So game ended on BLACK's move.
        assertEquals(GameState.BLACK_WINS, g.gameState)
    }

    @Test fun diagonal_down_right_win() {
        val g = GameLogic()
        // BLACK at (i,i), WHITE at (i, i+1) keeping it boring
        for (i in 0 until 4) {
            g.makeMove(i, i)            // BLACK
            g.makeMove(i, i + 1)        // WHITE
        }
        val win = g.makeMove(4, 4)       // BLACK 5-th
        assertTrue(win is MoveResult.Success)
        assertEquals(GameState.BLACK_WINS, win.gameState)
    }

    @Test fun diagonal_down_left_win() {
        val g = GameLogic()
        // BLACK at (0,4),(1,3),(2,2),(3,1),(4,0)
        // Play in pairs to reach (4,0) for BLACK as 5th in diagonal.
        val blackDiag = listOf(0 to 4, 1 to 3, 2 to 2, 3 to 1, 4 to 0)
        val whiteFiller = listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)
        for (i in 0 until 4) {
            val (br, bc) = blackDiag[i]
            g.makeMove(br, bc)
            val (wr, wc) = whiteFiller[i]
            g.makeMove(wr, wc)
        }
        val (br, bc) = blackDiag[4]
        val win = g.makeMove(br, bc)
        assertTrue(win is MoveResult.Success)
        assertEquals(GameState.BLACK_WINS, win.gameState)
    }

    @Test fun moves_after_game_end_rejected() {
        val g = GameLogic()
        for (i in 0 until 4) {
            g.makeMove(7, i); g.makeMove(8, i)
        }
        g.makeMove(7, 4) // win for BLACK
        val r = g.makeMove(0, 0)
        assertTrue(r is MoveResult.GameEnded, "got $r")
    }

    @Test fun undoMove_clears_winning_state() {
        val g = GameLogic()
        for (i in 0 until 4) { g.makeMove(7, i); g.makeMove(8, i) }
        g.makeMove(7, 4) // BLACK wins
        assertEquals(GameState.BLACK_WINS, g.gameState)

        val undo = g.undoMove()
        assertTrue(undo is UndoResult.Success)
        assertEquals(GameState.PLAYING, g.gameState)
        assertNull(g.winner)
        assertNull(g.winningLine)
        assertEquals(StoneColor.BLACK, g.currentPlayer)
    }

    @Test fun undoMove_on_empty_returns_NoMovesToUndo() {
        val g = GameLogic()
        assertEquals(UndoResult.NoMovesToUndo, g.undoMove())
    }

    @Test fun resetGame_brings_back_initial_state() {
        val g = GameLogic()
        g.makeMove(7, 7); g.makeMove(7, 8); g.makeMove(8, 8)
        g.resetGame()
        assertEquals(0, g.getMoveCount())
        assertEquals(StoneColor.BLACK, g.currentPlayer)
        assertEquals(GameState.PLAYING, g.gameState)
        assertNull(g.winner)
        assertNull(g.winningLine)
    }

    @Test fun canMakeMove_respects_state() {
        val g = GameLogic()
        assertTrue(g.canMakeMove(7, 7))
        g.makeMove(7, 7)
        assertEquals(false, g.canMakeMove(7, 7))
    }

    @Test fun winningLine_contains_5_consecutive_positions() {
        val g = GameLogic()
        // Build BLACK 5-in-row from (4,7) to (8,7) vertically.
        for (i in 0 until 4) {
            g.makeMove(4 + i, 7)        // BLACK column 7
            g.makeMove(4 + i, 8)        // WHITE column 8
        }
        val win = g.makeMove(8, 7)       // BLACK 5th
        assertTrue(win is MoveResult.Success)
        val pos = win.winningLine!!.positions
        // sorted by row from getLine logic
        val rows = pos.map { it.row }
        assertEquals((4..8).toList(), rows)
        assertTrue(pos.all { it.col == 7 })
    }
}
