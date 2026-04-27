import logic.MoveResult
import logic.UndoResult
import model.Move
import model.Position
import model.StoneColor
import model.WinningLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MoveTest {

    @Test fun move_position_getter_matches_row_col() {
        val m = Move(7, 7, StoneColor.BLACK, 1)
        assertEquals(Position(7, 7), m.position)
    }

    @Test fun move_notation_uses_position_notation() {
        val m = Move(0, 0, StoneColor.BLACK, 1)
        assertEquals("A1", m.notation)
        val m2 = Move(14, 14, StoneColor.WHITE, 5)
        assertEquals("O15", m2.notation)
    }

    @Test fun move_toString_contains_player_symbol_and_notation() {
        val s = Move(0, 0, StoneColor.BLACK, 3).toString()
        assertTrue("●" in s, "expected '●' in '$s'")
        assertTrue("A1" in s, "expected 'A1' in '$s'")
        assertTrue("3" in s, "expected move number 3 in '$s'")
    }

    @Test fun moves_with_same_fields_are_equal() {
        val a = Move(1, 2, StoneColor.WHITE, 4)
        val b = Move(1, 2, StoneColor.WHITE, 4)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test fun moves_differ_by_any_field() {
        val base = Move(1, 2, StoneColor.WHITE, 4)
        assertNotEquals(base, base.copy(row = 0))
        assertNotEquals(base, base.copy(col = 0))
        assertNotEquals(base, base.copy(player = StoneColor.BLACK))
        assertNotEquals(base, base.copy(moveNumber = 5))
    }

    @Test fun winningLine_equality() {
        val a = WinningLine(listOf(Position(0, 0), Position(0, 1)))
        val b = WinningLine(listOf(Position(0, 0), Position(0, 1)))
        assertEquals(a, b)
    }

    @Test fun moveResult_invalidMove_carries_reason() {
        val r = MoveResult.InvalidMove("занято")
        assertEquals("занято", r.reason)
    }

    @Test fun moveResult_gameEnded_carries_reason() {
        val r = MoveResult.GameEnded("уже победили")
        assertEquals("уже победили", r.reason)
    }

    @Test fun undoResult_no_moves_is_singleton() {
        val a = UndoResult.NoMovesToUndo
        val b = UndoResult.NoMovesToUndo
        assertTrue(a === b)
    }

    @Test fun undoResult_success_carries_undone_move() {
        val m = Move(7, 7, StoneColor.BLACK, 1)
        val r = UndoResult.Success(m)
        assertEquals(m, r.undoneMove)
    }

    @Test fun undoResult_error_carries_message() {
        val r = UndoResult.Error("не получилось")
        assertEquals("не получилось", r.message)
    }
}
