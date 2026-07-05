package logic

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.time.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import logic.ai.Difficulty
import model.*
import kotlin.coroutines.EmptyCoroutineContext

// Журнал партий + статистика + печати (достижения). Персист — records.json
// рядом с settings.json, тот же паттерн lazy-save.

@Serializable
data class GameRecord(
    val endedAt: Long,
    val mode: String, // "ai" | "pvp"
    val difficulty: Difficulty? = null,
    val result: GameState,
    val humanColor: StoneColor? = null, // цвет человека в AI-партии
    val moves: List<Move>,
    val winLine: List<Position>? = null,
)

@Serializable
data class PlayerStats(
    val games: Int = 0,
    val wins: Int = 0, // победы человека в AI-партиях
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val stonesTotal: Int = 0, // все поставленные камни (обе стороны)
    val pvpGames: Int = 0,
    val seals: Set<String> = emptySet(),
)

@Serializable
data class RecordsData(
    val stats: PlayerStats = PlayerStats(),
    val games: List<GameRecord> = emptyList(), // новые в начале
)

// Ключи печатей — иероглифы, порядок = сетка 3×3 экрана «Печати».
// 和 (ничья) — легенда вне коллекции, в сетку не входит.
object Seals {
    val COLLECTION = listOf("初", "連", "速", "心", "師", "白", "石", "月", "友")
    const val LEGEND = "和"
    const val MASTER = "極"

    fun isComplete(stats: PlayerStats): Boolean = stats.seals.containsAll(COLLECTION)
}

object RecordsStore {
    var current: RecordsData = RecordsData()
        private set

    private const val APP_ID = "gomoku"
    private const val FILE_NAME = "records.json"
    private const val MAX_GAMES = 50

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load() {
        try {
            val dir = StandardPaths.appPreferencesFolder(APP_ID)
            val file = localVfs(dir)[FILE_NAME]
            if (file.exists()) {
                current = json.decodeFromString(file.readString())
            }
        } catch (e: Throwable) {
            // первый запуск или битый файл — остаёмся с пустыми
        }
    }

    /**
     * Фиксирует завершённую партию: журнал + статистика + печати.
     * @return новые печати, заработанные этой партией (для оттиска на экране победы).
     */
    fun onGameFinished(record: GameRecord, undoUsed: Boolean): List<String> {
        val s = current.stats
        val humanWon = record.mode == "ai" && record.humanColor != null && when (record.result) {
            GameState.BLACK_WINS -> record.humanColor == StoneColor.BLACK
            GameState.WHITE_WINS -> record.humanColor == StoneColor.WHITE
            else -> false
        }
        val streak = when {
            record.mode != "ai" -> s.streak
            humanWon -> s.streak + 1
            else -> 0
        }
        val stones = s.stonesTotal + record.moves.size
        val pvp = s.pvpGames + if (record.mode == "pvp") 1 else 0
        val hour = DateTime.fromUnixMillis(record.endedAt).local.hours

        val earned = mutableSetOf<String>()
        fun award(key: String, cond: Boolean) {
            if (cond && key !in s.seals) earned += key
        }
        award("初", humanWon)
        award("連", streak >= 3)
        award("速", humanWon && record.moves.size <= 20)
        award("心", humanWon && !undoUsed)
        award("師", humanWon && record.difficulty == Difficulty.HARD)
        award("白", humanWon && record.humanColor == StoneColor.WHITE)
        award("石", stones >= 1000)
        award("月", hour in 0..3)
        award("友", pvp >= 10)
        award(Seals.LEGEND, record.result == GameState.DRAW)

        current = RecordsData(
            stats = s.copy(
                games = s.games + 1,
                wins = s.wins + if (humanWon) 1 else 0,
                streak = streak,
                bestStreak = maxOf(s.bestStreak, streak),
                stonesTotal = stones,
                pvpGames = pvp,
                seals = s.seals + earned,
            ),
            games = (listOf(record) + current.games).take(MAX_GAMES),
        )
        scheduleSave()
        // порядок коллекции, легенда последней
        return (Seals.COLLECTION + Seals.LEGEND).filter { it in earned }
    }

    private fun scheduleSave() {
        launchImmediately(EmptyCoroutineContext) { save() }
    }

    private suspend fun save() {
        try {
            val dir = StandardPaths.appPreferencesFolder(APP_ID)
            val file = localVfs(dir)[FILE_NAME]
            file.parent.mkdirs()
            file.writeString(json.encodeToString(current))
        } catch (e: Throwable) {
            // не критично
        }
    }
}
