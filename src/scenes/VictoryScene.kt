package scenes

import korlibs.event.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import model.*
import ui.*

class VictoryScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()
        val centerX = w / 2.0

        solidRect(w, h, theme.paper)

        keys.down {
            if (it.key == Key.BACK || it.key == Key.ESCAPE) Nav.goMenu()
        }

        val winner = Nav.currentVictoryWinner

        var y = h * 0.18

        // Камень (96 px) с золотыми жилами поверх
        container {
            position(centerX, y)
            // Камень
            kinStone(
                isBlack = winner == StoneColor.BLACK,
                radius = 48.0,
                isWin = false,
                theme = theme,
            )
            // Главная жила пересекает камень + ответвление
            kinSeam(
                x1 = -28.0, y1 = 48.0, x2 = 60.0, y2 = -28.0,
                jitter = 5.0, seed = 11, width = 2.8, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
            kinSeam(
                x1 = 0.0, y1 = 0.0, x2 = 22.0, y2 = -14.0,
                jitter = 2.0, seed = 17, width = 1.4, opacity = 0.85,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }
        y += 96.0 + 28.0

        text("ПАРТИЯ ЗАВЕРШЕНА", Type.meta.size, theme.muted, font = Fonts.uiMedium) {
            alignment = TextAlignment.TOP_CENTER
            position(centerX, y)
        }
        y += 22.0

        text(
            if (winner == StoneColor.BLACK) "Чёрные победили" else "Белые победили",
            40.0, theme.ink, font = Fonts.serif,
        ) {
            alignment = TextAlignment.TOP_CENTER
            position(centerX, y)
        }
        y += 56.0

        // Композиция из пяти жил («корень + 4 ветви»)
        container {
            position(centerX - 130.0, y)
            kinSeam(2.0, 22.0, 130.0, 18.0, jitter = 3.0, seed = 81, width = 1.5, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(130.0, 18.0, 258.0, 26.0, jitter = 3.0, seed = 83, width = 1.4, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(92.0, 20.0, 70.0, 4.0, jitter = 2.0, seed = 87, width = 0.9, opacity = 0.8,
                color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(170.0, 20.0, 196.0, 36.0, jitter = 2.0, seed = 89, width = 0.9, opacity = 0.8,
                color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(210.0, 24.0, 236.0, 6.0, jitter = 1.5, seed = 93, width = 0.7, opacity = 0.7,
                color = theme.gold, colorSoft = theme.goldSoft)
        }
        y += 70.0

        // Кнопки
        val btnW = 280.0
        val btnX = centerX - btnW / 2.0

        kinButton(
            width = btnW, label = "Ещё партию", primary = true, centered = true,
            theme = theme,
            onPress = {
                GameSession.newGame(GameSession.mode)
                Nav.goGameKeepState()
            },
        ).position(btnX, y)
        y += 64.0

        kinButton(
            width = btnW, label = "В меню", centered = true, theme = theme,
            onPress = { Nav.goMenu() },
        ).position(btnX, y)
    }
}
