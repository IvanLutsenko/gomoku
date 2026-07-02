package test

import logic.*
import model.*
import kotlin.test.*

// Логические тесты ядра (Board/GameLogic/победные линии).
// kotlin.test — гоняются в `./gradlew jvmTest` и в CI.
class GomokuLogicTest {

    @Test
    fun board() {
        val board = Board()

        assertTrue(board.placeStone(7, 7, StoneColor.BLACK), "Размещение камня")
        assertEquals(StoneColor.BLACK, board.getStone(7, 7), "Получение камня")
        assertFalse(board.isEmpty(7, 7), "Клетка занята")
        assertFalse(board.placeStone(7, 7, StoneColor.WHITE), "Повторное размещение запрещено")

        val undone = board.undoLastMove()
        assertNotNull(undone, "Отмена хода")
        assertEquals(StoneColor.BLACK, undone.player, "Отменён ход чёрных")
        assertTrue(board.isEmpty(7, 7), "Клетка освобождена")
    }

    @Test
    fun gameLogic() {
        val game = GameLogic()

        assertEquals(StoneColor.BLACK, game.currentPlayer, "Чёрные начинают")

        val result1 = game.makeMove(7, 7)
        assertIs<MoveResult.Success>(result1, "Первый ход")
        assertEquals(StoneColor.WHITE, game.currentPlayer, "Смена игрока")

        val result2 = game.makeMove(7, 7)
        assertIs<MoveResult.InvalidMove>(result2, "Повторный ход запрещён")
    }

    @Test
    fun horizontalWin() {
        val game = GameLogic()
        for (i in 0 until 4) {
            game.makeMove(7, i)     // чёрный
            game.makeMove(8, i)     // белый
        }
        val winResult = game.makeMove(7, 4)
        assertIs<MoveResult.Success>(winResult)
        assertEquals(GameState.BLACK_WINS, winResult.gameState, "Горизонтальная победа")
        assertNotNull(winResult.winningLine, "Выигрышная линия найдена")
    }

    @Test
    fun verticalWin() {
        val game = GameLogic()
        for (i in 0 until 4) {
            game.makeMove(i, 7)     // чёрный
            game.makeMove(i, 8)     // белый
        }
        val winResult = game.makeMove(4, 7)
        assertIs<MoveResult.Success>(winResult)
        assertEquals(GameState.BLACK_WINS, winResult.gameState, "Вертикальная победа")
    }

    @Test
    fun diagonalWin() {
        val game = GameLogic()
        for (i in 0 until 4) {
            game.makeMove(i, i)         // чёрный
            game.makeMove(i, i + 1)     // белый
        }
        val winResult = game.makeMove(4, 4)
        assertIs<MoveResult.Success>(winResult)
        assertEquals(GameState.BLACK_WINS, winResult.gameState, "Диагональная победа")
    }

    @Test
    fun undoAfterWinResumesGame() {
        val game = GameLogic()
        for (i in 0 until 4) {
            game.makeMove(7, i)
            game.makeMove(8, i)
        }
        game.makeMove(7, 4) // победа чёрных
        assertEquals(GameState.BLACK_WINS, game.gameState)

        game.undoMove()
        assertEquals(GameState.PLAYING, game.gameState, "Отмена победного хода возвращает партию")
        assertNull(game.winningLine, "Выигрышная линия сброшена")
        assertEquals(StoneColor.BLACK, game.currentPlayer, "Ход снова за чёрными")
    }
}
