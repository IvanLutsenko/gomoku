import logic.SettingsStore
import ui.KIN_DARK
import ui.KIN_LIGHT
import ui.Theme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {

    private var saved = SettingsStore.current

    @BeforeTest fun snapshotState() {
        saved = SettingsStore.current
    }

    @AfterTest fun restoreState() {
        SettingsStore.set(saved)
    }

    @Test fun dark_getter_reflects_settings_store() {
        SettingsStore.update { it.copy(dark = false) }
        assertFalse(Theme.dark)
        SettingsStore.update { it.copy(dark = true) }
        assertTrue(Theme.dark)
    }

    @Test fun colors_returns_light_palette_when_not_dark() {
        SettingsStore.update { it.copy(dark = false) }
        assertEquals(KIN_LIGHT, Theme.colors)
    }

    @Test fun colors_returns_dark_palette_when_dark() {
        SettingsStore.update { it.copy(dark = true) }
        assertEquals(KIN_DARK, Theme.colors)
    }

    @Test fun toggle_inverts_dark_flag_in_store() {
        SettingsStore.update { it.copy(dark = false) }
        Theme.toggle()
        assertTrue(SettingsStore.current.dark)
        Theme.toggle()
        assertFalse(SettingsStore.current.dark)
    }

    @Test fun toggle_swaps_palette_too() {
        SettingsStore.update { it.copy(dark = false) }
        val before = Theme.colors
        Theme.toggle()
        assertEquals(if (before == KIN_LIGHT) KIN_DARK else KIN_LIGHT, Theme.colors)
    }

    @Test fun set_idempotent_no_double_invocation() {
        SettingsStore.update { it.copy(dark = true) }
        val mutationsBefore = SettingsStore.current
        Theme.set(true) // уже true, no-op
        assertEquals(mutationsBefore, SettingsStore.current)
    }

    @Test fun set_writes_through_to_store() {
        SettingsStore.update { it.copy(dark = false) }
        Theme.set(true)
        assertTrue(SettingsStore.current.dark)
    }
}
