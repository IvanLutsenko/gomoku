package ui

import korlibs.image.color.*
import korlibs.korge.view.*
import model.*

// Мини-доска — плоский снимок позиции (экран победы, миниатюры журнала).
// Камни — плоские круги без градиента, победные — с золотой обводкой.
fun Container.kinMiniBoard(
    moves: List<Move>,
    winLine: List<Position>?,
    size: Double,
    theme: KinPalette = Theme.colors,
    showSeam: Boolean = false,
): Container = container {
    val n = 15
    val cell = size / (n + 1)
    val pad = cell
    fun pos(i: Int) = pad + i * cell

    solidRect(size, size, theme.paperDeep)

    val gridAlpha = if (theme.isDark) 0.15 else 0.35
    val gridColor = RGBA(theme.ink.r, theme.ink.g, theme.ink.b, (255 * gridAlpha).toInt())
    val lineLen = cell * (n - 1)
    for (i in 0 until n) {
        solidRect(lineLen, 0.5, gridColor) { position(pad, pos(i)) }
        solidRect(0.5, lineLen, gridColor) { position(pos(i), pad) }
    }

    val blackFill = if (theme.isDark) Colors["#8f8578"] else Colors["#1a1814"]
    val whiteFill = Colors["#f8f4ea"]
    val whiteStroke = rgba255(0, 0, 0, 0.25)
    val isWin = { r: Int, c: Int -> winLine?.any { it.row == r && it.col == c } == true }

    val stoneR = cell * 0.44
    for (m in moves) {
        val win = isWin(m.row, m.col)
        val isBlack = m.player == StoneColor.BLACK
        circle(
            radius = stoneR,
            fill = if (isBlack) blackFill else whiteFill,
            stroke = when {
                win -> theme.gold
                !isBlack -> whiteStroke
                else -> Colors.TRANSPARENT
            },
            strokeThickness = if (win) 1.0 else if (!isBlack) 0.4 else 0.0,
        ).position(pos(m.col) - stoneR, pos(m.row) - stoneR)
    }

    if (showSeam && winLine != null && winLine.size >= 2) {
        val a = winLine.first()
        val b = winLine.last()
        kinSeam(
            x1 = pos(a.col), y1 = pos(a.row), x2 = pos(b.col), y2 = pos(b.row),
            jitter = 2.0, seed = 12, width = 1.2,
            color = theme.gold, colorSoft = theme.goldSoft,
        )
    }
}
