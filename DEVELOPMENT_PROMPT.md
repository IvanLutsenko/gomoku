# 🎮 GOMOKU GAME DEVELOPMENT PROMPT

## 📋 PROJECT OVERVIEW

**Goal**: Create a fully functional Gomoku (Five in a Row) game using KorGE engine
**Target Platform**: Multiplatform (Desktop, Web, Mobile)
**Engine**: KorGE (Kotlin Multiplatform Game Engine)
**Project Path**: `/Users/lutse/KorgeProjects/Gomoku`

## 🎯 GAME SPECIFICATIONS

### Core Rules
- **Board Size**: 15x15 grid (standard Gomoku)
- **Objective**: First player to get 5 stones in a row (horizontal, vertical, or diagonal) wins
- **Players**: Human vs AI / Human vs Human modes
- **Stones**: Black stones (first player), White stones (second player)

### Technical Requirements
- **Resolution**: 512x512 window (current)
- **Input**: Mouse clicks for stone placement
- **Graphics**: 2D sprites and shapes
- **Audio**: Sound effects for stone placement and game events
- **States**: Menu, Game, Game Over

## 📈 DEVELOPMENT PHASES

### PHASE 1: FOUNDATION ARCHITECTURE
**Objective**: Set up basic game structure and board rendering

**Tasks**:
1. **Game Architecture Setup**
   - Create `GameScene` class replacing current `MyScene`
   - Implement basic game state management (PLAYING, GAME_OVER, MENU)
   - Set up proper scene structure

2. **Board Implementation** 
   - Create `Board` class (15x15 grid)
   - Implement board rendering with grid lines
   - Add coordinate system (0-14 for both axes)
   - Calculate cell size based on window dimensions

3. **Visual Foundation**
   - Remove template animation code
   - Create clean background
   - Draw board grid lines
   - Add board border and labels (A-O, 1-15)

**Success Criteria**:
- ✅ Clean 15x15 grid displayed
- ✅ Proper coordinate mapping
- ✅ No template code remaining
- ✅ Window centered and responsive

### PHASE 2: STONE MECHANICS
**Objective**: Implement stone placement and basic game logic

**Tasks**:
1. **Stone Classes**
   - Create `Stone` enum (BLACK, WHITE, EMPTY)
   - Create `Position` data class (row, col)
   - Implement board state tracking

2. **Input Handling**
   - Mouse click detection on board
   - Convert screen coordinates to board coordinates
   - Validate legal moves (empty cells only)

3. **Stone Rendering**
   - Draw black and white stones as circles
   - Implement stone placement animation
   - Add visual feedback for hover states

4. **Turn Management**
   - Track current player (BLACK starts first)
   - Switch turns after valid moves
   - Display current player indicator

**Success Criteria**:
- ✅ Stones appear on mouse clicks
- ✅ Only empty cells accept stones  
- ✅ Turn alternates between black/white
- ✅ Visual feedback for interactions

### PHASE 3: WIN CONDITION LOGIC
**Objective**: Implement win detection and game completion

**Tasks**:
1. **Win Detection Algorithm**
   - Check horizontal lines (5 in a row)
   - Check vertical lines (5 in a row)
   - Check diagonal lines (both directions)
   - Implement efficient win checking after each move

2. **Game Over State**
   - Detect and announce winner
   - Highlight winning line
   - Prevent further moves after win
   - Add restart functionality

3. **Draw Condition**
   - Detect board full (225 moves)
   - Handle draw state appropriately

**Success Criteria**:
- ✅ Correctly detects 5 in a row in all directions
- ✅ Game stops after win/draw
- ✅ Winner is clearly indicated
- ✅ Restart functionality works

### PHASE 4: AI OPPONENT
**Objective**: Create intelligent AI player

**Tasks**:
1. **Basic AI Structure**
   - Create `AIPlayer` class
   - Implement move evaluation system
   - Add configurable difficulty levels

2. **AI Algorithms**
   - **Easy**: Random valid moves
   - **Medium**: Block opponent wins + basic attack
   - **Hard**: Minimax with alpha-beta pruning (depth 4-6)

