package scenes

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.time.*
import model.*
import ui.*
import kotlin.math.min

// Финальные экраны партии — полноэкранные сцены (редизайн 2026-07;
// оверлей поверх доски заменён, просмотр партии — через «Кифу»).

class VictoryScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val cx = w / 2.0
        val record = GameSession.lastRecord
        val result = record?.result ?: GameSession.game.gameState

        kinPaperBackground(theme)
        onBackOrEscape { Nav.goMenu() }

        // акай ито вдоль правого поля
        kinAkaiItoV(380.0, theme).position(w - 30.0, 96.0)

        val seal = GameSession.lastEarnedSeals.firstOrNull()
        var y = if (seal != null) 78.0 else 104.0

        // Камень победителя с жилой сквозь него
        val winnerBlack = result != GameState.WHITE_WINS
        container {
            kinStone(isBlack = winnerBlack, radius = 42.0, theme = theme)
            kinSeam(
                x1 = -50.0, y1 = 38.0, x2 = 50.0, y2 = -38.0,
                jitter = 5.0, seed = 11, width = 2.8, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
            kinSeam(
                x1 = 0.0, y1 = 0.0, x2 = 22.0, y2 = -14.0,
                jitter = 2.0, seed = 17, width = 1.4, opacity = 0.85,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(cx, y + 42.0)
        y += 84.0 + 22.0

        kinText(Str.VICTORY_META, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
        y += 20.0

        val title = when (result) {
            GameState.BLACK_WINS -> Str.BLACK_WINS
            GameState.WHITE_WINS -> Str.WHITE_WINS
            else -> Str.DRAW
        }
        kinText(title, 32.0, theme.ink, Fonts.serif) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
        y += 46.0 + 12.0

        // Снимок финальной позиции
        val boardSize = 100.0
        if (record != null) {
            kinMiniBoard(record.moves, record.winLine, boardSize, theme, showSeam = true)
                .position(cx - boardSize / 2.0, y)
            y += boardSize + 12.0
        }

        // Новая печать — оттиск ханко
        if (seal != null) {
            renderSealStamp(this, cx, y, seal, theme)
            y += 48.0 + 10.0
        }

        // Жилы, расходящиеся от камня
        container {
            kinSeam(2.0, 22.0, 130.0, 18.0, width = 1.5, jitter = 3.0, seed = 81, branches = true, color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(130.0, 18.0, 258.0, 26.0, width = 1.4, jitter = 3.0, seed = 83, branches = true, color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(92.0, 20.0, 70.0, 4.0, width = 0.9, jitter = 2.0, seed = 87, opacity = 0.8, color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(170.0, 20.0, 196.0, 36.0, width = 0.9, jitter = 2.0, seed = 89, opacity = 0.8, color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(210.0, 24.0, 236.0, 6.0, width = 0.7, jitter = 1.5, seed = 93, opacity = 0.7, color = theme.gold, colorSoft = theme.goldSoft)
        }.position(cx - 130.0, y)
        y += 40.0 + 10.0

        val btnW = 280.0
        val btnX = cx - btnW / 2.0
        kinButton(width = btnW, label = Str.VICTORY_AGAIN, primary = true, centered = true, theme = theme) {
            GameSession.newGame(GameSession.mode)
            Nav.goGameKeepState()
        }.position(btnX, y)
        y += 52.0 + 10.0
        kinButton(width = btnW, label = Str.VICTORY_TO_MENU, centered = true, theme = theme) {
            Nav.goMenu()
        }.position(btnX, y)
        y += 52.0 + 16.0

        if (record != null) {
            kinTextButton(Str.VICTORY_KIFU, color = theme.muted) {
                Nav.goKifu(record) { Nav.goVictory() }
            }.apply {
                alignment = TextAlignment.TOP_CENTER
                position(cx, y)
            }
            y += 24.0
        }
        // Вернуться к доске: финальная позиция с победной жилой, отмена
        // хода возвращает партию в игру (GameSession.reopen).
        kinTextButton(Str.VICTORY_VIEW_BOARD, color = theme.muted) {
            Nav.goGameKeepState()
        }.apply {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
    }
}

class DefeatScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val cx = w / 2.0
        val record = GameSession.lastRecord

        kinPaperBackground(theme)
        onBackOrEscape { Nav.goMenu() }

        var y = 128.0
        kinText(Str.VICTORY_META, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
        y += 24.0
        kinText(Str.DEFEAT_TITLE, 36.0, theme.ink, Fonts.serif) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
        y += 50.0
        kinText(Str.DEFEAT_SUB, Type.captionItalic, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
        y += 22.0 + 32.0

        // Разбитый камень, который чинят золотом — кинцуги-момент
        kinBrokenStone(theme).position(cx - 52.0, y)
        y += 104.0 + 24.0

        // жилы расходятся от камня — починка началась
        container {
            kinSeam(120.0, 6.0, 30.0, 26.0, width = 1.3, jitter = 3.0, seed = 61, branches = true, color = theme.gold, colorSoft = theme.goldSoft)
            kinSeam(120.0, 6.0, 214.0, 22.0, width = 1.2, jitter = 3.0, seed = 67, branches = true, color = theme.gold, colorSoft = theme.goldSoft)
        }.position(cx - 120.0, y)
        y += 34.0 + 22.0

        val btnW = 280.0
        val btnX = cx - btnW / 2.0
        kinButton(width = btnW, label = Str.DEFEAT_REMATCH, primary = true, centered = true, theme = theme) {
            GameSession.newGame(GameSession.mode)
            Nav.goGameKeepState()
        }.position(btnX, y)
        y += 52.0 + 12.0
        kinButton(width = btnW, label = Str.VICTORY_TO_MENU, centered = true, theme = theme) {
            Nav.goMenu()
        }.position(btnX, y)
        y += 52.0 + 16.0

        if (record != null) {
            kinTextButton(Str.VICTORY_KIFU, color = theme.muted) {
                Nav.goKifu(record) { Nav.goDefeat() }
            }.apply {
                alignment = TextAlignment.TOP_CENTER
                position(cx, y)
            }
            y += 24.0
        }
        kinTextButton(Str.VICTORY_VIEW_BOARD, color = theme.muted) {
            Nav.goGameKeepState()
        }.apply {
            alignment = TextAlignment.TOP_CENTER
            position(cx, y)
        }
    }
}

// Оттиск печати: киноварный квадрат с иероглифом + подпись. Анимация —
// «шлепок» ханко: scale 1.6→1 с лёгким поворотом.
private fun renderSealStamp(host: Container, cx: Double, y: Double, seal: String, theme: KinPalette) {
    val chipW = 200.0
    val row = host.container { }.position(cx - chipW / 2.0, y)
    val stamp = row.container {
        roundRect(Size(40.0, 40.0), RectCorners(5.0), fill = theme.vermillion)
        kinText(seal, 20.0, BtnSpec.primaryFg, Fonts.serifJp) {
            alignment = TextAlignment.MIDDLE_CENTER
            position(20.0, 21.0)
        }
    }
    // якорь в центре штампа, чтобы масштаб «шлёпал» из середины
    stamp.position(20.0, 20.0)
    stamp.forEachChild { it.position(it.x - 20.0, it.y - 20.0) }

    row.kinText(Str.VICTORY_NEW_SEAL, 9.0, theme.muted, Fonts.uiMedium) {
        alignment = TextAlignment.TOP_LEFT
        position(52.0, 6.0)
    }
    row.kinText(Str.sealName(seal), 13.0, theme.ink, Fonts.uiSemiBold) {
        alignment = TextAlignment.TOP_LEFT
        position(52.0, 20.0)
    }

    var t = -0.35 // задержка перед оттиском
    stamp.alpha = 0.0
    var upd: Cancellable? = null
    upd = stamp.addUpdater { dt ->
        t = min(1.0, t + dt.milliseconds / 450.0)
        if (t <= 0.0) return@addUpdater
        stamp.alpha = min(1.0, t * 3.0)
        val k = 1.0 - t
        stamp.scale(1.0 + 0.6 * k * k)
        stamp.rotation = (-2.0 - 7.0 * k * k).degrees
        if (t >= 1.0) upd?.cancel()
    }
}

// Разбитый камень: две «половины» (клип-контейнеры с лёгким поворотом,
// зазор между ними — скол) + золотая жила по трещине + осколки у подножия.
private fun Container.kinBrokenStone(theme: KinPalette): Container = container {
    val r = 52.0
    val leftClip = clipContainer(Size(50.0, 112.0)) {
        position(0.0, -4.0)
        rotation = (-2.0).degrees
    }
    leftClip.kinStone(isBlack = true, radius = r, theme = theme).position(52.0, 56.0)
    val rightClip = clipContainer(Size(56.0, 112.0)) {
        position(54.0, -4.0)
        rotation = 1.5.degrees
    }
    rightClip.kinStone(isBlack = true, radius = r, theme = theme).position(-2.0, 56.0)

    // жила по трещине
    kinSeam(
        x1 = 60.0, y1 = -16.0, x2 = 44.0, y2 = 118.0,
        jitter = 5.0, seed = 29, width = 2.6, branches = true,
        color = theme.gold, colorSoft = theme.goldSoft,
    )

    // осколки
    circle(2.5, if (theme.isDark) Colors["#4a4438"] else Colors["#2a2620"]) { position(20.0, 110.0) }
    circle(1.5, theme.gold) { position(36.0, 114.0) }
}
