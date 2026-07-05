package logic

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import logic.ai.Difficulty
import kotlin.coroutines.EmptyCoroutineContext

// Цвет игрока в AI-режиме. ALTERNATE — чередование между партиями.
@Serializable
enum class PlayerColorPref { ALTERNATE, WHITE, BLACK }

// Правила: классика (любые пять и длиннее) или рэндзю (запреты для чёрных).
@Serializable
enum class Rules { CLASSIC, RENJU }

@Serializable
data class Settings(
    val dark: Boolean = false,
    val rules: Rules = Rules.CLASSIC,
    val aiDifficulty: Difficulty = Difficulty.MID,
    val hints: Boolean = false,
    val sound: Boolean = true,
    val music: Boolean = true,
    val confirmMoves: Boolean = false,
    val playerColor: PlayerColorPref = PlayerColorPref.ALTERNATE,
    val firstRun: Boolean = true,
    // Момент первого запуска (unix ms), ставится один раз в main.kt.
    // Правилами рекламы больше не используется, но дату установки задним
    // числом не восстановить — копим для будущей аналитики. 0 = не проставлен.
    val firstRunAt: Long = 0,
    // Покупка «Тишина» из Чайной — убирает рекламу навсегда.
    val adsRemoved: Boolean = false,
)

// In-memory + lazy persistence в applicationDataVfs. Save идёт фоновой
// корутиной — UI не ждёт. Load — на старте app в main.kt.
object SettingsStore {
    var current: Settings = Settings()
        private set

    private const val APP_ID = "gomoku"
    private const val FILE_NAME = "settings.json"

    private val json = Json {
        prettyPrint = true
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
            // первый запуск или нет доступа — остаёмся с дефолтами
        }
    }

    fun update(block: (Settings) -> Settings) {
        current = block(current)
        scheduleSave()
    }

    fun set(new: Settings) {
        current = new
        scheduleSave()
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
            // не критично — сохранимся в следующий раз
        }
    }
}
