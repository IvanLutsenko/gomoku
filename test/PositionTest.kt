import model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PositionTest {

    @Test fun notation_roundtrip_corners() {
        // A1 ⇒ (row=0, col=0)
        assertEquals(Position(0, 0), Position.fromNotation("A1"))
        assertEquals("A1", Position(0, 0).toNotation())
        // O15 ⇒ (row=14, col=14)
        assertEquals(Position(14, 14), Position.fromNotation("O15"))
        assertEquals("O15", Position(14, 14).toNotation())
    }

    @Test fun notation_arbitrary_roundtrip() {
        val cases = listOf(
            Position(0, 7) to "H1",
            Position(7, 7) to "H8",
            Position(14, 0) to "A15",
            Position(3, 11) to "L4",
        )
        for ((pos, notation) in cases) {
            assertEquals(notation, pos.toNotation(), "toNotation $pos")
            assertEquals(pos, Position.fromNotation(notation), "fromNotation $notation")
        }
    }

    @Test fun notation_lowercase_accepted() {
        assertEquals(Position(0, 0), Position.fromNotation("a1"))
        assertEquals(Position(7, 7), Position.fromNotation("h8"))
    }

    @Test fun notation_invalid_returns_null() {
        // Слишком короткая
        assertNull(Position.fromNotation("A"))
        // Буква вне A..O
        assertNull(Position.fromNotation("Z1"))
        // Номер вне 1..15
        assertNull(Position.fromNotation("A0"))
        assertNull(Position.fromNotation("A16"))
        // Не число
        assertNull(Position.fromNotation("Axx"))
    }

    @Test fun isValid_within_15x15() {
        assertTrue(Position(0, 0).isValid())
        assertTrue(Position(14, 14).isValid())
        assertTrue(Position(7, 7).isValid())
    }

    @Test fun isValid_out_of_bounds() {
        assertEquals(false, Position(-1, 0).isValid())
        assertEquals(false, Position(0, -1).isValid())
        assertEquals(false, Position(15, 0).isValid())
        assertEquals(false, Position(0, 15).isValid())
    }
}
