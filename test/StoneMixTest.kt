import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import ui.mix
import kotlin.test.Test
import kotlin.test.assertEquals

class StoneMixTest {

    private val red = RGBA(255, 0, 0, 255)
    private val blue = RGBA(0, 0, 255, 255)

    @Test fun mix_at_zero_returns_first() {
        assertEquals(red, mix(red, blue, 0.0))
    }

    @Test fun mix_at_one_returns_second() {
        assertEquals(blue, mix(red, blue, 1.0))
    }

    @Test fun mix_at_half_is_midpoint() {
        val m = mix(red, blue, 0.5)
        assertEquals(127, m.r) // (255 + 0) * 0.5 = 127.5 → 127
        assertEquals(0, m.g)
        assertEquals(127, m.b)
        assertEquals(255, m.a)
    }

    @Test fun mix_clamps_below_zero_to_first() {
        assertEquals(red, mix(red, blue, -0.5))
    }

    @Test fun mix_clamps_above_one_to_second() {
        assertEquals(blue, mix(red, blue, 1.5))
    }

    @Test fun mix_with_alpha_channel() {
        val opaque = RGBA(100, 100, 100, 255)
        val transparent = RGBA(100, 100, 100, 0)
        val m = mix(opaque, transparent, 0.5)
        assertEquals(127, m.a)
    }

    @Test fun mix_identity_when_colors_equal() {
        val c = Colors["#abcdef"]
        for (t in listOf(0.0, 0.25, 0.5, 0.75, 1.0)) {
            assertEquals(c, mix(c, c, t))
        }
    }
}
