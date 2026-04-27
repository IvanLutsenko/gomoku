import model.Position
import ui.BoardSpec
import ui.Spacing
import ui.Type
import ui.Viewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypographyAndConstantsTest {

    @Test fun type_styles_match_design_tokens() {
        // tokens/typography.json — нормативные значения. Сверяем размеры.
        assertEquals(56.0, Type.display.size)
        assertEquals(36.0, Type.title.size)
        assertEquals(22.0, Type.subtitle.size)
        assertEquals(14.0, Type.body.size)
        assertEquals(14.0, Type.bodyStrong.size)
        assertEquals(16.0, Type.button.size)
        assertEquals(14.0, Type.buttonSmall.size)
        assertEquals(13.0, Type.caption.size)
        assertEquals(13.0, Type.captionItalic.size)
        assertEquals(11.0, Type.meta.size)
        assertEquals(12.0, Type.labelCaps.size)
        assertEquals(12.0, Type.trackingBrand.size)
        assertEquals(28.0, Type.ideogram.size)
    }

    @Test fun board_spec_constants() {
        assertEquals(15, BoardSpec.SIZE_CELLS)
        assertEquals(22.0, BoardSpec.CELL)
        assertEquals(14.0, BoardSpec.PADDING)
        assertEquals(0.42, BoardSpec.STONE_RADIUS_RATIO)
        assertEquals(1.8, BoardSpec.HOSHI_RADIUS)
    }

    @Test fun board_total_formula() {
        // total = padding * 2 + cell * (N - 1)
        val expected = BoardSpec.PADDING * 2 + BoardSpec.CELL * (BoardSpec.SIZE_CELLS - 1)
        assertEquals(expected, BoardSpec.TOTAL)
    }

    @Test fun hoshi_includes_tengen_center() {
        // Тэнгэн (центр) на 15×15 — (7, 7).
        assertTrue(7 to 7 in BoardSpec.HOSHI)
    }

    @Test fun hoshi_has_5_points_symmetric() {
        // Стандарт: 4 угловых хоси + центр.
        assertEquals(5, BoardSpec.HOSHI.size)
        assertTrue(3 to 3 in BoardSpec.HOSHI)
        assertTrue(3 to 11 in BoardSpec.HOSHI)
        assertTrue(11 to 3 in BoardSpec.HOSHI)
        assertTrue(11 to 11 in BoardSpec.HOSHI)
    }

    @Test fun viewport_is_mobile_portrait() {
        assertEquals(360, Viewport.W)
        assertEquals(720, Viewport.H)
        assertTrue(Viewport.H > Viewport.W, "portrait should be taller than wide")
    }

    @Test fun spacing_scale_increases_monotonically() {
        // 4 / 8 / 14 / 24 / 32 / 40
        assertTrue(Spacing.XS < Spacing.SM)
        assertTrue(Spacing.SM < Spacing.MD)
        assertTrue(Spacing.MD < Spacing.LG)
        assertTrue(Spacing.LG < Spacing.XL)
        assertTrue(Spacing.XL < Spacing.XXL)
    }

    @Test fun spacing_values_match_design_tokens() {
        // tokens/spacing.json
        assertEquals(4.0, Spacing.XS)
        assertEquals(8.0, Spacing.SM)
        assertEquals(14.0, Spacing.MD)
        assertEquals(24.0, Spacing.LG)
        assertEquals(32.0, Spacing.XL)
        assertEquals(40.0, Spacing.XXL)
        assertEquals(24.0, Spacing.SCREEN_PAD_H)
    }

    @Test fun board_hoshi_within_bounds() {
        for ((r, c) in BoardSpec.HOSHI) {
            assertTrue(Position(r, c).isValid(), "hoshi ($r,$c) out of bounds")
        }
    }
}
