package scenes

import korlibs.image.text.*
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

        solidRect(w, h, theme.paper)

        kinTextButton("← Назад", color = theme.muted) { Nav.goMenu() }
            .apply {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, 22.0)
            }

        var y = 56.0

        text("Настройки", Type.title.size, theme.ink, font = Fonts.serif) {
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

        text("ПАРАМЕТРЫ ПАРТИИ", Type.labelCaps.size, theme.muted, font = Fonts.uiMedium) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 32.0

        // Тема
        kinFieldLabel("Тема", theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0
        kinSegmented(
            items = listOf("☀  Свет", "☾  Тьма"),
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
        y += 42.0 + 28.0

        // Сложность AI
        kinFieldLabel("Сложность AI", theme).apply {
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
        y += 42.0 + 28.0

        // Помощники
        kinFieldLabel("Помощники", theme).apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0

        renderToggleRow(
            this, w, y, "Подсказки ходов", SettingsStore.current.hints, theme,
            onChange = { v -> SettingsStore.update { it.copy(hints = v) } },
        )
        y += 48.0

        renderToggleRow(
            this, w, y, "Звуки и вибрация", SettingsStore.current.sound, theme,
            last = true,
            onChange = { v -> SettingsStore.update { it.copy(sound = v) } },
        )
        y += 48.0

        // Footer
        val footerY = h - 36.0
        solidRect(w - 48.0, 1.0, theme.line) { position(24.0, footerY - 12.0) }
        text("ВЕРСИЯ 1.0.4", 11.0, theme.muted, font = Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, footerY)
        }
        circle(radius = 3.0, fill = theme.gold) { position(w - 100.0, footerY) }
        text("ОНЛАЙН", 11.0, theme.muted, font = Fonts.uiMedium) {
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
        host.text(label, Type.body.size, theme.ink, font = Fonts.ui) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, y + 24.0)
        }
        host.kinToggle(initial, theme = theme, onChange = onChange)
            .position(w - 24.0 - 44.0, y + 12.0)
        if (!last) {
            host.solidRect(w - 48.0, 1.0, theme.line) { position(24.0, y + 47.0) }
        }
    }
}
