package model

import kotlinx.serialization.Serializable

@Serializable
enum class StoneColor {
    BLACK, WHITE;
    
    val symbol: String get() = when (this) {
        BLACK -> "●"
        WHITE -> "○"
    }
    
    val opposite: StoneColor
        get() = when (this) {
        BLACK -> WHITE
        WHITE -> BLACK
    }
}

@Serializable
data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..14 && col in 0..14
    
    fun toNotation(): String {
        val colLetter = ('A' + col).toString()
        val rowNumber = row + 1
        return "$colLetter$rowNumber"
    }
    
    companion object {
        fun fromNotation(notation: String): Position? {
            if (notation.length < 2) return null
            
            val colLetter = notation[0].uppercaseChar()
            val rowNumber = notation.substring(1).toIntOrNull()
            
            if (colLetter !in 'A'..'O' || rowNumber == null || rowNumber !in 1..15) return null
            
            return Position(rowNumber - 1, colLetter - 'A')
        }
    }
}

@Serializable
data class Move(
    val row: Int,
    val col: Int,
    val player: StoneColor,
    val moveNumber: Int
) {
    val position: Position get() = Position(row, col)
    val notation: String get() = position.toNotation()
    
    override fun toString(): String = "${player.symbol} $notation (#$moveNumber)"
}

@Serializable
enum class GameState {
    PLAYING, BLACK_WINS, WHITE_WINS, DRAW
}

@Serializable
data class WinningLine(val positions: List<Position>)
