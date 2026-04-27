import korlibs.image.color.Colors
import ui.KIN_DARK
import ui.KIN_LIGHT
import ui.blackStonePalette
import ui.whiteStonePalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaletteTest {

    @Test fun light_dark_flag_set_correctly() {
        assertFalse(KIN_LIGHT.isDark)
        assertTrue(KIN_DARK.isDark)
    }

    @Test fun light_paper_matches_design_token() {
        assertEquals(Colors["#f5f0e6"], KIN_LIGHT.paper)
        assertEquals(Colors["#1a1814"], KIN_LIGHT.ink)
        assertEquals(Colors["#b48a3c"], KIN_LIGHT.gold)
        assertEquals(Colors["#c1442a"], KIN_LIGHT.vermillion)
    }

    @Test fun dark_paper_matches_design_token() {
        assertEquals(Colors["#14130f"], KIN_DARK.paper)
        assertEquals(Colors["#f0ede4"], KIN_DARK.ink)
        assertEquals(Colors["#d4a652"], KIN_DARK.gold)
        assertEquals(Colors["#d35a3e"], KIN_DARK.vermillion)
    }

    @Test fun line_alpha_is_partial() {
        // line @ 0.10, line_firm @ 0.32 — должны иметь альфу < 255
        assertTrue(KIN_LIGHT.line.a < 255)
        assertTrue(KIN_LIGHT.lineFirm.a < 255)
        assertTrue(KIN_LIGHT.line.a < KIN_LIGHT.lineFirm.a)
    }

    @Test fun light_and_dark_differ_substantially() {
        // Палитры не должны совпадать ни по одному из крупных полей.
        assertNotEquals(KIN_LIGHT.paper, KIN_DARK.paper)
        assertNotEquals(KIN_LIGHT.ink, KIN_DARK.ink)
        assertNotEquals(KIN_LIGHT.gold, KIN_DARK.gold)
        assertNotEquals(KIN_LIGHT.muted, KIN_DARK.muted)
    }

    @Test fun blackStone_dark_theme_uses_lighter_inner_for_visibility() {
        val light = blackStonePalette(KIN_LIGHT)
        val dark = blackStonePalette(KIN_DARK)
        // Тёмная тема: чёрный камень должен быть светлее в центре
        // (#9a8f80) чем на светлой теме (#4a4640).
        assertTrue(dark.inner.r > light.inner.r)
        assertTrue(dark.inner.g > light.inner.g)
        assertTrue(dark.inner.b > light.inner.b)
    }

    @Test fun blackStone_dark_theme_needs_light_outline() {
        assertTrue(blackStonePalette(KIN_DARK).needsLightOutline)
        assertFalse(blackStonePalette(KIN_LIGHT).needsLightOutline)
    }

    @Test fun blackStone_no_border_either_theme() {
        assertNull(blackStonePalette(KIN_LIGHT).border)
        assertNull(blackStonePalette(KIN_DARK).border)
    }

    @Test fun whiteStone_independent_of_theme() {
        val light = whiteStonePalette(KIN_LIGHT)
        val dark = whiteStonePalette(KIN_DARK)
        assertEquals(light.inner, dark.inner)
        assertEquals(light.outer, dark.outer)
    }

    @Test fun whiteStone_has_dark_border_for_paper_readability() {
        val w = whiteStonePalette(KIN_LIGHT)
        // Border должен быть прозрачным чёрным (alpha < 255).
        val border = w.border
        assertTrue(border != null && border.a < 255)
    }
}
