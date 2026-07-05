package scenes

import korlibs.audio.sound.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.service.vibration.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlinx.coroutines.*
import logic.*
import logic.ai.*
import model.*
import ui.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

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

        kinButton(
            width = 44.0, label = if (theme.isDark) "☾" else "☀",
            small = true, centered = true, theme = theme,
            onPress = { Theme.toggle(); Nav.goGameKeepState() },
        ).position(w - menuBtnW - 24.0 - 44.0 - 8.0, 20.0)

        // ── Layout ──────────────────────────────────────────────────────
        // Чипы соперников + акай ито (красная нить) между ними
        val chipsContainer = container { }.apply { position(w / 2.0, 84.0) }
        kinAkaiItoH(230.0, theme).position(w / 2.0 - 115.0, 100.0)

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

        // 思考中 — татэгаки у правого края доски, пока AI думает
        val thinkingLabel = container {
            Str.THINKING_VERTICAL.forEachIndexed { i, ch ->
                kinText(ch.toString(), 9.0, theme.muted, Fonts.serifJp) {
                    alignment = TextAlignment.TOP_CENTER
                    position(0.0, i * 15.0)
                }
            }
            alpha = 0.45
            visible = false
        }.position(boardX + BoardSpec.TOTAL - 8.0, boardY + 8.0)

        // Редкий лепесток сакуры — падает над доской при бездействии («Дыхание»).
        val petalLayer = container { }
        launch {
            while (true) {
                delay(9_000L + Random.nextLong(8_000L))
                if (game.gameState == GameState.PLAYING) {
                    spawnPetal(petalLayer, boardX, boardY)
                }
            }
        }

        val bottomY = h - 78.0
        val moveCountText = kinText(
            Str.GAME_MOVE_PREFIX + game.getMoveCount(), Type.meta, theme.muted,
        ).apply {
            alignment = TextAlignment.MIDDLE_LEFT
            position(24.0, bottomY)
        }
        kinText(Str.GAME_15X15, Type.meta, theme.muted) {
            alignment = TextAlignment.MIDDLE_RIGHT
            position(w - 24.0, bottomY)
        }
        val controlsContainer = container { }
        val hintContainer = container { }.position(w / 2.0, boardY + BoardSpec.TOTAL + 34.0)
        val btnY = h - 50.0

        // ── Логика. Чтобы избежать forward-reference проблемы локальных
        // функций, держим колбэки в `var`-переменных. ────────────────────
        var aiThinking = false
        var pendingMove: Pair<Int, Int>? = null

        var renderUi: () -> Unit = {}
        var renderControlsRef: () -> Unit = {}
        var undoAndRefresh: () -> Unit = {}
        var newGame: () -> Unit = {}
        var applyMoveRef: (Int, Int) -> Boolean = { _, _ -> false }

        fun isHumanTurn(): Boolean =
            game.gameState == GameState.PLAYING &&
                (mode == GameMode.PVP || game.currentPlayer == humanColor) &&
                !aiThinking

        renderControlsRef = {
            renderControls(
                controlsContainer, theme, btnY, game,
                busy = aiThinking, pending = pendingMove,
                onUndo = { undoAndRefresh() }, onNew = { newGame() },
                onCancel = {
                    pendingMove = null
                    boardView.clearGhost()
                    renderControlsRef()
                },
                onConfirm = {
                    val p = pendingMove
                    if (p != null) {
                        pendingMove = null
                        boardView.clearGhost()
                        if (applyMoveRef(p.first, p.second) &&
                            mode == GameMode.AI && game.gameState == GameState.PLAYING
                        ) {
                            launch { aiTurnRef() }
                        }
                    }
                },
            )
        }

        renderUi = {
            pendingMove = null
            boardView.redraw() // сбрасывает и ghost, и подсказки не переживают ход
            renderChips(chipsContainer, theme, aiThinking)
            thinkingLabel.visible = aiThinking
            moveCountText.text = Str.GAME_MOVE_PREFIX + game.getMoveCount()
            renderHintButton(hintContainer, theme, boardView, canHint = isHumanTurn())
            renderControlsRef()
        }

        applyMoveRef = lam@{ r, c ->
            when (val res = game.makeMove(r, c)) {
                is MoveResult.Success -> {
                    triggerHaptic(strong = res.winner != null)
                    playStoneSound()
                    renderUi()
                    if (res.gameState != GameState.PLAYING) {
                        GameSession.recordFinished()
                        launch {
                            delay(1200) // дать победной жиле «прорасти»
                            if (game === GameSession.game && game.gameState != GameState.PLAYING) {
                                val lost = mode == GameMode.AI &&
                                    res.winner != null && res.winner != humanColor
                                if (lost) Nav.goDefeat() else Nav.goVictory()
                            }
                        }
                    }
                    true
                }
                is MoveResult.Forbidden -> {
                    // Рэндзю: запрещённая точка — киноварь + дрожание доски
                    boardView.flashForbidden(r, c)
                    shake(boardView, boardX)
                    triggerHaptic()
                    showForbiddenReason(hintContainer, theme, res.violation)
                    false
                }
                is MoveResult.InvalidMove -> {
                    shake(boardView, boardX)
                    false
                }
                else -> false
            }
        }

        undoAndRefresh = {
            if (game.getMoveCount() > 0) {
                GameSession.undoUsed = true
                // Отмена на завершённой партии возвращает её в PLAYING
                // («Посмотреть доску» на экране финала → отмотать → доиграть).
                if (game.gameState != GameState.PLAYING) GameSession.reopen()
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
            // Режим подтверждения: тап — ghost-превью + панель «Поставить камень»,
            // повторный тап той же клетки — фиксация.
            if (SettingsStore.current.confirmMoves) {
                if (pendingMove != r to c) {
                    if (game.getBoard().isEmpty(r, c)) {
                        pendingMove = r to c
                        boardView.showGhost(r, c, isBlack = game.currentPlayer == StoneColor.BLACK)
                        triggerHaptic()
                        renderControlsRef()
                    } else {
                        shake(boardView, boardX)
                    }
                    return@handler
                }
                pendingMove = null
                boardView.clearGhost()
            }
            if (!applyMoveRef(r, c)) return@handler
            if (mode == GameMode.AI && game.gameState == GameState.PLAYING) {
                launch { aiTurnRef() }
            }
        }

        aiTurnRef = turn@{
            if (mode != GameMode.AI || game.gameState != GameState.PLAYING || game.currentPlayer != aiColor) return@turn
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
            ) return@turn
            applyMoveRef(pos.row, pos.col)
        }

        renderUi()

        if (mode == GameMode.AI && game.currentPlayer == aiColor && game.gameState == GameState.PLAYING) {
            launch { aiTurnRef() }
        }
    }

    // aiTurn как var, чтобы ссылаться из renderControls (forward reference).
    private var aiTurnRef: suspend () -> Unit = {}

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

    // Лепесток: 11×9 розовый овал, падает ~6.5 с с покачиванием и вращением.
    private fun spawnPetal(host: Container, boardX: Double, boardY: Double) {
        val x0 = boardX + 20.0 + Random.nextDouble() * (BoardSpec.TOTAL - 40.0)
        val y0 = boardY - 6.0
        val img = host.image(petalBitmap).apply {
            anchor(0.5, 0.5)
            scaleY = 0.82
            position(x0, y0)
            alpha = 0.0
        }
        var t = 0.0
        var upd: Cancellable? = null
        upd = img.addUpdater { dt ->
            t += dt.milliseconds / 6500.0
            if (t >= 1.0) {
                img.removeFromParent()
                upd?.cancel()
                return@addUpdater
            }
            img.y = y0 + t * 340.0
            img.x = x0 + sin(t * PI * 2.6) * 13.0
            img.rotation = (t * 240.0).degrees
            img.alpha = when {
                t < 0.08 -> t / 0.08 * 0.8
                t > 0.85 -> (1.0 - t) / 0.15 * 0.8
                else -> 0.8
            }
        }
    }

    // Дрожание доски при недопустимом/запрещённом ходе.
    private fun shake(view: View, baseX: Double) {
        var t = 0.0
        var upd: Cancellable? = null
        upd = view.addUpdater { dt ->
            t += dt.milliseconds / 320.0
            if (t >= 1.0) {
                view.x = baseX
                upd?.cancel()
            } else {
                view.x = baseX + sin(t * PI * 4) * 3.0 * (1.0 - t)
            }
        }
    }

    // ── Чипы соперников: камень + имя (+★ победителю) + подпись, между ними vs.
    private fun renderChips(host: Container, theme: KinPalette, aiThinking: Boolean) {
        host.removeChildren()
        val game = GameSession.game
        val mode = GameSession.mode
        val gameOver = game.gameState != GameState.PLAYING

        val subB: String
        val subW: String
        if (mode == GameMode.AI) {
            subB = if (humanColor == StoneColor.BLACK) Str.CHIP_YOU else Str.CHIP_AI
            subW = if (humanColor == StoneColor.WHITE) Str.CHIP_YOU else Str.CHIP_AI
        } else {
            subB = Str.CHIP_P1
            subW = Str.CHIP_P2
        }

        val vs = host.kinText(Str.GAME_VS, Type.captionItalic.size, theme.muted, Fonts.serifItalic) {
            alignment = TextAlignment.MIDDLE_CENTER
            position(0.0, 10.0)
        }

        fun chip(color: StoneColor, label: String, sub: String, alignRight: Boolean) {
            val active = !gameOver && game.currentPlayer == color
            val won = game.winner == color
            val c = host.container { }
            val isBlack = color == StoneColor.BLACK
            val stoneD = 14.0
            c.kinStone(isBlack = isBlack, radius = stoneD / 2.0, theme = theme)
                .position(stoneD / 2.0, 10.0)
            val labelText = c.kinText(label, 13.0, theme.ink, Fonts.uiSemiBold) {
                alignment = TextAlignment.MIDDLE_LEFT
                position(stoneD + 8.0, 4.0)
            }
            var rowW = stoneD + 8.0 + labelText.scaledWidth
            if (won) {
                c.kinText("★", 13.0, theme.gold, Fonts.uiSemiBold) {
                    alignment = TextAlignment.MIDDLE_LEFT
                    position(rowW + 5.0, 4.0)
                }
                rowW += 5.0 + 14.0
            }
            val subText = c.kinText(capsTracked(sub, 1), 10.0, theme.muted, Fonts.uiMedium) {
                alignment = TextAlignment.MIDDLE_LEFT
                position(stoneD + 8.0, 19.0)
            }
            // Точки «AI думает» — после подписи чипа AI
            val isAiChip = mode == GameMode.AI && color != humanColor
            if (isAiChip && aiThinking) {
                val dotsX = stoneD + 8.0 + subText.scaledWidth + 6.0
                for (i in 0..2) {
                    val dot = c.circle(1.5, theme.ink) { position(dotsX + i * 6.0, 18.0) }
                    var t = i * -0.2
                    dot.addUpdater { dt ->
                        t += dt.milliseconds / 1300.0
                        val phase = ((t % 1.0) + 1.0) % 1.0
                        dot.alpha = 0.15 + 0.7 * (if (phase < 0.3) sin(phase / 0.3 * PI) else 0.0)
                    }
                }
            }
            c.alpha = if (active || won) 1.0 else 0.45
            c.position(if (alignRight) -20.0 - rowW else 20.0, 0.0)
        }

        chip(StoneColor.BLACK, Str.CHIP_BLACK, subB, alignRight = true)
        chip(StoneColor.WHITE, Str.CHIP_WHITE, subW, alignRight = false)
        vs.position(0.0, 10.0)
    }

    // Подсказка по запросу: кнопка с лимитом на партию.
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

    // Причина запрета рэндзю — киноварная строка под доской, гаснет сама.
    private fun showForbiddenReason(host: Container, theme: KinPalette, violation: RenjuViolation) {
        host.removeChildren()
        val label = when (violation) {
            RenjuViolation.OVERLINE -> Str.FORBIDDEN_OVERLINE
            RenjuViolation.DOUBLE_FOUR -> Str.FORBIDDEN_DOUBLE_FOUR
            RenjuViolation.DOUBLE_THREE -> Str.FORBIDDEN_DOUBLE_THREE
        }
        val txt = host.kinText(label, Type.caption.size, theme.vermillion, Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_CENTER
        }
        var t = 0.0
        var upd: Cancellable? = null
        upd = txt.addUpdater { dt ->
            t += dt.milliseconds / 1600.0
            if (t >= 1.0) {
                txt.removeFromParent()
                upd?.cancel()
            } else if (t > 0.7) {
                txt.alpha = 1.0 - (t - 0.7) / 0.3
            }
        }
    }

    private companion object {
        // Розовый градиентный овал — один битмап на все лепестки.
        val petalBitmap: Bitmap by lazy {
            Bitmap32Context2d(12, 12, true) {
                fill(
                    createRadialGradient(4.2, 4.2, 0.0, 6.0, 6.0, 5.6).also {
                        it.addColorStop(0.0, Colors["#f2c7cb"])
                        it.addColorStop(1.0, Colors["#dfa0aa"])
                    },
                ) { circle(Point(6.0, 6.0), 5.5) }
            }
        }
    }

    private fun renderControls(
        host: Container, theme: KinPalette, btnY: Double,
        game: GameLogic, busy: Boolean, pending: Pair<Int, Int>?,
        onUndo: () -> Unit, onNew: () -> Unit,
        onCancel: () -> Unit, onConfirm: () -> Unit,
    ) {
        host.removeChildren()
        val w = Viewport.W.toDouble()

        if (pending != null) {
            // Подтверждение хода: координата · ✕ · «Поставить камень»
            val (r, c) = pending
            val coord = "${('A' + c)}${15 - r}"
            host.kinText(coord, 24.0, theme.ink, Fonts.serif) {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, btnY - 4.0)
            }
            host.kinText(
                capsTracked(Str.GAME_MOVE_PREFIX + (game.getMoveCount() + 1), 0),
                10.0, theme.muted, Fonts.uiMedium,
            ) {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, btnY + 26.0)
            }
            host.kinButton(
                width = 44.0, label = Str.CONFIRM_CANCEL, small = true, centered = true,
                theme = theme, onPress = onCancel,
            ).position(84.0, btnY)
            host.kinButton(
                width = w - 140.0 - 24.0, label = Str.CONFIRM_PLACE,
                primary = true, small = true, centered = true,
                theme = theme, onPress = onConfirm,
            ).position(140.0, btnY)
            return
        }

        val btnW = (w - 24.0 * 2.0 - 10.0) / 2.0
        host.kinButton(
            width = btnW, label = Str.GAME_UNDO, small = true, centered = true, theme = theme,
            // busy: undo во время хода AI мутировал бы Board, который
            // chooseMove читает на Dispatchers.Default (data race).
            // Отмена работает и после финала — партия возвращается в PLAYING.
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
