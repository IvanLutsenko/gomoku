import model.Board
import model.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTest {

    @Test fun newBoard_isEmpty_everywhere() {
        val b = Board()
        assertEquals(0, b.moveCount)
        for (r in 0 until 15) for (c in 0 until 15) {
            assertTrue(b.isEmpty(r, c))
            assertNull(b.getStone(r, c))
        }
        assertFalse(b.isFull())
        assertEquals(225, b.getEmptyPositions().size)
    }

    @Test fun isValidPosition_bounds() {
        val b = Board()
        assertTrue(b.isValidPosition(0, 0))
        assertTrue(b.isValidPosition(14, 14))
        assertFalse(b.isValidPosition(-1, 0))
        assertFalse(b.isValidPosition(0, -1))
        assertFalse(b.isValidPosition(15, 0))
        assertFalse(b.isValidPosition(0, 15))
    }

    @Test fun placeStone_success_and_history() {
        val b = Board()
        assertTrue(b.placeStone(7, 7, StoneColor.BLACK))
        assertEquals(StoneColor.BLACK, b.getStone(7, 7))
        assertFalse(b.isEmpty(7, 7))
        assertEquals(1, b.moveCount)

        val move = b.moveHistory.last()
        assertEquals(7, move.row)
        assertEquals(7, move.col)
        assertEquals(StoneColor.BLACK, move.player)
        assertEquals(1, move.moveNumber)
    }

    @Test fun placeStone_rejects_occupied() {
        val b = Board()
        b.placeStone(7, 7, StoneColor.BLACK)
        assertFalse(b.placeStone(7, 7, StoneColor.WHITE))
        assertEquals(StoneColor.BLACK, b.getStone(7, 7))
        assertEquals(1, b.moveCount)
    }

    @Test fun placeStone_rejects_out_of_bounds() {
        val b = Board()
        assertFalse(b.placeStone(-1, 0, StoneColor.BLACK))
        assertFalse(b.placeStone(15, 14, StoneColor.BLACK))
        assertEquals(0, b.moveCount)
    }

    @Test fun undoLastMove_returns_lastMove_and_clears_cell() {
        val b = Board()
        b.placeStone(3, 4, StoneColor.BLACK)
        b.placeStone(5, 6, StoneColor.WHITE)
        val undone = b.undoLastMove()
        assertNotNull(undone)
        assertEquals(StoneColor.WHITE, undone.player)
        assertEquals(5, undone.row); assertEquals(6, undone.col)
        assertNull(b.getStone(5, 6))
        assertTrue(b.isEmpty(5, 6))
        assertEquals(1, b.moveCount)
    }

    @Test fun undoLastMove_on_empty_returns_null() {
        val b = Board()
        assertNull(b.undoLastMove())
    }

    @Test fun clear_resets_grid_and_history() {
        val b = Board()
        b.placeStone(0, 0, StoneColor.BLACK)
        b.placeStone(1, 1, StoneColor.WHITE)
        b.clear()
        assertEquals(0, b.moveCount)
        assertNull(b.getStone(0, 0))
        assertNull(b.getStone(1, 1))
        assertEquals(225, b.getEmptyPositions().size)
    }

    @Test fun getEmptyPositions_decreases_with_each_placement() {
        val b = Board()
        b.placeStone(7, 7, StoneColor.BLACK)
        assertEquals(224, b.getEmptyPositions().size)
        b.placeStone(7, 8, StoneColor.WHITE)
        assertEquals(223, b.getEmptyPositions().size)
    }

    @Test fun moveHistory_returns_a_copy() {
        val b = Board()
        b.placeStone(0, 0, StoneColor.BLACK)
        val snapshot = b.moveHistory
        b.placeStone(1, 1, StoneColor.WHITE)
        // Изменения после снимка не должны попасть в старый список.
        assertEquals(1, snapshot.size)
        assertEquals(2, b.moveHistory.size)
    }
}
