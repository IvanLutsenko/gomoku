package com.gomoku.shared.model

import kotlinx.serialization.Serializable

/**
 * Игровое поле Gomoku 15x15
 */
@Serializable
class Board(private val size: Int = 15) {
    
    private val grid = Array(size) { Array<StoneColor?>(size) { null } }
    private val _moveHistory = mutableListOf<Move>()
    
    val moveHistory: List<Move> get() = _moveHistory.toList()
    val moveCount: Int get() = _moveHistory.size
    
    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until size && col in 0 until size
    }
    
    fun isEmpty(row: Int, col: Int): Boolean {
        return isValidPosition(row, col) && grid[row][col] == null
    }
    
    fun placeStone(row: Int, col: Int, player: StoneColor): Boolean {
        if (!isEmpty(row, col)) return false
        
        grid[row][col] = player
        _moveHistory.add(Move(row, col, player, _moveHistory.size + 1))
        return true
    }
    
    fun getStone(row: Int, col: Int): StoneColor? {
        if (!isValidPosition(row, col)) return null
        return grid[row][col]
    }
    
    fun clear() {
        for (row in 0 until size) {
            for (col in 0 until size) {
                grid[row][col] = null
            }
        }
        _moveHistory.clear()
    }
    
    fun undoLastMove(): Move? {
        if (_moveHistory.isEmpty()) return null
        
        val lastMove = _moveHistory.removeLast()
        grid[lastMove.row][lastMove.col] = null
        return lastMove
    }
    
    fun getGridCopy(): Array<Array<StoneColor?>> {
        return Array(size) { row ->
            Array(size) { col ->
                grid[row][col]
            }
        }
    }
    
    fun isFull(): Boolean = _moveHistory.size == size * size
    
    fun getEmptyPositions(): List<Position> {
        val positions = mutableListOf<Position>()
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (isEmpty(row, col)) {
                    positions.add(Position(row, col))
                }
            }
        }
        return positions
    }
}
