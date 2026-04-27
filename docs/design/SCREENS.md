# Screens

Базовое разрешение **360×720** логических px (портрет). Все размеры ниже — в этих единицах. Status bar (если у движка) занимает 28 px и не учитывается в макетах.

---

## 1. Menu (`menu.tscn`)

```
┌──────────────────────────────────┐
│  [stat]                  [☀/☾]  │  <- top bar
│                                  │
│           ◯ ✦  (энсо + жила)      │  <- 96×96, центр
│                                  │
│              五 目                │  <- display, 56 px
│                                  │
│        G  O  M  O  K  U           │  <- meta caps, разрядка
│         Пять камней в ряд          │  <- caption italic, muted
│                                  │
│      ╲╲╲ /// (длинная жила)         │  <- KinSeam 220×34, sweep
│                                  │
│    ┌────────────────────────┐    │
│    │  Игра с AI         →   │    │  <- primary, 280 wide
│    └────────────────────────┘    │
│    ┌────────────────────────┐    │
│    │  Игра вдвоём        →  │    │  <- secondary
│    └────────────────────────┘    │
│    ┌─────────┐ ┌─────────┐       │
│    │Настройки│ │ Помощь  │       │  <- secondary small, gap=12
│    └─────────┘ └─────────┘       │
│                                  │
│   ●  2026  ·  KORGE EDITION      │  <- footer, 11 px caps, gap=2
└──────────────────────────────────┘
```

**Узлы:**
- `Control` (root, fill window)
- `MarginContainer` `padding = 48 32 28 32` (top, h-sides, bottom, h-sides)
- `VBoxContainer` `alignment = center, separation = 6`
  - `Control` (96×96) — Эншо + Line2D жила (см. `VEINS.md` §1)
  - `Label "五目"` стиль `display`
  - `Label "G O M O K U"` стиль `label_caps`, `modulate = muted`
  - `Label "Пять камней в ряд"` стиль `caption_italic`, `muted`
  - `Control` 220×34 — длинная диагональная жила (`VEINS.md` §1)
  - `VBoxContainer` `separation = 12` — три кнопки
  - `HBoxContainer` `separation = 12` — Настройки + Помощь
- Footer как `MarginContainer` с `anchor_bottom = 1` и центрированный `Label`.

**Кнопка-тема (☀/☾):** `Button` 36×36, `flat = false`, абсолютно расположена `anchor_top_right`, бордер `1 px line_firm`, `corner_radius = 18` (исключение для этой круглой кнопки).

---

## 2. Game (`game.tscn`)

```
┌──────────────────────────────────┐
│ 五目                       ☾  МЕНЮ│
│ VS AI · СРЕДНЕ                   │
│                                  │
│           ● Ход чёрных            │  <- with stone dot
│                                  │
│    ╲╲ (corner vein top-left)      │
│  ┌────────────────────────────┐  │
│  │   ┃   ┃   ┃   ┃   ┃   ┃    │  │
│  │  ─●──○─────●──────────     │  │  <- 15×15 grid
│  │     │ │ │ ●  │  │ ░         │  │
│  │   ··········· (board)       │  │
│  └────────────────────────────┘  │
│                       ╲ (br vein)│
│                                  │
│   ХОД 11           ●○●○●          │  <- last-5 history dots
│                                  │
│  ┌──────────┐  ┌──────────────┐ │
│  │ ← Отменить│  │ Новая партия │ │  <- secondary + primary
│  └──────────┘  └──────────────┘ │
└──────────────────────────────────┘
```

**Узлы:**
- Top bar: `HBoxContainer` (24 24 — h-padding) с `VBox` слева (五目 + режим caps) и `HBox` справа (theme btn + menu btn).
- Status row: `HBoxContainer` `alignment = center, separation = 10`. Stone dot — `Control` 12×12 с радиальным градиентом, `Label` — стиль `body_strong`.
- Board: `Control` 322×322 (cell 22 × 14 + padding 14×2). Drawing — см. `STRUCTURE.md` § GameBoard.gd.
- Bottom bar: фиксирован `anchor_bottom`, `margin_bottom = -28`. Содержит `HBox` (Ход N + history dots) и `HBox` с двумя кнопками `separation = 10`.

**Жилы по углам доски:** см. `VEINS.md` §3. Это два маленьких `Line2D` поверх `Control` доски, не интерактивные (`mouse_filter = IGNORE`).

---

## 3. Victory (`victory.tscn` или overlay над game)

