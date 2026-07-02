package scenes

import korlibs.event.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.service.vibration.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.*
import logic.*
import logic.ai.*
import model.*
import ui.*

class GameScene : Scene() {

    private val humanColor = StoneColor.BLACK
    private val aiColor = StoneColor.WHITE

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()
        val game = GameSession.game
        val mode = GameSession.mode
        val ai: AiPlayer = AiFactory.create(SettingsStore.current.aiDifficulty)

        solidRect(w, h, theme.paper)

        onBackOrEscape { Nav.goMenu() }

        // ── Top bar ─────────────────────────────────────────────────────
        kinText("五目", Type.subtitle.size, theme.ink, Fonts.serifJp) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 20.0)
        }
        val modeLabel = if (mode == GameMode.AI)
            "VS  AI · ${SettingsStore.current.aiDifficulty.label.uppercase()}"
        else "VS  ИГРОК"
        kinText(modeLabel, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 50.0)
        }

        val menuBtnW = 64.0
        kinButton(
            width = menuBtnW, label = "МЕНЮ", small = true, centered = true,
            theme = theme, onPress = { Nav.goMenu() },
        ).position(w - menuBtnW - 24.0, 20.0)

        // Квадратная themed-кнопка в стиле «МЕНЮ» — как в дизайне игрового экрана
        // (круглая — только в главном меню).
        kinButton(
            width = 44.0, label = if (theme.isDark) "☾" else "☀",
            small = true, centered = true, theme = theme,
            onPress = { Theme.toggle(); Nav.goGameKeepState() },
        ).position(w - menuBtnW - 24.0 - 44.0 - 8.0, 20.0)

        // ── Layout ──────────────────────────────────────────────────────
        val turnIndicator = container { }.apply { position(w / 2.0, 92.0) }

        val boardX = (w - BoardSpec.TOTAL) / 2.0
        val boardY = 116.0

        container {
            kinSeam(
                x1 = 4.0, y1 = 32.0, x2 = 48.0, y2 = 6.0,
                jitter = 2.5, seed = 67, width = 1.2, opacity = 0.75,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(boardX - 18.0, boardY - 18.0)

        val boardView = KinBoardView(game, theme) { _, _ -> }
        addChild(boardView.apply { position(boardX, boardY) })

        container {
            kinSeam(
                x1 = 4.0, y1 = 42.0, x2 = 44.0, y2 = 8.0,
                jitter = 2.0, seed = 71, width = 1.0, opacity = 0.6,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(boardX + BoardSpec.TOTAL - 28.0, boardY + BoardSpec.TOTAL - 28.0)

        val bottomY = h - 78.0
        val moveCountText = kinText(
            "ХОД ${game.getMoveCount()}", Type.meta, theme.muted,
        ).apply {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, bottomY)
        }
        val historyContainer = container { }.position(w - 24.0, bottomY)
        val controlsContainer = container { }
        val btnY = h - 50.0
        val btnW = (w - 24.0 * 2.0 - 10.0) / 2.0

        // ── Логика. Чтобы избежать forward-reference проблемы локальных
        // функций, держим колбэки в `var`-переменных. ────────────────────
        var aiThinking = false

        var renderUi: () -> Unit = {}
        var undoAndRefresh: () -> Unit = {}
        var newGame: () -> Unit = {}
        var applyMoveRef: (Int, Int) -> Boolean = { _, _ -> false }

        fun isHumanTurn(): Boolean =
            game.gameState == GameState.PLAYING &&
                (mode == GameMode.PVP || game.currentPlayer == humanColor) &&
                !aiThinking

        renderUi = {
            boardView.redraw()
            renderTurnIndicator(turnIndicator, theme, aiThinking)
            moveCountText.text = "ХОД ${game.getMoveCount()}"
            renderHistoryDots(historyContainer, theme)
            renderHints(boardView, isHumanTurn() && SettingsStore.current.hints)
            renderControls(
                controlsContainer, theme, btnY, btnW, game,
                onUndo = { undoAndRefresh() }, onNew = { newGame() },
            )
        }

        applyMoveRef = lam@{ r, c ->
            val res = game.makeMove(r, c)
            if (res !is MoveResult.Success) return@lam false
            triggerHaptic(strong = res.winner != null)
            renderUi()
            if (res.gameState != GameState.PLAYING && res.winner != null) {
                launch {
                    delay(1200) // как в дизайне: дать победной жиле «прозвучать»
                    Nav.goVictory(res.winner!!)
                }
            }
            true
        }

        suspend fun aiTurn() {
            if (mode != GameMode.AI || game.gameState != GameState.PLAYING || game.currentPlayer != aiColor) return
            aiThinking = true
            renderUi()
            delay(450)
            val pos = withContext(Dispatchers.Default) { ai.chooseMove(game) }
            aiThinking = false
            applyMoveRef(pos.row, pos.col)
        }

        undoAndRefresh = {
            if (game.getMoveCount() > 0) {
                game.undoMove()
                if (mode == GameMode.AI && game.getMoveCount() > 0 && game.currentPlayer != humanColor) {
                    game.undoMove()
                }
                renderUi()
            }
        }

        newGame = {
            GameSession.newGame(mode)
            Nav.goGameKeepState()
        }

        boardView.onCellClickHandler = handler@{ r, c ->
            if (!isHumanTurn()) return@handler
            if (!applyMoveRef(r, c)) return@handler
            if (mode == GameMode.AI && game.gameState == GameState.PLAYING) {
                launch { aiTurn() }
            }
        }

        renderUi()

        if (mode == GameMode.AI && game.currentPlayer == aiColor && game.gameState == GameState.PLAYING) {
            launch { aiTurn() }
        }
    }

    private val vibration by lazy { NativeVibration(coroutineContext) }

    private fun triggerHaptic(strong: Boolean = false) {
        if (!SettingsStore.current.sound) return
        try {
            vibration.vibrate(
                time = if (strong) 60.milliseconds else 20.milliseconds,
                amplitude = if (strong) 0.9 else 0.5,
            )
        } catch (e: Throwable) {
            // нет permission / не поддерживается — игнорируем
        }
    }

    private fun renderTurnIndicator(host: Container, theme: KinPalette, aiThinking: Boolean) {
        host.removeChildren()
        val game = GameSession.game
        val (label, color) = when {
            aiThinking -> "AI думает…" to game.currentPlayer
            game.gameState == GameState.PLAYING -> when (game.currentPlayer) {
                StoneColor.BLACK -> "Ход чёрных" to StoneColor.BLACK
                StoneColor.WHITE -> "Ход белых" to StoneColor.WHITE
            }
            game.gameState == GameState.BLACK_WINS -> "Чёрные победили" to StoneColor.BLACK
            game.gameState == GameState.WHITE_WINS -> "Белые победили" to StoneColor.WHITE
            else -> "Ничья" to StoneColor.BLACK
        }
        val isBlack = color == StoneColor.BLACK
        val gameOver = game.gameState != GameState.PLAYING

        // Центрируем пару «камень + текст (+ ★)» по фактической ширине текста.
        val txt = host.kinText(label, Type.bodyStrong, theme.ink) {
            alignment = TextAlignment.MIDDLE_LEFT
        }
        val star = if (gameOver && game.gameState != GameState.DRAW) {
            host.kinText("★", Type.bodyStrong.size, theme.gold, Fonts.uiSemiBold) {
                alignment = TextAlignment.MIDDLE_LEFT
            }
        } else null

        val stoneD = 12.0
        val gap = 10.0
        val starGap = 8.0
        val totalW = stoneD + gap + txt.scaledWidth + (star?.let { starGap + it.scaledWidth } ?: 0.0)
        var x = -totalW / 2.0
        host.kinStone(isBlack = isBlack, radius = stoneD / 2.0, theme = theme)
            .position(x + stoneD / 2.0, 0.0)
        x += stoneD + gap
        txt.position(x, 0.0)
        x += txt.scaledWidth + starGap
        star?.position(x, 0.0)
    }

    private fun renderHistoryDots(host: Container, theme: KinPalette) {
        host.removeChildren()
        val moves = GameSession.game.getMoveHistory().takeLast(5)
        val gap = 12.0
        moves.forEachIndexed { i, m ->
            val x = -((moves.size - 1 - i) * gap)
            val isBlack = m.player == StoneColor.BLACK
            if (isBlack) {
                host.circle(radius = 4.0, fill = theme.ink) { position(x, 0.0) }
            } else {
                host.circle(radius = 4.0, fill = theme.paper, stroke = theme.lineFirm, strokeThickness = 0.5) {
                    position(x, 0.0)
                }
            }
        }
    }

    private fun renderHints(boardView: KinBoardView, enabled: Boolean) {
        if (!enabled) {
            boardView.clearHints()
            return
        }
        val hints = topMoves(GameSession.game, count = 3)
        boardView.showHints(hints.map { it.pos })
    }

    private fun renderControls(
        host: Container, theme: KinPalette, btnY: Double, btnW: Double,
        game: GameLogic,
        onUndo: () -> Unit, onNew: () -> Unit,
    ) {
        host.removeChildren()
        host.kinButton(
            width = btnW, label = "← Отменить", small = true, centered = true, theme = theme,
            // Как в дизайне: отмена доступна и после победы — undoMove
            // сбрасывает состояние в PLAYING, партию можно продолжить.
            enabled = game.getMoveCount() > 0,
            onPress = onUndo,
        ).position(24.0, btnY)
        host.kinButton(
            width = btnW, label = "Новая партия", primary = true, small = true, centered = true,
            theme = theme,
            onPress = onNew,
        ).position(24.0 + btnW + 10.0, btnY)
    }
}
