package ui

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.io.lang.*
import korlibs.time.*
import logic.*
import model.*
import kotlin.math.*

class KinBoardView(
    private val game: GameLogic,
    private val theme: KinPalette,
    onCellClick: (Int, Int) -> Unit,
) : Container() {

    var onCellClickHandler: (Int, Int) -> Unit = onCellClick

    private val cell = BoardSpec.CELL
    private val pad = BoardSpec.PADDING
    private val total = BoardSpec.TOTAL

    private val stonesLayer: Container
    private val seamLayer: Container
    private val hintsLayer: Container
    private val ghostLayer: Container

    // Анимации: камень появляется только на новом ходе, жила «прорастает».
    private var lastAnimatedMoveCount = -1
    private var seamUpdater: Cancellable? = null
    private var seamAnimated = false

    init {
        // Фон
        solidRect(total, total, theme.paperDeep)

        // Кромка «керамики» из редизайна: светлый блик по верхней грани,
        // тонкая тень сразу под нижней.
        val topGlow = if (theme.isDark) rgba255(255, 253, 245, 0.04) else rgba255(255, 255, 255, 0.4)
        val dropShadow = if (theme.isDark) rgba255(0, 0, 0, 0.4) else rgba255(0, 0, 0, 0.05)
        solidRect(total, 1.0, topGlow)
        solidRect(total, 1.0, dropShadow) { y = total }

        drawGrid()

        stonesLayer = container { }
        seamLayer = container { }
        hintsLayer = container { }
        ghostLayer = container { }

        // Один большой hit-area поверх всех слоёв. На клик вычисляем
        // ближайшее пересечение — точное попадание не нужно, палец просто
        // обязан быть «где-то рядом» с нужной клеткой.
        solidRect(total, total, Colors.TRANSPARENT).onClick { e ->
            val pt = e.currentPosLocal
            val c = ((pt.x - pad) / cell).roundToInt()
            val r = ((pt.y - pad) / cell).roundToInt()
            if (r in 0 until BoardSpec.SIZE_CELLS && c in 0 until BoardSpec.SIZE_CELLS) {
                onCellClickHandler(r, c)
            }
        }

        redraw()
    }

    private fun drawGrid() {
        val gridAlpha = if (theme.isDark) 0.45 else 0.7
        val gridColor = RGBA(theme.ink.r, theme.ink.g, theme.ink.b, (255 * gridAlpha).toInt())
        val lineLen = cell * (BoardSpec.SIZE_CELLS - 1)

        for (i in 0 until BoardSpec.SIZE_CELLS) {
            solidRect(lineLen, BoardSpec.GRID_STROKE, gridColor) {
                position(pad, pad + i * cell - BoardSpec.GRID_STROKE / 2.0)
            }
            solidRect(BoardSpec.GRID_STROKE, lineLen, gridColor) {
                position(pad + i * cell - BoardSpec.GRID_STROKE / 2.0, pad)
            }
        }

        val hoshiColor = if (theme.isDark) RGBA(theme.ink.r, theme.ink.g, theme.ink.b, (255 * 0.8).toInt()) else theme.ink
        for ((r, c) in BoardSpec.HOSHI) {
            circle(BoardSpec.HOSHI_RADIUS, hoshiColor) {
                position(pad + c * cell, pad + r * cell)
            }
        }
    }

    fun redraw() {
        stonesLayer.removeChildren()
        seamUpdater?.cancel()
        seamUpdater = null
        seamLayer.removeChildren()
        ghostLayer.removeChildren()

        val board = game.getBoard()
        val lastMove = game.getMoveHistory().lastOrNull()
        val winPositions = game.winningLine?.positions
        val moveCount = game.getMoveCount()
        val animateLast = moveCount == lastAnimatedMoveCount + 1
        lastAnimatedMoveCount = moveCount

        for (r in 0 until BoardSpec.SIZE_CELLS) {
            for (c in 0 until BoardSpec.SIZE_CELLS) {
                val stone = board.getStone(r, c) ?: continue
                val isLast = lastMove?.let { it.row == r && it.col == c } == true
                val isWin = winPositions?.any { it.row == r && it.col == c } == true
                val view = stonesLayer.kinStone(
                    isBlack = stone == StoneColor.BLACK,
                    radius = cell * BoardSpec.STONE_RADIUS_RATIO,
                    isLast = isLast,
                    isWin = isWin,
                    theme = theme,
                ).position(pad + c * cell, pad + r * cell)
                if (isLast && animateLast) animateAppear(view)
            }
        }

        if (winPositions != null && winPositions.size >= 2) {
            val first = winPositions.first()
            val last = winPositions.last()
            val x1 = pad + first.col * cell
            val y1 = pad + first.row * cell
            val x2 = pad + last.col * cell
            val y2 = pad + last.row * cell
            fun drawSeam(progress: Double) {
                seamLayer.removeChildren()
                seamLayer.kinSeam(
                    x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                    jitter = 4.0, seed = 12, width = 2.4,
                    color = theme.gold, colorSoft = theme.goldSoft,
                    progress = progress,
                )
            }
            if (seamAnimated) {
                drawSeam(1.0)
            } else {
                // «Прорастание» золотой трещины по выигрышной линии, 600 мс.
                seamAnimated = true
                var t = 0.0
                seamUpdater = addUpdater { dt ->
                    t = min(1.0, t + dt.milliseconds / 600.0)
                    drawSeam(t)
                    if (t >= 1.0) {
                        seamUpdater?.cancel()
                        seamUpdater = null
                    }
                }
            }
        } else {
            seamAnimated = false
        }

        // Пульсирующий тэнгэн на пустой доске — приглашение к первому ходу.
        if (moveCount == 0) {
            val dot = stonesLayer.circle(4.0, theme.gold) {
                position(pad + 7 * cell - 4.0, pad + 7 * cell - 4.0)
            }
            var t = 0.0
            dot.addUpdater { dt ->
                t += dt.milliseconds / 1800.0
                val k = (sin(t * 2.0 * PI) + 1.0) / 2.0
                dot.alpha = 0.2 + 0.45 * k
                dot.scale(1.0 + 0.5 * k)
            }
        }
    }

    // Появление камня: scale 0.7→1.0 с лёгким overshoot за ~120 мс (ease-out-back).
    private fun animateAppear(view: View) {
        view.scale(0.7)
        var t = 0.0
        var upd: Cancellable? = null
        upd = view.addUpdater { dt ->
            t = min(1.0, t + dt.milliseconds / 120.0)
            val k = t - 1.0
            val ease = 1.0 + 2.70158 * k * k * k + 1.70158 * k * k
            view.scale(0.7 + 0.3 * ease)
            if (t >= 1.0) {
                view.scale(1.0)
                upd?.cancel()
            }
        }
    }

    // Ghost stone — полупрозрачное превью хода (режим подтверждения):
    // киноварное перекрестие через клетку + камень-призрак с кольцом.
    fun showGhost(r: Int, c: Int, isBlack: Boolean) {
        ghostLayer.removeChildren()
        val crossColor = RGBA(theme.vermillion.r, theme.vermillion.g, theme.vermillion.b, (255 * 0.22).toInt())
        val lineLen = cell * (BoardSpec.SIZE_CELLS - 1)
        ghostLayer.solidRect(1.0, lineLen, crossColor) { position(pad + c * cell, pad) }
        ghostLayer.solidRect(lineLen, 1.0, crossColor) { position(pad, pad + r * cell) }
        val stoneR = cell * BoardSpec.STONE_RADIUS_RATIO
        ghostLayer.kinStone(
            isBlack = isBlack,
            radius = stoneR,
            theme = theme,
        ).apply {
            position(pad + c * cell, pad + r * cell)
            alpha = 0.55
        }
        ghostLayer.circle(
            stoneR + 4.0, Colors.TRANSPARENT,
            stroke = theme.vermillion, strokeThickness = 1.0,
        ).apply {
            position(pad + c * cell - stoneR - 4.0, pad + r * cell - stoneR - 4.0)
            alpha = 0.9
        }
    }

    // Рэндзю: вспышка киноварного кольца на запрещённой точке.
    fun flashForbidden(r: Int, c: Int) {
        val ring = ghostLayer.circle(
            cell * 0.5, Colors.TRANSPARENT,
            stroke = theme.vermillion, strokeThickness = 1.5,
        ).apply {
            position(pad + c * cell - cell * 0.5, pad + r * cell - cell * 0.5)
        }
        var t = 0.0
        var upd: Cancellable? = null
        upd = ring.addUpdater { dt ->
            t += dt.milliseconds / 700.0
            if (t >= 1.0) {
                ring.removeFromParent()
                upd?.cancel()
            } else {
                ring.alpha = 1.0 - t * t
            }
        }
    }

    fun clearGhost() {
        ghostLayer.removeChildren()
    }

    fun showHints(positions: List<Position>) {
        hintsLayer.removeChildren()
        for (p in positions) {
            hintsLayer.circle(radius = 3.0, fill = theme.gold) {
                position(pad + p.col * cell, pad + p.row * cell)
                alpha = 0.7
            }
        }
    }

    fun clearHints() {
        hintsLayer.removeChildren()
    }
}
