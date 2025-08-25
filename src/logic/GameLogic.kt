package logic

import model.*

/**
 * Основная игровая логика Gomoku
 */
class GameLogic {
    private val board = Board()
    
    var currentPlayer = StoneColor.BLACK
        private set
        
    var gameState = GameState.PLAYING
        private set
        
    var winner: StoneColor? = null
        private set
        
    var winningLine: WinningLine? = null
        private set
    
    // Направления для проверки: горизонталь, вертикаль, диагонали
    private val directions = listOf(
        Pair(0, 1),   // горизонталь →
        Pair(1, 0),   // вертикаль ↓
        Pair(1, 1),   // диагональ ↘
        Pair(1, -1)   // диагональ ↙
    )
    
    fun makeMove(row: Int, col: Int): MoveResult {
        if (gameState != GameState.PLAYING) {
            return MoveResult.GameEnded("Игра уже завершена")
        }
        
        if (!board.isValidPosition(row, col)) {
            return MoveResult.InvalidMove("Неверные координаты")
        }
        
        if (!board.isEmpty(row, col)) {
            return MoveResult.InvalidMove("Клетка уже занята")
        }
        
        // Размещаем камень
        board.placeStone(row, col, currentPlayer)
        
        // Проверяем победу
        val winCheck = checkWin(row, col, currentPlayer)
        if (winCheck != null) {
            gameState = if (currentPlayer == StoneColor.BLACK) GameState.BLACK_WINS else GameState.WHITE_WINS
            winner = currentPlayer
            winningLine = winCheck
        } else if (board.isFull()) {
            gameState = GameState.DRAW
        }
        
        val movePlayer = currentPlayer
        if (gameState == GameState.PLAYING) {
            currentPlayer = currentPlayer.opposite
        }
        
        return MoveResult.Success(
            move = board.moveHistory.last(),
            gameState = gameState,
            winner = winner,
            winningLine = winningLine,
            nextPlayer = currentPlayer
        )
    }
    
    private fun checkWin(row: Int, col: Int, player: StoneColor): WinningLine? {
        for ((dRow, dCol) in directions) {
            val line = getLine(row, col, dRow, dCol, player)
            if (line.size >= 5) {
                return WinningLine(line)
            }
        }
        return null
    }
    
    private fun getLine(row: Int, col: Int, dRow: Int, dCol: Int, player: StoneColor): List<Position> {
        val line = mutableListOf<Position>()
        line.add(Position(row, col)) // Включаем начальную позицию
        
        // Проверяем в положительном направлении
        var r = row + dRow
        var c = col + dCol
        while (board.isValidPosition(r, c) && board.getStone(r, c) == player) {
            line.add(Position(r, c))
            r += dRow
            c += dCol
        }
        
        // Проверяем в отрицательном направлении
        r = row - dRow
        c = col - dCol
        while (board.isValidPosition(r, c) && board.getStone(r, c) == player) {
            line.add(0, Position(r, c)) // Добавляем в начало
            r -= dRow
            c -= dCol
        }
        
        return line
    }
    
    fun undoMove(): UndoResult {
        if (board.moveCount == 0) {
            return UndoResult.NoMovesToUndo
        }
        
        val undoneMove = board.undoLastMove()
        if (undoneMove != null) {
            // Восстанавливаем состояние игры
            gameState = GameState.PLAYING
            winner = null
            winningLine = null
            currentPlayer = undoneMove.player
            
            return UndoResult.Success(undoneMove)
        }
        
        return UndoResult.Error("Не удалось отменить ход")
    }
    
    fun resetGame() {
        board.clear()
        currentPlayer = StoneColor.BLACK
        gameState = GameState.PLAYING
        winner = null
        winningLine = null
    }
    
    fun getBoard(): Board = board
    fun getMoveCount(): Int = board.moveCount
    fun getMoveHistory(): List<Move> = board.moveHistory
    
    fun canMakeMove(row: Int, col: Int): Boolean {
        return gameState == GameState.PLAYING && board.isEmpty(row, col)
    }
}

sealed class MoveResult {
    data class Success(
        val move: Move,
        val gameState: GameState,
        val winner: StoneColor?,
        val winningLine: WinningLine?,
        val nextPlayer: StoneColor
    ) : MoveResult()
    
    data class InvalidMove(val reason: String) : MoveResult()
    data class GameEnded(val reason: String) : MoveResult()
}

sealed class UndoResult {
    data class Success(val undoneMove: Move) : UndoResult()
    object NoMovesToUndo : UndoResult()
    data class Error(val message: String) : UndoResult()
}
