package ui

import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import logic.*
import model.*
import kotlin.math.roundToInt

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
        seamLayer.removeChildren()

        val board = game.getBoard()
        val lastMove = game.getMoveHistory().lastOrNull()
        val winPositions = game.winningLine?.positions

        for (r in 0 until BoardSpec.SIZE_CELLS) {
            for (c in 0 until BoardSpec.SIZE_CELLS) {
                val stone = board.getStone(r, c) ?: continue
                val isLast = lastMove?.let { it.row == r && it.col == c } == true
                val isWin = winPositions?.any { it.row == r && it.col == c } == true
                stonesLayer.kinStone(
                    isBlack = stone == StoneColor.BLACK,
                    radius = cell * BoardSpec.STONE_RADIUS_RATIO,
                    isLast = isLast,
                    isWin = isWin,
                    theme = theme,
                ).position(pad + c * cell, pad + r * cell)
            }
        }

        if (winPositions != null && winPositions.size >= 2) {
            val first = winPositions.first()
            val last = winPositions.last()
            seamLayer.kinSeam(
                x1 = pad + first.col * cell, y1 = pad + first.row * cell,
                x2 = pad + last.col * cell, y2 = pad + last.row * cell,
                jitter = 4.0, seed = 12, width = 2.4,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }
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
