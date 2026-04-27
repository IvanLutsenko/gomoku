import scenes.GameMode
import scenes.GameSession
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class GameSessionTest {

    @AfterTest fun reset() {
        // Очистить state, чтобы порядок тестов не имел значения.
        GameSession.newGame(GameMode.PVP)
    }

    @Test fun newGame_creates_fresh_logic() {
        GameSession.newGame(GameMode.PVP)
        val first = GameSession.game
        first.makeMove(7, 7)
        assertEquals(1, first.getMoveCount())

        GameSession.newGame(GameMode.PVP)
        val second = GameSession.game
        assertNotSame(first, second)
        assertEquals(0, second.getMoveCount())
    }

    @Test fun newGame_sets_mode() {
        GameSession.newGame(GameMode.AI)
        assertEquals(GameMode.AI, GameSession.mode)
        GameSession.newGame(GameMode.PVP)
        assertEquals(GameMode.PVP, GameSession.mode)
    }

    @Test fun mode_changes_only_on_explicit_newGame() {
        GameSession.newGame(GameMode.AI)
        GameSession.game.makeMove(7, 7)
        // Простое продолжение партии не должно сбрасывать mode
        assertEquals(GameMode.AI, GameSession.mode)
    }

    @Test fun gameMode_enum_has_two_values() {
        assertEquals(2, GameMode.entries.size)
        assertTrue(GameMode.PVP in GameMode.entries)
        assertTrue(GameMode.AI in GameMode.entries)
    }
}
