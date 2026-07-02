package scenes

import korlibs.audio.sound.*
import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.file.std.*
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

    // Цвет человека задаётся настройкой (чередование/белые/чёрные) в GameSession.
    private val humanColor: StoneColor get() = GameSession.humanColor
    private val aiColor: StoneColor get() = GameSession.humanColor.opposite

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()
        val game = GameSession.game
        val mode = GameSession.mode
        val ai: AiPlayer = AiFactory.create(SettingsStore.current.aiDifficulty)
        stoneSound = runCatching { resourcesVfs["sounds/stone.wav"].readSound() }.getOrNull()

        kinPaperBackground(theme)

        onBackOrEscape { Nav.goMenu() }

        // ── Top bar ─────────────────────────────────────────────────────
        kinText(Str.BRAND_IDEOGRAM, Type.subtitle.size, theme.ink, Fonts.serifJp) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 20.0)
        }
        val modeLabel = if (mode == GameMode.AI)
            Str.GAME_VS_AI_PREFIX + SettingsStore.current.aiDifficulty.label.uppercase()
        else Str.GAME_VS_HUMAN
        kinText(modeLabel, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 50.0)
        }

        val menuBtnW = 64.0
        kinButton(
            width = menuBtnW, label = Str.GAME_MENU, small = true, centered = true,
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
            Str.GAME_MOVE_PREFIX + game.getMoveCount(), Type.meta, theme.muted,
        ).apply {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, bottomY)
        }
        val historyContainer = container { }.position(w - 24.0, bottomY)
        val controlsContainer = container { }
        val hintContainer = container { }.position(w / 2.0, boardY + BoardSpec.TOTAL + 34.0)
        val overlayContainer = container { }
        val btnY = h - 50.0
        val btnW = (w - 24.0 * 2.0 - 10.0) / 2.0

        // ── Логика. Чтобы избежать forward-reference проблемы локальных
        // функций, держим колбэки в `var`-переменных. ────────────────────
        var aiThinking = false
        var pendingMove: Pair<Int, Int>? = null

        var renderUi: () -> Unit = {}
        var undoAndRefresh: () -> Unit = {}
        var newGame: () -> Unit = {}
        var applyMoveRef: (Int, Int) -> Boolean = { _, _ -> false }

        fun isHumanTurn(): Boolean =
            game.gameState == GameState.PLAYING &&
                (mode == GameMode.PVP || game.currentPlayer == humanColor) &&
                !aiThinking

        renderUi = {
            pendingMove = null
            boardView.redraw() // сбрасывает и ghost, и подсказки не переживают ход
            renderTurnIndicator(turnIndicator, theme, aiThinking)
            moveCountText.text = Str.GAME_MOVE_PREFIX + game.getMoveCount()
            renderHistoryDots(historyContainer, theme)
            renderHintButton(hintContainer, theme, boardView, canHint = isHumanTurn())
            renderControls(
                controlsContainer, theme, btnY, btnW, game, busy = aiThinking,
                onUndo = { undoAndRefresh() }, onNew = { newGame() },
            )
        }

        applyMoveRef = lam@{ r, c ->
            val res = game.makeMove(r, c)
            if (res !is MoveResult.Success) return@lam false
            triggerHaptic(strong = res.winner != null)
            playStoneSound()
            renderUi()
            if (res.gameState != GameState.PLAYING) {
                launch {
                    delay(1200) // дать победной жиле «прорасти»
                    // Undo после победы мог вернуть партию — перепроверяем.
                    if (game === GameSession.game && game.gameState != GameState.PLAYING) {
                        showVictoryOverlay(overlayContainer, theme, res.winner, game.getMoveCount())
                    }
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
            // Пока AI «думал», партию могли сбросить/откатить (кнопки блокируются
            // через busy, но новая партия пересоздаёт сцену, а корутина живёт) —
            // перепроверяем, что ход всё ещё за AI.
            if (game !== GameSession.game || game.gameState != GameState.PLAYING ||
                game.currentPlayer != aiColor
            ) return
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
            // Режим подтверждения: первый тап — ghost-превью, повторный — фиксация.
            if (SettingsStore.current.confirmMoves) {
                if (pendingMove != r to c) {
                    if (game.getBoard().isEmpty(r, c)) {
                        pendingMove = r to c
                        boardView.showGhost(r, c, isBlack = game.currentPlayer == StoneColor.BLACK)
                        triggerHaptic()
                    }
                    return@handler
                }
                pendingMove = null
                boardView.clearGhost()
            }
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
    private var stoneSound: Sound? = null

    private fun playStoneSound() {
        if (!SettingsStore.current.sound) return
        stoneSound?.let { launch { it.play() } }
    }

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
            aiThinking -> Str.GAME_AI_THINKING to game.currentPlayer
            game.gameState == GameState.PLAYING -> when (game.currentPlayer) {
                StoneColor.BLACK -> Str.TURN_BLACK to StoneColor.BLACK
                StoneColor.WHITE -> Str.TURN_WHITE to StoneColor.WHITE
            }
            game.gameState == GameState.BLACK_WINS -> Str.BLACK_WINS to StoneColor.BLACK
            game.gameState == GameState.WHITE_WINS -> Str.WHITE_WINS to StoneColor.WHITE
            else -> Str.DRAW to StoneColor.BLACK
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

    // Подсказка по запросу: кнопка с лимитом на партию (вместо постоянной
    // подсветки, которая играла за игрока). Подсветка живёт до следующего
    // redraw (т.е. до ближайшего хода/отмены).
    private fun renderHintButton(
        host: Container, theme: KinPalette, boardView: KinBoardView, canHint: Boolean,
    ) {
        host.removeChildren()
        if (!SettingsStore.current.hints || GameSession.hintsLeft <= 0 || !canHint) return
        host.kinTextButton(
            "${Str.GAME_HINT} · ${GameSession.hintsLeft}",
            color = theme.gold,
        ) {
            if (GameSession.hintsLeft <= 0) return@kinTextButton
            GameSession.hintsLeft--
            boardView.showHints(topMoves(GameSession.game, count = 3).map { it.pos })
            renderHintButton(host, theme, boardView, canHint = true)
        }.apply {
            alignment = TextAlignment.MIDDLE_CENTER
        }
    }

    // Финал партии: оверлей поверх доски — победную линию видно, из оверлея
    // можно уйти «Посмотреть доску» (кнопки внизу продолжают работать,
    // включая отмену победного хода).
    private fun showVictoryOverlay(
        host: Container, theme: KinPalette, winner: StoneColor?, moves: Int,
    ) {
        host.removeChildren()
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        // Скрим гасит клики по доске.
        val scrim = host.solidRect(w, h, RGBA(theme.paper.r, theme.paper.g, theme.paper.b, 150))
        scrim.onClick { }

        val panelW = w - 48.0
        val panelH = 268.0
        val panel = host.container { }.position(24.0, (h - panelH) / 2.0 - 40.0)
        panel.solidRect(panelW, panelH, theme.surface)
        panel.solidRect(panelW, 1.0, theme.lineFirm)
        panel.solidRect(panelW, 1.0, theme.lineFirm) { y = panelH - 1.0 }
        panel.solidRect(1.0, panelH, theme.lineFirm)
        panel.solidRect(1.0, panelH, theme.lineFirm) { x = panelW - 1.0 }

        val cx = panelW / 2.0
        panel.kinText(Str.VICTORY_META, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, 28.0)
        }
        panel.kinText(
            when (winner) {
                StoneColor.BLACK -> Str.BLACK_WINS
                StoneColor.WHITE -> Str.WHITE_WINS
                null -> Str.DRAW
            },
            28.0, theme.ink, Fonts.serif,
        ) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, 52.0)
        }
        panel.kinText(Str.GAME_MOVE_PREFIX + moves, Type.meta, theme.muted) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, 92.0)
        }
        panel.container {
            kinSeam(
                x1 = 2.0, y1 = 8.0, x2 = panelW - 96.0, y2 = 6.0,
                jitter = 3.0, seed = 81, width = 1.4, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(48.0, 112.0)

        val btnW = panelW - 48.0
        panel.kinButton(
            width = btnW, label = Str.VICTORY_AGAIN, primary = true, centered = true,
            theme = theme,
            onPress = {
                GameSession.newGame(GameSession.mode)
                Nav.goGameKeepState()
            },
        ).position(24.0, 140.0)
        panel.kinButton(
            width = btnW, label = Str.VICTORY_TO_MENU, centered = true, theme = theme,
            onPress = { Nav.goMenu() },
        ).position(24.0, 140.0 + 52.0 + 10.0)

        host.kinTextButton(Str.VICTORY_VIEW_BOARD, color = theme.muted) {
            host.removeChildren()
        }.apply {
            alignment = TextAlignment.MIDDLE_CENTER
            position(w / 2.0, (h - panelH) / 2.0 - 40.0 + panelH + 28.0)
        }
    }

    private fun renderControls(
        host: Container, theme: KinPalette, btnY: Double, btnW: Double,
        game: GameLogic, busy: Boolean,
        onUndo: () -> Unit, onNew: () -> Unit,
    ) {
        host.removeChildren()
        host.kinButton(
            width = btnW, label = Str.GAME_UNDO, small = true, centered = true, theme = theme,
            // Как в дизайне: отмена доступна и после победы — undoMove
            // сбрасывает состояние в PLAYING, партию можно продолжить.
            // busy: undo во время хода AI мутировал бы Board, который
            // chooseMove читает на Dispatchers.Default (data race).
            enabled = game.getMoveCount() > 0 && !busy,
            onPress = onUndo,
        ).position(24.0, btnY)
        host.kinButton(
            width = btnW, label = Str.GAME_NEW, primary = true, small = true, centered = true,
            theme = theme,
            onPress = onNew,
        ).position(24.0 + btnW + 10.0, btnY)
    }
}
