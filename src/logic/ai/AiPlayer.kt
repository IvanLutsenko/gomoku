package logic.ai

import kotlinx.serialization.Serializable
import logic.*
import model.*
import kotlin.math.*
import kotlin.random.Random

@Serializable
enum class Difficulty(val label: String) {
    EASY("Легко"),
    MID("Средне"),
    HARD("Сложно");
}

interface AiPlayer {
    /** Выбрать ход. Гарантируется хотя бы одна свободная клетка. */
    fun chooseMove(game: GameLogic): Position
}

object AiFactory {
    fun create(d: Difficulty): AiPlayer = when (d) {
        Difficulty.EASY -> RandomNearAi()
        Difficulty.MID  -> HeuristicAi(neighborhood = 1, opponentBias = 0.9)
        Difficulty.HARD -> HeuristicAi(neighborhood = 2, opponentBias = 1.0)
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

// ── Single-ply эвристика с оценкой угроз. Достаточно сильна чтобы
// блокировать четвёрки и тройки, и сама строит атаки. Не считает
// «двойную тройку» по-настоящему но в большинстве позиций играет
// по-человечески разумно.
class HeuristicAi(
    private val neighborhood: Int = 1,
    private val opponentBias: Double = 0.9,
) : AiPlayer {

    override fun chooseMove(game: GameLogic): Position {
        val board = game.getBoard()
        val me = game.currentPlayer
        val opp = me.opposite

        if (board.moveCount == 0) return Position(7, 7)

        val candidates = candidateCells(board, radius = neighborhood)
        var best = candidates.first()
        var bestScore = Long.MIN_VALUE

        for (pos in candidates) {
            val s = scoreMove(board, pos.row, pos.col, me, opp, opponentBias)
            if (s > bestScore) {
                bestScore = s
                best = pos
            }
        }
        return best
    }
}

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
        attack += evalLine(board, r, c, dr, dc, me, hypothetical = true)
        defense += evalLine(board, r, c, dr, dc, opp, hypothetical = false)
    }
    val centerBonus = (7 - max(abs(r - 7), abs(c - 7))).toLong()
    // attack и defense уже сопоставимы по масштабу (evalLine возвращает
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

// Оценка фрагмента линии вдоль (dr,dc) при условии, что в (r,c) стоит
// `color` (для атаки) или там ставится оппонент чтобы блок (для защиты —
// в том же месте). Учитываем длину и количество открытых концов.
private fun evalLine(
    board: Board, r: Int, c: Int, dr: Int, dc: Int, color: StoneColor,
    hypothetical: Boolean,
): Long {
    var count = if (hypothetical) 1 else 0  // hypothetical: представляем камень `color` в (r,c)
    var openEnds = 0

    // Если не гипотетически — считаем угрозу, как если бы оппонент уже сходил
    // в (r,c). Это даёт correct «defense» — наш ход в (r,c) убивает всю эту
    // потенциальную угрозу.
    if (!hypothetical) count = 1

    var nr = r + dr; var nc = c + dc
    while (board.isValidPosition(nr, nc) && board.getStone(nr, nc) == color) {
        count++; nr += dr; nc += dc
    }
    if (board.isValidPosition(nr, nc) && board.isEmpty(nr, nc)) openEnds++

    nr = r - dr; nc = c - dc
    while (board.isValidPosition(nr, nc) && board.getStone(nr, nc) == color) {
        count++; nr -= dr; nc -= dc
    }
    if (board.isValidPosition(nr, nc) && board.isEmpty(nr, nc)) openEnds++

    return when {
        count >= 5 -> 10_000_000L      // выигрыш
        count == 4 && openEnds == 2 -> 1_000_000L  // open four
        count == 4 && openEnds == 1 -> 100_000L    // closed four
        count == 3 && openEnds == 2 -> 50_000L     // open three
        count == 3 && openEnds == 1 -> 1_000L
        count == 2 && openEnds == 2 -> 500L
        count == 2 && openEnds == 1 -> 50L
        count == 1 && openEnds == 2 -> 10L
        count == 1 && openEnds == 1 -> 1L
        else -> 0L
    }
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