```
┌──────────────────────────────────┐
│                                  │
│                                  │
│            ●                      │  <- 96 stone, центр
│            ╱╱╱ (жила сквозь него)  │  <- 140×140 svg overlay
│            ╲╲╲                     │
│                                  │
│      ПАРТИЯ ЗАВЕРШЕНА             │  <- meta caps
│      Чёрные победили             │  <- title 40
│       Линия из пяти —            │  <- caption italic
│       золотая жила               │
│                                  │
│   ╲╲╱╱ ╲╲╱╱╱ (3+ ветв. жилы)        │  <- KinSeam ×5, 260×40
│                                  │
│    ┌────────────────────────┐    │
│    │  Ещё партию             │    │  <- primary
│    └────────────────────────┘    │
│    ┌────────────────────────┐    │
│    │  В меню                 │    │  <- secondary
│    └────────────────────────┘    │
└──────────────────────────────────┘
```

Камень-победитель — `TextureRect` или `Control` с `_draw()` (радиальный градиент). Поверх — `Control` 140×140 со смещением `-22, -22`, в нём 2 `Line2D` (главная жила + ответвление).

Под текстом — горизонтальная композиция из **5 жил** (см. `VEINS.md` §4): корневая ветка + два продолжения вверх и вниз.

---

## 4. Settings (`settings.tscn`)

```
┌──────────────────────────────────┐
│ ← Назад                          │
│                                  │
│ Настройки                         │  <- title 36
│ ──────                            │  <- KinSeam 96×12, gentle
│ ПАРАМЕТРЫ ПАРТИИ                 │  <- label_caps muted
│                                  │
│ ТЕМА                              │
│ ┌──────────┬──────────┐          │
│ │ ☀  Свет  │ ☾  Тьма  │          │  <- segmented
│ └──────────┴──────────┘          │
│                                  │
│ СЛОЖНОСТЬ AI                      │
│ ┌──────┬───────┬───────┐         │
│ │Легко │Средне │Сложно │         │
│ └──────┴───────┴───────┘         │
│                                  │
│ ПОМОЩНИКИ                         │
│ Подсказки ходов          ──◯─    │
│ ──────────────                    │  <- divider line
│ Звуки и вибрация         ─◯──    │
│                                  │
│ ──────────────────────────────    │  <- divider 1px line
│ ВЕРСИЯ 1.0.4         ● ОНЛАЙН    │  <- gold dot
└──────────────────────────────────┘
```

**Тогглы:** 44×24, gold-free. Off — bg `line_firm` (с прозрачностью соотв. темы), thumb `paper`. On — bg `ink`, thumb `paper`. Анимация — `Tween` 200 ms.

**Сегменты:** `HBoxContainer` `separation = 0` внутри `PanelContainer` с бордером 1 px. Каждый сегмент — `Button` `flat = true`, активный — `bg = ink, fg = paper`. Между сегментами — `VSeparator` или `theme_override_constants/separation = 0` + бордер слева у каждой кнопки кроме первой.

---

## 5. Help (`help.tscn`)

Скроллящийся экран. `ScrollContainer` поверх всего, внутри `VBoxContainer`.

```
┌──────────────────────────────────┐
│ ← Назад                          │
│                                  │
│ Помощь                            │  <- title 36
│ ╱──╲ (короткая жила с веткой)      │  <- KinSeam 110×18
│ ПРАВИЛА И СОВЕТЫ                 │
│                                  │
│ 一  Цель игры                     │  <- gold ideogram + heading
│     Выстройте пять камней…       │
│                                  │
│ 二  Управление                    │
│     Касание — поставить камень…   │
│                                  │
│ 三  Стратегия                     │
│     Блокируйте открытые тройки…   │
│                                  │
│ 四  Центр                         │
│     Первый ход — традиционно…    │
│                                  │
│ 五  Кинцуги                       │
│     Победная линия выкладывается │
│     золотом — как трещины…       │
│                                  │
│ ▼ scroll                         │
└──────────────────────────────────┘
```

**Секция:**
- `HBoxContainer` `alignment = top, separation = 16`
- `Label` иероглиф — `font_family = Noto Serif JP, font_size = 28, color = gold`, `custom_minimum_size = (32, 28)`.
- `VBoxContainer` (заголовок 16/600 + body 14/400 line_height ≈ 1.6, `ink_soft`).

Тексты — те же что в HTML-макете (см. также `tokens/components.json`).
