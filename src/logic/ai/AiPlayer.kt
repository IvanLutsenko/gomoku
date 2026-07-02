package logic.ai

import kotlinx.serialization.Serializable
import logic.*
import model.*
import kotlin.math.*
import kotlin.random.Random

@Serializable
enum class Difficulty {
    EASY,
    MID,
    HARD;
}

interface AiPlayer {
    /** Выбрать ход. Гарантируется хотя бы одна свободная клетка. */
    fun chooseMove(game: GameLogic): Position
}

object AiFactory {
    fun create(d: Difficulty): AiPlayer = when (d) {
        Difficulty.EASY -> RandomNearAi()
        Difficulty.MID  -> HeuristicAi(neighborhood = 1, opponentBias = 0.9)
        Difficulty.HARD -> HeuristicAi(neighborhood = 2, opponentBias = 1.0, depth = 2)
    }
}

// ── Random со смещением: первый ход в центр, дальше — случайная клетка
// в пределах двух от существующих камней. Не делает совсем тупых ходов
// «в углу пустой доски».
private class RandomNearAi : AiPlayer {
    override fun chooseMove(game: GameLogic): Position {
        val board = game.getBoard()
        if (board.moveCount == 0) return Position(7, 7)
        val candidates = candidateCells(board, radius = 2)
        return candidates.random(Random.Default)
    }
}

// ── Эвристика угроз на скользящих окнах (видит разрывные паттерны XX_XX)
// + опциональный 2-ply поиск: мой ход → лучший ответ соперника.
class HeuristicAi(
    private val neighborhood: Int = 1,
    private val opponentBias: Double = 0.9,
    private val depth: Int = 1,
) : AiPlayer {

    override fun chooseMove(game: GameLogic): Position {
        val board = game.getBoard()
        val me = game.currentPlayer
        val opp = me.opposite

        if (board.moveCount == 0) return Position(7, 7)

        val candidates = candidateCells(board, radius = neighborhood)
        val scored = candidates
            .map { it to scoreMove(board, it.row, it.col, me, opp, opponentBias) }
            .sortedByDescending { it.second }

        // Немедленный выигрыш/блок выигрыша: 2-ply не нужен.
        if (depth < 2 || scored.first().second >= WIN_SCORE) {
            return pickBest(scored)
        }

        // 2-ply: по топ-10 кандидатам симулируем свой ход и оцениваем лучший
        // ответ соперника (топ-8 его кандидатов). Полного минимакса нет —
        // на 15×15 этого хватает, чтобы не отдавать двухходовые ловушки.
        // ponytail: мутируем реальный Board через place/undo — UI на время
        // chooseMove заблокирован (aiThinking), гонок нет.
        val results = mutableListOf<Pair<Position, Long>>()
        for ((pos, myScore) in scored.take(10)) {
            if (!board.placeStone(pos.row, pos.col, me)) continue
            val oppBest = candidateCells(board, radius = 1)
                .map { scoreMove(board, it.row, it.col, opp, me, opponentBias) }
                .sortedDescending()
                .take(8)
                .maxOrNull() ?: 0L
            board.undoLastMove()
            results += pos to (myScore - (oppBest * 0.9).toLong())
        }
        return pickBest(results.sortedByDescending { it.second })
    }

    // Случайный tie-break среди равных по очкам: иначе партии детерминированы
    // и заучиваются.
    private fun pickBest(sorted: List<Pair<Position, Long>>): Position {
        val top = sorted.first().second
        return sorted.takeWhile { it.second == top }.random(Random.Default).first
    }
}

const val WIN_SCORE = 10_000_000L

// Считает очки гипотетического хода в (r,c) с точки зрения цвета `me`.
// Возвращает большие значения за создание собственных угроз и блокировку
// чужих. Чуть-чуть склоняется к центру, чтобы не зависать.
fun scoreMove(
    board: Board, r: Int, c: Int,
    me: StoneColor, opp: StoneColor,
    opponentBias: Double = 0.9,
): Long {
    val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
    var attack = 0L
    var defense = 0L
    for ((dr, dc) in dirs) {
        attack += evalWindows(board, r, c, dr, dc, me)
        defense += evalWindows(board, r, c, dr, dc, opp)
    }
    val centerBonus = (7 - max(abs(r - 7), abs(c - 7))).toLong()
    // attack и defense уже сопоставимы по масштабу (evalWindows возвращает
    // одни и те же диапазоны для обеих сторон). opponentBias < 1 чтобы
    // при равном выборе предпочитать собственную атаку защите.
    return attack + (defense * opponentBias).toLong() + centerBonus
}

// Поиск кандидатов: все пустые клетки в радиусе `radius` от занятых.
fun candidateCells(board: Board, radius: Int = 2): List<Position> {
    val out = mutableSetOf<Position>()
    val n = 15
    for (r in 0 until n) {
        for (c in 0 until n) {
            if (board.getStone(r, c) != null) {
                for (dr in -radius..radius) {
                    for (dc in -radius..radius) {
                        val nr = r + dr; val nc = c + dc
                        if (board.isEmpty(nr, nc)) out += Position(nr, nc)
                    }
                }
            }
        }
    }
    if (out.isEmpty()) out += Position(7, 7)
    return out.toList()
}

// Оценка вдоль (dr,dc) скользящими окнами длины 5, при условии что в (r,c)
// стоит камень `color` (гипотетически). Окно засчитывается, только если в нём
// нет камней противника — поэтому разрывные паттерны (XX_XX, X_XXX, _XX_X_)
// оцениваются естественно: чем больше своих камней в «живом» окне, тем дороже.
// Открытые фигуры автоматически дороже закрытых — они попадают в больше окон.
private fun evalWindows(
    board: Board, r: Int, c: Int, dr: Int, dc: Int, color: StoneColor,
): Long {
    var total = 0L
    for (offset in -4..0) {
        var mine = 0
        var blocked = false
        for (k in 0..4) {
            val rr = r + (offset + k) * dr
            val cc = c + (offset + k) * dc
            if (!board.isValidPosition(rr, cc)) { blocked = true; break }
            val s = if (rr == r && cc == c) color else board.getStone(rr, cc)
            when (s) {
                color -> mine++
                null -> {}
                else -> { blocked = true }
            }
            if (blocked) break
        }
        if (blocked) continue
        total += when (mine) {
            5 -> WIN_SCORE       // пять в окне (включая «через дырку» — ход в разрыв)
            4 -> 120_000L        // четвёрка: ход достраивает до пяти
            3 -> 5_000L
            2 -> 200L
            else -> 5L           // одиночный камень в живом окне
        }
    }
    return total
}

// ── Утилиты для подсказок (используются GameScene'ом) ────────────────────

data class HintMove(val pos: Position, val score: Long)

fun topMoves(game: GameLogic, count: Int = 3): List<HintMove> {
    val board = game.getBoard()
    val me = game.currentPlayer
    val opp = me.opposite
    val candidates = candidateCells(board, radius = 1)
    return candidates
        .map { HintMove(it, scoreMove(board, it.row, it.col, me, opp)) }
        .sortedByDescending { it.score }
        .take(count)
}