3. **AI Integration**
   - Add Human vs AI game mode
   - Implement AI move delays for natural feel
   - Add AI thinking indicator

**Success Criteria**:
- ✅ AI makes legal moves
- ✅ Medium AI blocks obvious threats
- ✅ Hard AI provides challenging gameplay
- ✅ Smooth AI integration with turn system

### PHASE 5: POLISH & UX
**Objective**: Enhance user experience and add final features

**Tasks**:
1. **User Interface**
   - Main menu with game mode selection
   - In-game UI (current player, score, restart button)
   - Settings menu (AI difficulty, board themes)

2. **Audio System**
   - Stone placement sound effects
   - Win/lose music
   - Background ambient sounds
   - Volume controls

3. **Visual Enhancements**
   - Stone placement animations
   - Win line highlighting animation
   - Particle effects for special moves
   - Multiple board themes

4. **Quality of Life**
   - Move history/undo functionality
   - Save/load game state
   - Statistics tracking
   - Hints system for beginners

**Success Criteria**:
- ✅ Complete menu system
- ✅ Audio feedback for all actions
- ✅ Smooth animations throughout
- ✅ Professional game feel

## 🛠️ TECHNICAL GUIDELINES

### Code Structure
```kotlin
// Suggested file organization
src/
├── main.kt                    // Entry point
├── scenes/
│   ├── MenuScene.kt          // Main menu
│   ├── GameScene.kt          // Main gameplay
│   └── GameOverScene.kt      // Results screen
├── game/
│   ├── Board.kt              // Game board logic
│   ├── Stone.kt              // Stone types and logic
│   ├── GameState.kt          // Game state management
│   └── WinChecker.kt         // Win condition logic
├── ai/
│   ├── AIPlayer.kt           // AI interface
│   ├── RandomAI.kt           // Easy difficulty
│   ├── MediumAI.kt           // Medium difficulty
│   └── MinimaxAI.kt          // Hard difficulty
└── ui/
    ├── GameUI.kt             // In-game interface
    └── MenuUI.kt             // Menu interface
```

### KorGE Best Practices
- Use `SContainer.sceneMain()` for scene setup
- Leverage KorGE's tween system for animations
- Use `input.mouse` for click handling
- Implement proper resource loading with `resourcesVfs`
- Use KorGE's graphics API for efficient rendering

## 📝 PROMPT INSTRUCTIONS FOR CLAUDE

When asking Claude for help with each phase:

1. **Always specify current phase**: "I'm working on Phase X: [Phase Name]"
2. **Provide context**: Share relevant existing code
3. **Be specific**: Ask for specific tasks within the phase
4. **Request complete code**: Ask for full implementations, not snippets
5. **Testing focus**: Request code that can be immediately tested

### Example Phase Request:
```
I'm working on Phase 1: Foundation Architecture for my Gomoku game in KorGE.

Current code: [paste main.kt]

Please help me implement Task 2: Board Implementation. I need:
- Complete Board class with 15x15 grid
- Board rendering with grid lines  
- Proper coordinate system setup
- Cell size calculation for 512x512 window

Provide complete, ready-to-use code that I can test immediately.
```

## 🎯 SUCCESS METRICS

### Phase Completion Checklist
- [ ] Phase 1: Clean board rendering
- [ ] Phase 2: Stone placement working
- [ ] Phase 3: Win detection functional
- [ ] Phase 4: AI opponent implemented
- [ ] Phase 5: Polish and UX complete

### Final Game Features
- [ ] Human vs Human mode
- [ ] Human vs AI mode (3 difficulty levels)
- [ ] Complete UI with menus
- [ ] Audio feedback
- [ ] Smooth animations
- [ ] Professional presentation

## 🚀 NEXT STEPS

1. **Start with Phase 1, Task 1**: Replace MyScene with GameScene
2. **Use this prompt** as reference for each Claude interaction
3. **Test after each task** to ensure functionality
4. **Document progress** in this file
5. **Move to next phase** only after current phase completion

---
*This prompt serves as the master plan for Gomoku development. Update progress as phases complete.*