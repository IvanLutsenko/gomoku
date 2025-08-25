package test

import logic.*
import model.*

fun main() {
    println("🧪 Тестирование Gomoku KorGE...")
    
    testBoard()
    testGameLogic()
    testWinConditions()
    
    println("✅ Все тесты пройдены! KorGE версия готова!")
}

fun testBoard() {
    println("\n📋 Тестируем Board:")
    
    val board = Board()
    
    // Тест размещения камней
    assert(board.placeStone(7, 7, StoneColor.BLACK)) { "Размещение камня" }
    assert(board.getStone(7, 7) == StoneColor.BLACK) { "Получение камня" }
    assert(!board.isEmpty(7, 7)) { "Клетка занята" }
    assert(!board.placeStone(7, 7, StoneColor.WHITE)) { "Повторное размещение запрещено" }
    
    // Тест отмены
    val undone = board.undoLastMove()
    assert(undone != null && undone.player == StoneColor.BLACK) { "Отмена хода" }
    assert(board.isEmpty(7, 7)) { "Клетка освобождена" }
    
    println("✅ Board работает корректно")
}

fun testGameLogic() {
    println("\n🎮 Тестируем GameLogic:")
    
    val game = GameLogic()
    
    // Тест базовых ходов
    assert(game.currentPlayer == StoneColor.BLACK) { "Чёрные начинают" }
    
    val result1 = game.makeMove(7, 7)
    assert(result1 is MoveResult.Success) { "Первый ход" }
    assert(game.currentPlayer == StoneColor.WHITE) { "Смена игрока" }
    
    // Тест невалидного хода
    val result2 = game.makeMove(7, 7)
    assert(result2 is MoveResult.InvalidMove) { "Повторный ход запрещён" }
    
    println("✅ GameLogic работает корректно")
}

fun testWinConditions() {
    println("\n🏆 Тестируем условия победы:")
    
    val game = GameLogic()
    
    // Горизонтальная победа
    for (i in 0 until 4) {
        game.makeMove(7, i)     // чёрный
        game.makeMove(8, i)     // белый
    }
    
    val winResult = game.makeMove(7, 4) // чёрный побеждает
    assert(winResult is MoveResult.Success && winResult.gameState == GameState.BLACK_WINS) { "Горизонтальная победа" }
    assert(winResult is MoveResult.Success && winResult.winningLine != null) { "Выигрышная линия найдена" }
    
    // Тест вертикальной победы
    val game2 = GameLogic()
    for (i in 0 until 4) {
        game2.makeMove(i, 7)     // чёрный
        game2.makeMove(i, 8)     // белый
    }
    
    val winResult2 = game2.makeMove(4, 7) // чёрный побеждает
    assert(winResult2 is MoveResult.Success && winResult2.gameState == GameState.BLACK_WINS) { "Вертикальная победа" }
    
    // Тест диагональной победы
    val game3 = GameLogic()
    for (i in 0 until 4) {
        game3.makeMove(i, i)         // чёрный
        game3.makeMove(i, i + 1)     // белый
    }
    
    val winResult3 = game3.makeMove(4, 4) // чёрный побеждает
    assert(winResult3 is MoveResult.Success && winResult3.gameState == GameState.BLACK_WINS) { "Диагональная победа" }
    
    println("✅ Все варианты победы работают")
}

fun assert(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw AssertionError("Тест провален: ${message()}")
    }
}
