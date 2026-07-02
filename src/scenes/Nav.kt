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
    var game: GameLogic = GameLogic()
        private set
    var mode: GameMode = GameMode.PVP
        private set

    fun newGame(mode: GameMode) {
        game = GameLogic()
        this.mode = mode
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

    // winner == null → ничья.
    fun goVictory(winner: StoneColor?) = launch {
        currentVictoryWinner = winner
        container.changeTo(time = FADE, transition = crossfade) { VictoryScene() }
    }

    var currentVictoryWinner: StoneColor? = StoneColor.BLACK
        private set

    private fun launch(block: suspend () -> Unit) {
        container.launchImmediately { block() }
    }
}
