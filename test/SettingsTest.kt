import logic.Settings
import logic.SettingsStore
import logic.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {

    @Test fun defaults_are_safe_first_run() {
        val s = Settings()
        assertFalse(s.dark)
        assertEquals(Difficulty.MID, s.aiDifficulty)
        assertFalse(s.hints)
        assertTrue(s.sound)
        assertTrue(s.firstRun)
    }

    @Test fun copy_keeps_other_fields() {
        val base = Settings()
        val withDark = base.copy(dark = true)
        assertTrue(withDark.dark)
        assertEquals(base.aiDifficulty, withDark.aiDifficulty)
        assertEquals(base.hints, withDark.hints)
        assertEquals(base.sound, withDark.sound)
        assertEquals(base.firstRun, withDark.firstRun)
    }

    @Test fun store_update_block_changes_current() {
        val before = SettingsStore.current
        SettingsStore.update { it.copy(hints = !it.hints) }
        assertEquals(!before.hints, SettingsStore.current.hints)
        // вернуть как было — не оставлять глобальное состояние грязным
        SettingsStore.update { it.copy(hints = before.hints) }
    }

    @Test fun store_set_replaces_current() {
        val before = SettingsStore.current
        val replacement = Settings(
            dark = !before.dark,
            aiDifficulty = Difficulty.HARD,
            hints = true,
            sound = false,
            firstRun = false,
        )
        SettingsStore.set(replacement)
        assertEquals(replacement, SettingsStore.current)
        SettingsStore.set(before)
    }
}
