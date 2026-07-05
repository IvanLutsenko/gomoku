package scenes

import korlibs.event.*
import korlibs.io.async.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.interpolation.*
import korlibs.time.*
import logic.*
import model.*

// Android Back/Esc обработчик с consume-ом события: KorgwActivity.onBackPressed
// диспатчит Key.BACK и смотрит KeyEvent.defaultPrevented. Если не вызвать
// `it.preventDefault()`, activity делает fallback (закрытие приложения).
fun View.onBackOrEscape(handler: () -> Unit) {
    keys.down {
        if (it.key == Key.BACK || it.key == Key.ESCAPE) {
            handler()
            it.preventDefault()
        }
    }
}

// Свой crossfade: видим оба кадра поверх друг друга, прозрачность плывёт
// от 1→0 и 0→1. KorGE имеет `crossfade`, но он @Deprecated в 6.x.
private val crossfade: Transition = Transition("crossfade") { _, prev, next, ratio ->
    val r = ratio.toFloat()
    prev.alphaF = 1f - r
    next.alphaF = r
}

enum class GameMode { PVP, AI }

// Глобальное состояние игры — переживает переключения сцен и темы.
object GameSession {
    const val HINTS_PER_GAME = 3

    var game: GameLogic = GameLogic()
        private set
    var mode: GameMode = GameMode.PVP
        private set

    // Цвет человека в AI-режиме. Начальное WHITE, чтобы первый ALTERNATE-флип
    // дал чёрных (первый ход) в первой партии.
    var humanColor: StoneColor = StoneColor.WHITE
        private set

    // Остаток подсказок на текущую партию (подсказки по запросу).
    var hintsLeft: Int = HINTS_PER_GAME

    // Была ли отмена в текущей партии (печать 心 «Без отмен»).
    var undoUsed: Boolean = false

    // Снимок завершённой партии + новые печати — для экранов победы/кифу.
    var lastRecord: GameRecord? = null
    var lastEarnedSeals: List<String> = emptyList()

    fun newGame(mode: GameMode) {
        game = GameLogic(renju = SettingsStore.current.rules == Rules.RENJU)
        this.mode = mode
        hintsLeft = HINTS_PER_GAME
        undoUsed = false
        lastRecord = null
        lastEarnedSeals = emptyList()
        if (mode == GameMode.AI) {
            humanColor = when (SettingsStore.current.playerColor) {
                PlayerColorPref.BLACK -> StoneColor.BLACK
                PlayerColorPref.WHITE -> StoneColor.WHITE
                PlayerColorPref.ALTERNATE -> humanColor.opposite
            }
        }
    }

    // Отмена хода на завершённой партии возвращает её в игру. Прежняя запись
    // остаётся в журнале, переигранная концовка станет новой записью.
    // ponytail: статистику/печати не откатываем — при переигровке партия
    // честно считается дважды; ревизия, если это начнёт мешать.
    fun reopen() {
        lastRecord = null
        lastEarnedSeals = emptyList()
    }

    // Партия закончилась: снимок в журнал, начисление печатей.
    fun recordFinished() {
        val g = game
        if (g.gameState == GameState.PLAYING || lastRecord != null) return
        val record = GameRecord(
            endedAt = korlibs.time.DateTime.now().unixMillisLong,
            mode = if (mode == GameMode.AI) "ai" else "pvp",
            difficulty = if (mode == GameMode.AI) SettingsStore.current.aiDifficulty else null,
            result = g.gameState,
            humanColor = if (mode == GameMode.AI) humanColor else null,
            moves = g.getMoveHistory(),
            winLine = g.winningLine?.positions,
        )
        lastRecord = record
        lastEarnedSeals = RecordsStore.onGameFinished(record, undoUsed)
    }
}

// Простейший роутинг между сценами с alpha-crossfade переходом.
// SceneContainer внедряется один раз в main(), затем сцены вызывают
// Nav.goXxx() синхронно.
object Nav {
    lateinit var container: SceneContainer

    private val FADE = 220.milliseconds

    // changeTo<T>() использует kotlin-reflect для конструирования сцены —
    // на Android рефлексия не подтянута, падает с NotMappedException.
    // Лямбда-форма обходит Injector и работает кросс-платформенно.
    fun goMenu() = launch { container.changeTo(time = FADE, transition = crossfade) { MenuScene() } }

    fun goGame(mode: GameMode) = launch {
        GameSession.newGame(mode)
        container.changeTo(time = FADE, transition = crossfade) { GameScene() }
    }

    fun goGameKeepState() = launch {
        container.changeTo(time = FADE, transition = crossfade) { GameScene() }
    }

    fun goSettings() = launch { container.changeTo(time = FADE, transition = crossfade) { SettingsScene() } }
    fun goHelp() = launch { container.changeTo(time = FADE, transition = crossfade) { HelpScene() } }
    fun goJournal() = launch { container.changeTo(time = FADE, transition = crossfade) { JournalScene() } }
    fun goSeals() = launch { container.changeTo(time = FADE, transition = crossfade) { SealsScene() } }
    fun goTea() = launch { container.changeTo(time = FADE, transition = crossfade) { TeaScene() } }
    fun goVictory() = launch { container.changeTo(time = FADE, transition = crossfade) { VictoryScene() } }
    fun goDefeat() = launch { container.changeTo(time = FADE, transition = crossfade) { DefeatScene() } }

    fun goKifu(record: logic.GameRecord, back: () -> Unit) = launch {
        container.changeTo(time = FADE, transition = crossfade) { KifuScene(record, back) }
    }

    private fun launch(block: suspend () -> Unit) {
        container.launchImmediately { block() }
    }
}
