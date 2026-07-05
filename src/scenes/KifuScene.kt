package scenes

import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.render.*
import korlibs.time.*
import kotlinx.coroutines.*
import logic.*
import model.*
import ui.*

// Кифу (棋譜) — запись партии: камни с номерами ходов в порядке постановки.
// Открывается с экранов победы/поражения и из журнала.
// «Поделиться» — текстовая нотация в буфер обмена; «Сохранить» — PNG-снимок
// доски (в ~/Downloads, если есть, иначе в папку данных приложения).
// ponytail: системный share-sheet на Android добавить, когда появится
// платформенный биндинг — сейчас общий для всех таргетов буфер обмена.
class KifuScene(
    private val record: GameRecord,
    private val onBack: () -> Unit,
) : Scene() {

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()

        kinPaperBackground(theme)
        onBackOrEscape { onBack() }

        kinTextButton(Str.BACK, color = theme.muted) { onBack() }.apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 22.0)
        }

        var y = 56.0
        kinText(Str.KIFU_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0
        container {
            kinSeam(
                x1 = 2.0, y1 = 6.0, x2 = 94.0, y2 = 6.0,
                jitter = 2.5, seed = 23, width = 1.3,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y)
        y += 18.0

        val resultLabel = when (record.result) {
            GameState.BLACK_WINS -> Str.BLACK_WINS
            GameState.WHITE_WINS -> Str.WHITE_WINS
            GameState.DRAW -> Str.DRAW
            GameState.PLAYING -> Str.KIFU_UNFINISHED
        }
        kinText(
            capsTracked("$resultLabel · ${Str.KIFU_MOVES_PREFIX}${record.moves.size}", 0),
            Type.labelCaps.size, theme.muted, Fonts.uiMedium,
        ) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 34.0

        val board = renderKifuBoard(this, (w - BoardSpec.TOTAL) / 2.0, y, theme)
        y += BoardSpec.TOTAL + 18.0

        // Статусная строка экспорта (появляется после действия)
        val status = kinText("", Type.caption.size, theme.muted, Fonts.ui) {
            alignment = TextAlignment.TOP_CENTER
            position(w / 2.0, y + 44.0 + 12.0)
        }

        val btnW = (w - 24.0 * 2.0 - 10.0) / 2.0
        kinButton(width = btnW, label = Str.KIFU_SHARE, small = true, centered = true, theme = theme) {
            launch {
                try {
                    views.gameWindow.clipboardWrite(TextClipboardData(notation()))
                    status.text = Str.KIFU_COPIED
                } catch (e: Throwable) {
                    status.text = Str.KIFU_EXPORT_FAILED
                }
            }
        }.position(24.0, y)
        kinButton(width = btnW, label = Str.KIFU_SAVE, primary = true, small = true, centered = true, theme = theme) {
            launch {
                try {
                    val bmp = board.renderToBitmap(views)
                    val home = Environment["HOME"]
                    val downloads = home?.let { localVfs("$it/Downloads") }
                    val dir = if (downloads != null && downloads.exists()) downloads
                    else localVfs(StandardPaths.appPreferencesFolder("gomoku"))
                    val file = dir["gomoku-kifu-${record.endedAt}.png"]
                    file.writeBitmap(bmp, PNG)
                    status.text = Str.KIFU_SAVED_PREFIX + file.absolutePath
                } catch (e: Throwable) {
                    status.text = Str.KIFU_EXPORT_FAILED
                }
            }
        }.position(24.0 + btnW + 10.0, y)
    }

    // Текстовая нотация: результат + ходы «1.●H8 2.○I9 …» (A–O × 1–15).
    private fun notation(): String {
        val header = when (record.result) {
            GameState.BLACK_WINS -> Str.BLACK_WINS
            GameState.WHITE_WINS -> Str.WHITE_WINS
            GameState.DRAW -> Str.DRAW
            GameState.PLAYING -> Str.KIFU_UNFINISHED
        }
        val moves = record.moves.joinToString(" ") {
            "${it.moveNumber}.${it.player.symbol}${it.notation}"
        }
        return "$header · ${record.moves.size} ${Str.movesWord(record.moves.size)}\n$moves"
    }

    private fun renderKifuBoard(host: Container, x: Double, y: Double, theme: KinPalette): Container {
        val board = host.container { }.position(x, y)
        val cell = BoardSpec.CELL
        val pad = BoardSpec.PADDING
        val total = BoardSpec.TOTAL
        fun pos(i: Int) = pad + i * cell

        board.solidRect(total, total, theme.paperDeep)

        val gridAlpha = if (theme.isDark) 0.18 else 0.5
        val gridColor = RGBA(theme.ink.r, theme.ink.g, theme.ink.b, (255 * gridAlpha).toInt())
        val lineLen = cell * (BoardSpec.SIZE_CELLS - 1)
        for (i in 0 until BoardSpec.SIZE_CELLS) {
            board.solidRect(lineLen, 0.5, gridColor) { position(pad, pos(i)) }
            board.solidRect(0.5, lineLen, gridColor) { position(pos(i), pad) }
        }
        for ((r, c) in BoardSpec.HOSHI) {
            board.circle(1.6, RGBA(theme.ink.r, theme.ink.g, theme.ink.b, (255 * (if (theme.isDark) 0.4 else 0.8)).toInt())) {
                position(pos(c) - 1.6, pos(r) - 1.6)
            }
        }

        val blackFill = if (theme.isDark) Colors["#8f8578"] else Colors["#1a1814"]
        val whiteFill = Colors["#f8f4ea"]
        val blackNum = if (theme.isDark) Colors["#14130f"] else Colors["#f5f0e6"]
        val whiteNum = Colors["#1a1814"]
        val win = record.winLine.orEmpty()
        val stoneR = 9.2

        record.moves.forEachIndexed { i, m ->
            val isBlack = m.player == StoneColor.BLACK
            val isWin = win.any { it.row == m.row && it.col == m.col }
            board.circle(
                radius = stoneR,
                fill = if (isBlack) blackFill else whiteFill,
                stroke = if (isWin) theme.gold else if (!isBlack) rgba255(0, 0, 0, 0.25) else Colors.TRANSPARENT,
                strokeThickness = if (isWin) 1.5 else if (!isBlack) 0.5 else 0.0,
            ).position(pos(m.col) - stoneR, pos(m.row) - stoneR)
            board.kinText("${i + 1}", 8.5, if (isBlack) blackNum else whiteNum, Fonts.uiSemiBold) {
                alignment = TextAlignment.MIDDLE_CENTER
                position(pos(m.col), pos(m.row))
            }
        }
        return board
    }
}
