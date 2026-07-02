package scenes

import korlibs.event.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import logic.*
import logic.ai.Difficulty
import ui.*

class SettingsScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        kinPaperBackground(theme)

        onBackOrEscape { Nav.goMenu() }

        kinTextButton(Str.BACK, color = theme.muted) { Nav.goMenu() }
            .apply {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, 22.0)
            }

        var y = 56.0

        kinText(Str.SETTINGS_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0

        container {
            kinSeam(
                x1 = 2.0, y1 = 6.0, x2 = 94.0, y2 = 6.0,
                jitter = 2.5, seed = 41, width = 1.3,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y)
        y += 16.0

        kinText(Str.SETTINGS_SECTION, Type.labelCaps, theme.muted) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 28.0

        // Тема
        kinFieldLabel(Str.SETTINGS_THEME, theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0
        kinSegmented(
            items = listOf(Str.THEME_LIGHT, Str.THEME_DARK),
            initialIndex = if (SettingsStore.current.dark) 1 else 0,
            totalWidth = w - 48.0,
            theme = theme,
            onChange = { idx ->
                val wantsDark = idx == 1
                if (Theme.dark != wantsDark) {
                    Theme.set(wantsDark)
                    Nav.goSettings()
                }
            },
        ).position(24.0, y)
        y += 42.0 + 18.0

        // Сложность AI
        kinFieldLabel(Str.SETTINGS_DIFFICULTY, theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0
        val diffs = listOf(Difficulty.EASY, Difficulty.MID, Difficulty.HARD)
        kinSegmented(
            items = diffs.map { it.label },
            initialIndex = diffs.indexOf(SettingsStore.current.aiDifficulty).coerceAtLeast(0),
            totalWidth = w - 48.0,
            theme = theme,
            onChange = { idx ->
                SettingsStore.update { it.copy(aiDifficulty = diffs[idx]) }
            },
        ).position(24.0, y)
        y += 42.0 + 18.0

        // Цвет игрока в AI-режиме (чередование по умолчанию)
        kinFieldLabel(Str.SETTINGS_PLAYER_COLOR, theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0
        val colorPrefs = listOf(PlayerColorPref.ALTERNATE, PlayerColorPref.WHITE, PlayerColorPref.BLACK)
        kinSegmented(
            items = listOf(Str.COLOR_ALTERNATE, Str.COLOR_WHITE, Str.COLOR_BLACK),
            initialIndex = colorPrefs.indexOf(SettingsStore.current.playerColor).coerceAtLeast(0),
            totalWidth = w - 48.0,
            theme = theme,
            onChange = { idx ->
                SettingsStore.update { it.copy(playerColor = colorPrefs[idx]) }
            },
        ).position(24.0, y)
        y += 42.0 + 18.0

        // Помощники
        kinFieldLabel(Str.SETTINGS_HELPERS, theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0

        renderToggleRow(
            this, w, y, Str.SETTINGS_HINTS, SettingsStore.current.hints, theme,
            onChange = { v -> SettingsStore.update { it.copy(hints = v) } },
        )
        y += 44.0

        renderToggleRow(
            this, w, y, Str.SETTINGS_CONFIRM, SettingsStore.current.confirmMoves, theme,
            onChange = { v -> SettingsStore.update { it.copy(confirmMoves = v) } },
        )
        y += 44.0

        renderToggleRow(
            this, w, y, Str.SETTINGS_SOUND, SettingsStore.current.sound, theme,
            onChange = { v -> SettingsStore.update { it.copy(sound = v) } },
        )
        y += 44.0

        renderToggleRow(
            this, w, y, Str.SETTINGS_MUSIC, SettingsStore.current.music, theme,
            last = true,
            onChange = { v ->
                SettingsStore.update { it.copy(music = v) }
                MusicPlayer.sync()
            },
        )
        y += 44.0

        // Footer
        val footerY = h - 36.0
        solidRect(w - 48.0, 1.0, theme.line) { position(24.0, footerY - 12.0) }
        kinText(Str.SETTINGS_VERSION_PREFIX + AppVersion.VERSION, 11.0, theme.muted, Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, footerY)
        }
        circle(radius = 3.0, fill = theme.gold) { position(w - 100.0, footerY) }
        kinText(Str.SETTINGS_ONLINE, 11.0, theme.gold, Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(w - 92.0, footerY)
        }
    }

    private fun renderToggleRow(
        host: Container, w: Double, y: Double,
        label: String, initial: Boolean, theme: KinPalette,
        last: Boolean = false,
        onChange: (Boolean) -> Unit,
    ) {
        host.kinText(label, Type.body, theme.ink) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, y + 22.0)
        }
        host.kinToggle(initial, theme = theme, onChange = onChange)
            .position(w - 24.0 - 44.0, y + 10.0)
        if (!last) {
            host.solidRect(w - 48.0, 1.0, theme.line) { position(24.0, y + 43.0) }
        }
    }
}
