# Project structure

```
res://
├── project.godot
├── icon.svg
├── theme/
│   ├── theme_light.tres
│   ├── theme_dark.tres
│   └── colors.gd                 # Singleton (autoload) с палитрой
├── assets/
│   └── fonts/
│       ├── NotoSerif-Regular.ttf
│       ├── NotoSerif-Italic.ttf
│       ├── NotoSerifJP-Regular.ttf
│       ├── NotoSerifJP-SemiBold.ttf
│       ├── Inter-Regular.ttf
│       ├── Inter-Medium.ttf
│       ├── Inter-SemiBold.ttf
│       └── Inter-Bold.ttf
├── addons/
│   └── kintsugi/
│       └── kin_seam.gd           # Line2D-генератор золотой жилы
├── ui/
│   ├── buttons/
│   │   ├── primary_button.tscn
│   │   ├── secondary_button.tscn
│   │   └── segmented.tscn
│   ├── toggle.tscn
│   └── status_bar.tscn
├── screens/
│   ├── menu.tscn
│   ├── menu.gd
│   ├── game.tscn
│   ├── game.gd
│   ├── victory.tscn
│   ├── victory.gd
│   ├── settings.tscn
│   ├── settings.gd
│   ├── help.tscn
│   └── help.gd
├── game/
│   ├── board.gd                  # Resource: 15×15 модель + win-detection
│   ├── ai.gd                     # Minimax + heuristic
│   └── settings_store.gd         # Wrap ConfigFile
└── nodes/
    ├── game_board.gd             # Control: рисует доску, ловит ввод
    ├── stone.gd                  # Control: камень с _draw градиентом
    └── enso.gd                   # Control: рисует энсо для меню
```

## Autoloads

В `Project Settings → Autoload`:
- `Colors` → `res://theme/colors.gd` — текущая палитра, переключение тем.
- `Settings` → `res://game/settings_store.gd` — singleton для чтения/записи.

## Colors singleton (`theme/colors.gd`)

```gdscript
extends Node

signal theme_changed(is_dark: bool)

const LIGHT := {
    "paper":      Color("#f5f0e6"),
    "paper_deep": Color("#ede5d4"),
    "surface":    Color("#faf6ec"),
    "ink":        Color("#1a1814"),
    "ink_soft":   Color("#3a3530"),
    "muted":      Color("#8a8378"),
    "line":       Color(0.102, 0.094, 0.078, 0.10),
    "line_firm":  Color(0.102, 0.094, 0.078, 0.32),
    "vermillion": Color("#c1442a"),
    "gold":       Color("#b48a3c"),
    "gold_soft":  Color("#d9b46a"),
}
const DARK := {
    "paper":      Color("#14130f"),
    "paper_deep": Color("#1c1a16"),
    "surface":    Color("#0e0d0a"),
    "ink":        Color("#f0ede4"),
    "ink_soft":   Color("#bdb8ad"),
    "muted":      Color("#6a6760"),
    "line":       Color(1.0, 0.992, 0.961, 0.08),
    "line_firm":  Color(1.0, 0.992, 0.961, 0.22),
    "vermillion": Color("#d35a3e"),
    "gold":       Color("#d4a652"),
    "gold_soft":  Color("#e9c476"),
}

var is_dark: bool = false
var current: Dictionary:
    get: return DARK if is_dark else LIGHT

func set_dark(v: bool) -> void:
    if v == is_dark: return
    is_dark = v
    theme_changed.emit(is_dark)
```

## Stretch & viewport

`project.godot`:
```
[display]
window/size/viewport_width=360
window/size/viewport_height=720
window/stretch/mode="canvas_items"
window/stretch/aspect="keep"
window/handheld/orientation="portrait"
```

## GameBoard.gd (узловой набросок)

```gdscript
class_name GameBoard
extends Control

const SIZE := 15
const CELL := 22
const PAD := 14

@export var model: Board
var hover_cell := Vector2i(-1, -1)

func _draw() -> void:
    var c = Colors.current
    var line_alpha = 0.7 if not Colors.is_dark else 0.45
    var ink_line = Color(c.ink.r, c.ink.g, c.ink.b, line_alpha)

    # background panel
    draw_rect(Rect2(Vector2.ZERO, size), c.paper_deep)

    # grid
    for i in SIZE:
        var p = PAD + i * CELL
        draw_line(Vector2(PAD, p), Vector2(PAD + (SIZE-1) * CELL, p), ink_line, 0.6)
        draw_line(Vector2(p, PAD), Vector2(p, PAD + (SIZE-1) * CELL), ink_line, 0.6)

    # hoshi
    for h in [Vector2i(3,3), Vector2i(3,11), Vector2i(7,7), Vector2i(11,3), Vector2i(11,11)]:
        draw_circle(_cell_center(h), 1.8, c.ink)

    # stones
    for r in SIZE:
        for col in SIZE:
            var v = model.cell(r, col)
            if v == 0: continue
            _draw_stone(_cell_center(Vector2i(col, r)), v, model.is_last(r, col), model.is_in_winline(r, col))

    # winning seam (динамическая жила)
    if model.has_winline():
        var line = model.winline_endpoints()
        var p1 = _cell_center(line[0])
        var p2 = _cell_center(line[1])
        # Используем helper, либо вкладываем Line2D-узлы — см. kin_seam.gd
        KinSeam.draw_inline(self, p1, p2, {"jitter": 4, "seed": 12, "width": 2.4})

func _cell_center(p: Vector2i) -> Vector2:
    return Vector2(PAD + p.x * CELL, PAD + p.y * CELL)
```

`_draw_stone` — radial gradient через `draw_circle` нескольких слоёв или `draw_texture` с предрендеренным камнем.

## Поток сцен

`Main` autoload или сцена-роутер: меняет current scene через `get_tree().change_scene_to_file()`. Передача данных между сценами — через `Settings` singleton (тема, сложность) и `GameSession` singleton (текущая партия, счётчик ходов, истории).
