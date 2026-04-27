import logic.GameLogic
import logic.ai.AiFactory
import logic.ai.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DifficultyTest {

    @Test fun all_difficulties_have_russian_label() {
        assertEquals("Легко", Difficulty.EASY.label)
        assertEquals("Средне", Difficulty.MID.label)
        assertEquals("Сложно", Difficulty.HARD.label)
    }

    @Test fun entries_returns_three_levels() {
        assertEquals(3, Difficulty.entries.size)
        assertTrue(Difficulty.EASY in Difficulty.entries)
        assertTrue(Difficulty.MID in Difficulty.entries)
        assertTrue(Difficulty.HARD in Difficulty.entries)
    }

    @Test fun factory_returns_non_null_for_each_difficulty() {
        for (d in Difficulty.entries) {
            assertNotNull(AiFactory.create(d))
        }
    }

    @Test fun factory_creates_ai_that_picks_legal_first_move() {
        val game = GameLogic()
        for (d in Difficulty.entries) {
            val ai = AiFactory.create(d)
            val pos = ai.chooseMove(game)
            assertTrue(pos.isValid(), "$d returned invalid pos $pos")
            assertTrue(game.getBoard().isEmpty(pos.row, pos.col))
        }
    }
}
