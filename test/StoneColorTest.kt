import model.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StoneColorTest {

    @Test fun opposite_inverts() {
        assertEquals(StoneColor.WHITE, StoneColor.BLACK.opposite)
        assertEquals(StoneColor.BLACK, StoneColor.WHITE.opposite)
    }

    @Test fun opposite_double_returns_self() {
        for (c in StoneColor.entries) {
            assertEquals(c, c.opposite.opposite)
        }
    }

    @Test fun symbols_are_distinct_and_classic() {
        assertEquals("●", StoneColor.BLACK.symbol)
        assertEquals("○", StoneColor.WHITE.symbol)
        assertNotEquals(StoneColor.BLACK.symbol, StoneColor.WHITE.symbol)
    }
}
