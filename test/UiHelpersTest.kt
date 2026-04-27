import ui.capsTracked
import ui.rgba255
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UiHelpersTest {

    @Test fun capsTracked_default_intensity_inserts_single_space() {
        assertEquals("G O M O K U", capsTracked("Gomoku"))
        assertEquals("A B C", capsTracked("abc"))
    }

    @Test fun capsTracked_intensity_two() {
        assertEquals("G  O  M  O", capsTracked("Gomo", intensity = 2))
    }

    @Test fun capsTracked_uppercase_already() {
        assertEquals("Х О Д", capsTracked("Ход"))
    }

    @Test fun rgba255_alpha_zero() {
        val c = rgba255(255, 0, 0, 0.0)
        assertEquals(0, c.a)
        assertEquals(255, c.r)
    }

    @Test fun rgba255_alpha_full() {
        val c = rgba255(0, 255, 0, 1.0)
        assertEquals(255, c.a)
        assertEquals(255, c.g)
    }

    @Test fun rgba255_alpha_half() {
        val c = rgba255(100, 100, 100, 0.5)
        // 0.5 * 255 = 127.5 → toInt → 127
        assertEquals(127, c.a)
    }

    @Test fun rgba255_alpha_clamped() {
        // Значения вне [0, 1] должны быть зажаты.
        assertEquals(0, rgba255(0, 0, 0, -1.0).a)
        assertEquals(255, rgba255(0, 0, 0, 2.0).a)
    }

    @Test fun rgba255_preserves_rgb_channels() {
        val c = rgba255(50, 100, 150, 0.5)
        assertEquals(50, c.r)
        assertEquals(100, c.g)
        assertEquals(150, c.b)
    }
}
