# Veins (золотые жилы) — где и как они появляются

Жилы — единственный декоративный элемент бренда. Никаких PNG-мазков. Все жилы рисуются скриптом `kin_seam.gd` через `Line2D` (см. `godot/kin_seam.gd`).

Каждая жила состоит из **трёх слоёв**:
1. **Underglow** — толстая полупрозрачная подложка (`width × 2.6–3`, opacity ≈ `0.18–0.20`).
2. **Main** — основной мазок цвета `gold` (`width = base`).
3. **Highlight** — тонкая нить цвета `gold_soft` (`width × 0.35–0.4`, opacity ≈ `0.85`).

Дополнительно может быть:
- **Branches** — 1–2 коротких ответвления, отходят перпендикулярно от точек 25 % и 70 % длины основной жилы.
- **Pool** — золотая капля (Circle radius `width × 0.9`) в одной из точек.

Координаты осей: x — горизонталь, y — вертикаль; начало в верхнем левом углу контейнера-`Control` жилы.

---

## §1. Меню — длинная диагональная жила через энсо

**Энсо (логотип):** `Control` 96×96. Сам круг рисуется в `_draw()` или как `Curve2D`/`Path2D`. Поверх — `KinSeam`:

```
KinSeam(start = Vector2(18, 56), end = Vector2(66, 28),
        jitter = 3, seed = 3, width = 1.6, branches = true)
```

Жила пересекает энсо по диагонали с лёгким искажением.

**Под подзаголовком:** `Control` 220×34, под надписью `Пять камней в ряд`:

```
KinSeam(start = Vector2(6, 22), end = Vector2(214, 12),
        jitter = 4, seed = 31, width = 1.5, branches = true)
```

Пологий sweep слева-вниз направо-вверх. Это «фирменный мазок» меню.

---

## §2. Заголовки Настройки и Помощь — короткая жила под заголовком

**Settings, под `Настройки`:** `Control` 96×12.
```
KinSeam(start = Vector2(2, 6), end = Vector2(94, 6),
        jitter = 2.5, seed = 41, width = 1.3)
```
Без ответвлений — спокойная подчерк-жила.

**Help, под `Помощь`:** `Control` 110×18.
```
KinSeam(start = Vector2(2, 9), end = Vector2(108, 11),
        jitter = 3, seed = 53, width = 1.3, branches = true)
```
С одним ответвлением — указывает, что ниже идёт многоступенчатый контент.

---

## §3. Игра — два угловых акцента у доски

Поверх `Control` доски, **не интерактивно**. Два малых `Control`:

**Top-left bracket** (60×60, смещён `top = -8, left = 22`):
```
KinSeam(start = Vector2(4, 32), end = Vector2(48, 6),
        jitter = 2.5, seed = 67, width = 1.2,
        global_opacity = 0.75)
```

**Bottom-right bracket** (50×50, смещён `bottom = -10, right = 24`):
```
KinSeam(start = Vector2(4, 42), end = Vector2(44, 8),
        jitter = 2, seed = 71, width = 1.0,
        global_opacity = 0.6)
```

---

## §4. Победа — пять переплетающихся жил

`Control` 260×40, под подписью `Линия из пяти — золотая жила`:

```
KinSeam(Vector2(2, 22),   Vector2(130, 18), jitter=3,   seed=81, width=1.5, branches=true)
KinSeam(Vector2(130, 18), Vector2(258, 26), jitter=3,   seed=83, width=1.4, branches=true)
KinSeam(Vector2(92, 20),  Vector2(70, 4),   jitter=2,   seed=87, width=0.9, opacity=0.8)
KinSeam(Vector2(170, 20), Vector2(196, 36), jitter=2,   seed=89, width=0.9, opacity=0.8)
KinSeam(Vector2(210, 24), Vector2(236, 6),  jitter=1.5, seed=93, width=0.7, opacity=0.7)
```

Это «корень + 4 ветки», создаёт ощущение прорастания.

**На самом камне-победителе** (140×140, поверх 96-камня со смещением `-22, -22`):
```
KinSeam(Vector2(20, 108), Vector2(120, 32), jitter=5, seed=11, width=2.8, branches=true)
KinSeam(Vector2(70, 70),  Vector2(92, 56),  jitter=2, seed=17, width=1.4, opacity=0.85)
```

---

## §5. Победная линия на доске (динамическая)

Когда срабатывает `winner != null`, рисуется **главная** игровая жила между центрами первого и последнего камней пятёрки:

```
var p1 = Vector2(padding + first_col * cell, padding + first_row * cell)
var p2 = Vector2(padding + last_col  * cell, padding + last_row  * cell)
KinSeam(p1, p2, jitter=4, seed=12, width=2.4)
```

Камни в выигрышной линии получают **обводку 1.5 px цвета `gold`** вокруг (вместо обычной тени). Реализуется в `_draw()` камня по флагу `is_win`.

---

## §6. Кнопка primary — без жилы

В первой итерации primary-кнопки не получают жилу — это сохраняет тишину. Если позже захочется акцент: тонкий `Line2D` шириной 1 px цвета `gold @ 0.4` по нижнему краю кнопки на ширину 60 % — задаётся через `Theme` override и выводится через `_draw()` кастомной кнопки `KinPrimaryButton`.

---

## §7. Расходники

| Параметр | Значение по умолчанию | Заметка |
|---|---|---|
| `segments` | `8` | сегментов в curve |
| `jitter` | `2.5–5` | амплитуда дрожания |
| `taper` | `sin(t × π)` | концы выходят чисто в endpoints |
| `branch_count` | `0–2` | по флагу `branches` |
| `pool_count` | `0–1` | пул золота в среднем сегменте |
| `seed` | int | детерминированность; меняй для каждого экземпляра |

Подробности реализации — в `godot/kin_seam.gd`.
