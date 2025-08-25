import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.korge.input.*
import korlibs.korge.ui.*
import logic.*
import model.*

suspend fun main() = Korge(
    windowSize = Size(600, 740), 
    backgroundColor = Colors["#2b2b2b"],
    title = "Gomoku KorGE - Правильные камни"
) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { GomokuScene() }
}

class GomokuScene : Scene() {
    private val gameLogic = GameLogic()
    private lateinit var boardContainer: Container
    private lateinit var statusText: Text
    private lateinit var moveCountText: Text
    private val cellSize = 35.0
    private val boardOffset = Point(50, 140) // Хороший отступ
    
    private val cells = Array(15) { Array<SolidRect?>(15) { null } }
    private val stones = Array(15) { Array<Text?>(15) { null } }
    
    override suspend fun SContainer.sceneMain() {
        setupUI()
        setupBoard()
        updateDisplay()
    }
    
    private suspend fun SContainer.setupUI() {
        // Заголовок
        text("🎮 Gomoku", 32.0, Colors.WHITE) {
            x = 250.0
            y = 20.0
        }
        
        // Статус игры
        statusText = text("Ход чёрных ●", 20.0, Colors.WHITE) {
            x = 220.0
            y = 75.0 
        }
        
        // Счётчик ходов
        moveCountText = text("Ходы: 0", 16.0, Colors.LIGHTGRAY) {
            x = 50.0
            y = 690.0
        }
        
        setupControlButtons()
    }
    
    private suspend fun SContainer.setupControlButtons() {
        val buttonY = 690.0
        
        uiButton(size = Size(80, 30)) {
            text = "Новая игра"
            position(200, buttonY)
            onClick { newGame() }
        }
        
        uiButton(size = Size(80, 30)) {
            text = "Отменить"
            position(290, buttonY)
            onClick { undoMove() }
        }
        
        uiButton(size = Size(60, 30)) {
            text = "Выход"
            position(380, buttonY)
            onClick { gameWindow.close() }
        }
    }
    
    private suspend fun SContainer.setupBoard() {
        boardContainer = container {
            position(boardOffset)
            
            // Фон доски
            solidRect(
                width = cellSize * 15 + 20,
                height = cellSize * 15 + 20,
                color = Colors["#8B4513"]
            ) {
                position(-10, -10)
            }
            
            // Создание сетки
            for (row in 0 until 15) {
                for (col in 0 until 15) {
                    val cell = solidRect(
                        width = cellSize - 2,
                        height = cellSize - 2,
                        color = Colors["#DEB887"]
                    ) {
                        position(col * cellSize + 1, row * cellSize + 1)
                        
                        mouse {
                            onDown { makeMove(row, col) }
                            onOver {
                                if (gameLogic.canMakeMove(row, col)) {
                                    color = Colors["#F4E4BC"]
                                }
                            }
                            onOut {
                                if (stones[row][col] == null) {
                                    color = Colors["#DEB887"]
                                }
                            }
                        }
                    }
                    
                    cells[row][col] = cell
                }
            }
        }
    }
    
    private fun makeMove(row: Int, col: Int) {
        val result = gameLogic.makeMove(row, col)
        
        when (result) {
            is MoveResult.Success -> {
                updateDisplay()
                if (result.gameState != GameState.PLAYING) {
                    handleGameEnd(result)
                }
            }
            is MoveResult.InvalidMove -> {}
            is MoveResult.GameEnded -> {}
        }
    }
    
    private fun updateDisplay() {
        val board = gameLogic.getBoard()
        
        // Обновляем камни на поле
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                val stone = board.getStone(row, col)
                val existingStone = stones[row][col]
                
                if (stone != null && existingStone == null) {
                    // ПРАВИЛЬНЫЕ КАМНИ: круглые символы с контрастными цветами!
                    val stoneSymbol = if (stone == StoneColor.BLACK) "●" else "○"
                    
                    // Максимально контрастные цвета
                    val stoneColor = if (stone == StoneColor.BLACK) {
                        Colors["#000000"] // Абсолютно черный
                    } else {
                        Colors["#FFFFFF"] // Абсолютно белый
                    }
                    
                    // Для белых камней добавляем черную обводку
                    if (stone == StoneColor.WHITE) {
                        // Черная обводка под белым камнем
                        boardContainer.text("●", 36.0, Colors["#000000"]) {
                            position(
                                col * cellSize + cellSize / 2 - 18,
                                row * cellSize + cellSize / 2 - 18
                            )
                        }
                    }
                    
                    val stoneText = boardContainer.text(
                        stoneSymbol,
                        32.0,
                        stoneColor
                    ) {
                        position(
                            col * cellSize + cellSize / 2 - 16,
                            row * cellSize + cellSize / 2 - 16
                        )
                    }
                    stones[row][col] = stoneText
                    
                    // Обновляем цвет клетки
                    cells[row][col]?.color = Colors["#CD853F"]
                    
                } else if (stone == null && existingStone != null) {
                    // Убираем камень (отмена хода)
                    existingStone.removeFromParent()
                    stones[row][col] = null
                    cells[row][col]?.color = Colors["#DEB887"]
                }
            }
        }
        
        // Выделяем выигрышную линию
        gameLogic.winningLine?.positions?.forEach { pos ->
            cells[pos.row][pos.col]?.color = Colors.GOLD
        }
        
        // Обновляем текст статуса - используем правильные символы
        statusText.text = when (gameLogic.gameState) {
            GameState.PLAYING -> "Ход ${if (gameLogic.currentPlayer == StoneColor.BLACK) "чёрных ●" else "белых ○"}"
            GameState.BLACK_WINS -> "🎉 Победили чёрные ●!"
            GameState.WHITE_WINS -> "🎉 Победили белые ○!" 
            GameState.DRAW -> "🤝 Ничья!"
        }
        
        // Обновляем счётчик
        moveCountText.text = "Ходы: ${gameLogic.getMoveCount()}"
    }
    
    private fun handleGameEnd(result: MoveResult.Success) {
        println("Игра окончена: ${result.gameState}")
    }
    
    private fun newGame() {
        // Очищаем визуальные камни
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                stones[row][col]?.removeFromParent()
                stones[row][col] = null
                cells[row][col]?.color = Colors["#DEB887"]
            }
        }
        
        gameLogic.resetGame()
        updateDisplay()
    }
    
    private fun undoMove() {
        val result = gameLogic.undoMove()
        if (result is UndoResult.Success) {
            updateDisplay()
        }
    }
}
