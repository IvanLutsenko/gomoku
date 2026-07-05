import korlibs.korge.scene.*
import korlibs.korge.tests.*
import korlibs.time.*
import logic.*
import model.*
import scenes.*
import ui.*
import kotlin.test.*

// Smoke: новые сцены редизайна конструируются и рендерятся без падений.
// Рекорд подставляется напрямую (RecordsStore не трогаем, чтобы тесты
// не писали в реальный records.json пользователя).
class NewScenesSmokeTest : ViewsForTesting() {

    private fun sampleRecord(result: GameState): GameRecord {
        val moves = mutableListOf<Move>()
        // чёрные строят ряд (7,3..7,7), белые — сверху
        for (i in 0..3) {
            moves += Move(7, 3 + i, StoneColor.BLACK, moves.size + 1)
            moves += Move(0, i, StoneColor.WHITE, moves.size + 1)
        }
        moves += Move(7, 7, StoneColor.BLACK, moves.size + 1)
        return GameRecord(
            endedAt = DateTime.nowUnixMillisLong(),
            mode = "ai",
            difficulty = logic.ai.Difficulty.MID,
            result = result,
            humanColor = StoneColor.BLACK,
            moves = moves,
            winLine = (3..7).map { Position(7, it) },
        )
    }

    @Test
    fun endScreensAndCollectionsRender() = viewsTest {
        Fonts.loadOnce()
        GameSession.newGame(GameMode.AI)
        GameSession.lastRecord = sampleRecord(GameState.BLACK_WINS)
        GameSession.lastEarnedSeals = listOf("初")

        val sc = sceneContainer()
        Nav.container = sc

        sc.changeTo { VictoryScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { DefeatScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { KifuScene(GameSession.lastRecord!!) { } }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { JournalScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { SealsScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { MenuScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { SettingsScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { HelpScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { TeaScene() }
        assertTrue(sc.numChildren > 0)

        sc.changeTo { SplashScene() }
        assertTrue(sc.numChildren > 0)
    }

    @Test
    fun undoAfterFinalReopensGame() = viewsTest {
        GameSession.newGame(GameMode.PVP)
        val g = GameSession.game
        // чёрные выигрывают по (7,3..7,7)
        for (i in 0..3) {
            g.makeMove(7, 3 + i)
            g.makeMove(0, i)
        }
        g.makeMove(7, 7)
        assertEquals(GameState.BLACK_WINS, g.gameState)
        // recordFinished не зовём (писал бы в реальный records.json) —
        // снимок подставляем напрямую.
        GameSession.lastRecord = sampleRecord(GameState.BLACK_WINS)
        assertNotNull(GameSession.lastRecord)

        // отмена возвращает партию в игру, снимок финала сбрасывается
        GameSession.reopen()
        g.undoMove()
        assertEquals(GameState.PLAYING, g.gameState)
        assertNull(GameSession.lastRecord)
    }

    @Test
    fun gameSceneRendersWithRenju() = viewsTest {
        Fonts.loadOnce()
        SettingsStore.set(SettingsStore.current.copy(rules = Rules.RENJU))
        try {
            GameSession.newGame(GameMode.PVP)
            assertTrue(GameSession.game.renju)
            val sc = sceneContainer()
            Nav.container = sc
            sc.changeTo { GameScene() }
            assertTrue(sc.numChildren > 0)
        } finally {
            SettingsStore.set(SettingsStore.current.copy(rules = Rules.CLASSIC))
        }
    }
}
