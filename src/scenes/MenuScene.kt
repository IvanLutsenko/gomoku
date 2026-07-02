package scenes

import korlibs.event.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import ui.*

class MenuScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()
        val centerX = w / 2.0

        solidRect(w, h, theme.paper)

        // Android Back из меню: НЕ вызываем preventDefault → KorgwActivity
        // делает super.onBackPressed() и закрывает activity. Желаемое поведение.
        // Desktop ESC: явно закрываем окно (там system back нет).
        keys.down {
            if (it.key == Key.ESCAPE) {
                gameWindow.close()
                it.preventDefault()
            }
        }

        // Theme toggle (top-right)
        kinIconRound(
            icon = if (theme.isDark) "☾" else "☀",
            theme = theme,
            onPress = {
                Theme.toggle()
                Nav.goMenu()
            },
        ).position(w - 56.0, 36.0)

        var y = 64.0

        // Энсо
        container { kinEnso(96.0, theme) }
            .position(centerX - 48.0, y)
        y += 96.0 + 8.0

        // 五目 — высота glyph-а ≈ Type.display.size, нужен явный gap.
        kinText("五目", Type.display.size, theme.ink, Fonts.serifJp) {
            alignment = TextAlignment.TOP_CENTER
            position(centerX, y)
        }
        y += Type.display.size + 12.0

        // G O M O K U (caps tracking)
        kinText("G  O  M  O  K  U", Type.trackingBrand, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(centerX, y)
        }
        y += 18.0

        // italic подзаголовок
        kinText("Пять камней в ряд", Type.captionItalic, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(centerX, y)
        }
        y += 22.0

        // Длинная декоративная жила
        container {
            kinSeam(
                x1 = 6.0, y1 = 22.0, x2 = 214.0, y2 = 12.0,
                jitter = 4.0, seed = 31, width = 1.5, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(centerX - 110.0, y)
        y += 34.0 + 36.0

        // Кнопки (max width 280, центрированы)
        val btnW = 280.0
        val btnX = centerX - btnW / 2.0

        kinButton(
            width = btnW, label = "Игра с AI", primary = true,
            rightArrow = true, theme = theme,
            onPress = { Nav.goGame(GameMode.AI) },
        ).position(btnX, y)
        y += 52.0 + 12.0

        kinButton(
            width = btnW, label = "Игра вдвоём",
            rightArrow = true, theme = theme, onPress = { Nav.goGame(GameMode.PVP) },
        ).position(btnX, y)
        y += 52.0 + 12.0

        // Маленькие кнопки в ряд
        val gap = 12.0
        val smallW = (btnW - gap) / 2.0
        kinButton(width = smallW, label = "Настройки", small = true, theme = theme, onPress = { Nav.goSettings() })
            .position(btnX, y)
        kinButton(width = smallW, label = "Помощь", small = true, theme = theme, onPress = { Nav.goHelp() })
            .position(btnX + smallW + gap, y)

        // Footer
        val footerY = h - 28.0
        circle(3.0, theme.gold) { position(centerX - 88.0, footerY) }
        kinText("2026 · KORGE EDITION", 11.0, theme.muted, Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(centerX - 80.0, footerY)
        }
    }
}
