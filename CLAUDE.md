# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект

Gomoku (5 в ряд) на движке **KorGE 6.0.0** (Kotlin Multiplatform). Цели сборки: JVM (desktop), JS, WasmJS, iOS, Android. `rootProject.name = "gomoku"` (settings.gradle.kts), Android applicationId — `com.awac.gomoku` (через `korge.id` в `build.gradle.kts`).

## Команды

KorGE 6 требует **JDK 21**. Если по умолчанию JDK 17, экспортируй перед `./gradlew`:

```bash
export JAVA_HOME=/Users/lutse/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.7/Contents/Home
./gradlew runJvm                      # запуск desktop-версии (основной вариант разработки)
./gradlew runJvmAutoreload            # JVM с авто-reload (есть конфигурация в .idea/.run)
./gradlew jsBrowserDevelopmentRun     # запуск в браузере (JS)
./gradlew compileDebugKotlinAndroid   # компиляция Android-таргета
./gradlew compileKotlinIosArm64       # компиляция iOS-таргета
./gradlew test                        # запуск всех тестов (KorGE ViewsForTesting на JVM)
./gradlew jvmTest                     # только JVM-тесты
./testJs.sh                           # скрипт для прогона тестов JS-таргета
./gradlew --stop                      # на случай зависшего daemon
```

## Раскладка исходников — нестандартная

KorGE-плагин маппит исходники прямо из корня репозитория, **без** `src/main/kotlin` или `src/commonMain/kotlin`:

- `src/main.kt` — точка входа (`suspend fun main() = Korge { ... }`), монтирует `sceneContainer` и `Nav`. Грузит `Fonts` и `SettingsStore`. На первом запуске (`Settings.firstRun`) ведёт в `HelpScene`.
- `src/scenes/` — экраны (`MenuScene`, `GameScene`, `SettingsScene`, `HelpScene`, `VictoryScene`) + `Nav` (роутинг) + `GameSession` (текущая партия).
- `src/ui/` — дизайн-система Kintsugi: `Theme.kt` (палитры/типы/spacing, читает `dark` из `SettingsStore`), `KinSeam.kt` (золотая жила), `Stone.kt`, `Enso.kt`, `Widgets.kt` (кнопки/тогглы/сегменты), `KinBoard.kt` (доска со слоем подсказок).
- `src/logic/` — пакет `logic`. `GameLogic.kt` (правила), `Settings.kt` (`Settings` + `SettingsStore` с JSON-персистом в `applicationDataVfs`).
- `src/logic/ai/AiPlayer.kt` — `Difficulty` enum, `AiFactory`, `RandomNearAi`, `HeuristicAi` (single-ply scoring threats). `topMoves(...)` для подсказок.
- `src/model/` — пакет `model` (модель данных).
- `test/` — тесты (пакет `test` для скриптовых, корневой пакет для `ViewsForTesting`).
- `resources/fonts/` — Noto Serif, Noto Serif JP, Inter (TTF, бандлятся в сборку).
- `docs/design/` — токены и спецификация дизайна (DESIGN_SYSTEM.md, SCREENS.md, VEINS.md, tokens/*.json) — источник истины.

Файлы вида `src/main_backup.kt.backup`, `null.frame`, `null.socket` — мусор от старых запусков, игнорируй. `.gitignore` явно исключает `src/main/*` (то есть стандартную раскладку).

## Архитектура

```
main.kt → MenuScene
            ├→ GameScene (через Nav.goGame; GameSession хранит GameLogic)
            │     ├→ VictoryScene (через Nav.goVictory после победы)
            │     └→ MenuScene
            ├→ SettingsScene
            └→ HelpScene
```

- **`scenes/Nav.kt`** — singleton с ссылкой на `SceneContainer`. Все навигационные функции синхронные (`Nav.goMenu()` и т.п.); внутри `launchImmediately` запускает `sceneContainer.changeTo<MyScene>()`. Также держит `currentVictoryWinner` для передачи в `VictoryScene`.
- **`scenes/GameSession.kt`** (в Nav.kt) — синглтон с текущим `GameLogic` и `GameMode`. Переживает переключения сцен и темы, чтобы `Theme.toggle() + Nav.goGameKeepState()` не сбрасывал партию.
- **`ui/Theme.kt`** — `Theme` singleton с `dark: Boolean` и `colors: KinPalette`. `Theme.toggle()` уведомляет подписчиков, но в этой реализации каждое переключение сцены просто читает `Theme.colors` свежим. Шрифты в `Fonts` объекте — `loadOnce()` в `main.kt` при старте.
- **`ui/KinSeam.kt`** — золотая жила: Catmull-Rom через jitter-точки → polyline → 3 слоя (underglow/main/highlight) + опциональные branches и pool. Порт `kin_seam.gd` из `docs/design/godot/kin_seam.gd`. Используется как декоративный элемент бренда и линия победы.
- **`ui/KinBoard.kt`** — `KinBoardView : Container`. 15×15 grid + хоси + слой камней (rebuilt on `redraw()`) + слой win-seam. 225 невидимых hit-target клеток. `var onCellClickHandler` чтобы навешивать обработчик после конструирования (нужно из-за circular deps в GameScene).
- **`logic/GameLogic.kt`** — текущий игрок, `GameState`, проверка выигрыша (4 направления, ≥5), отмена. Возвращает `MoveResult` (sealed: `Success` / `InvalidMove` / `GameEnded`) и `UndoResult`.
- **`model/Board.kt`** — 15×15 поле + история ходов. Источник истины состояния партии.
- **`model/GameModels.kt`** — `StoneColor`, `Position` (notation A1–O15), `Move`, `GameState`, `WinningLine`. Все `@Serializable`.

Темизация: при `Theme.toggle()` сцена не диффится — `Nav.goXxx()` создаёт сцену заново, она читает свежий `Theme.colors`. Это просто и чисто, цена — лёгкое мерцание при переключении (приемлемо).

## Дублирующийся модуль `shared/`

В `shared/src/commonMain/kotlin/com/gomoku/shared/model/` лежат `Board.kt` и `GameModels.kt` — упрощённые копии моделей из `src/model/`. Этот модуль **не включён** в `settings.gradle.kts` и не используется основной сборкой KorGE. Его собственный `shared/build.gradle.kts` рассчитан на отдельный KMM-проект (Android library + iOS framework через cocoapods), которого здесь нет.

Правки в `shared/` не повлияют на игру. Если задача про игровую модель — правь `src/model/`. Не синхронизируй копии «на всякий случай» — это путь к расхождению; уточни намерения у пользователя.

## Тесты

Два разных подхода в `test/`:

- **`test/GomokuTest.kt`** — самописные ассерты на `assert()` через `fun main()`. Это не JUnit, запускается как обычная программа. Покрывает `Board`, `GameLogic`, выигрышные линии.
- **`test/test.kt`** — `class MyTest : ViewsForTesting()` с `@Test` (kotlin.test) и `viewsTest { ... }` для имитации сцены. Запускается через `./gradlew test` / `jvmTest`.

Новые тесты на KorGE-сцены пиши в стиле `ViewsForTesting`. Чисто логические тесты можно как `kotlin.test` рядом — корневой пакет уже подхватывается.

## Зависимости и конфигурация

- `gradle/libs.versions.toml` — версия `korge = 6.0.0`.
- `build.gradle.kts`: `id = "com.awac.gomoku"`, включены `targetJvm/Js/WasmJs/Ios/Android` + `serializationJson()` + `androidPermission("android.permission.VIBRATE")`.
- `deps.kproject.yml` — все KorGE-аддоны (fleks, ldtk, compose, dragonbones, swf, spine, box2d) закомментированы, активных kproject-зависимостей нет.
- `gradle.properties`: `-Xmx4g`, configuration cache включён (`org.gradle.configuration-cache=true`).
- `local.properties` хранит `sdk.dir` для Android — генерируется локально, не коммитить.

## DEVELOPMENT_PROMPT.md

Это историческая «дорожная карта» (фазы 1–5). Реализованы фазы 1–4 целиком и почти вся фаза 5: доска, ходы, победа, AI (3 уровня), меню, настройки, помощь, экран победы, дизайн-система Kintsugi, persistence настроек, crossfade-анимация переходов сцен, haptic feedback на Android (через `NativeVibration`). **Не сделано**: звук (нет `.wav`-ассетов — `Settings.sound` живёт в Settings, но не подцеплен к источнику звука). Размер окна — 360×720 (мобильный портрет), не 512×512 как в этом документе.

## Анимация переходов и haptics

`scenes/Nav.kt` определяет `Transition crossfade` (220 мс): прозрачность старой сцены 1→0, новой 0→1. Используется во всех `Nav.goXxx()`. KorGE 6.x декларирует `alphaTransition` как `@Deprecated`, поэтому пишем свой `Transition("crossfade") { ... }` через `prev.alphaF`/`next.alphaF`.

`GameScene.triggerHaptic()` использует `korlibs.korge.service.vibration.NativeVibration(coroutineContext)`. Срабатывает на ход игрока и AI (`amplitude=0.5`, 20 мс), и сильнее при победе (`amplitude=0.9`, 60 мс). Гейтится `SettingsStore.current.sound` (тоггл «Звуки и вибрация»). На JVM — no-op (нет железа). На Android — Android `Vibrator`; нужен permission `android.permission.VIBRATE` — добавлен через `korge { androidPermission("android.permission.VIBRATE") }` в `build.gradle.kts`.

## APK

```bash
export JAVA_HOME=/Users/lutse/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.7/Contents/Home
./gradlew assembleDebug
```

Результат: `build/outputs/apk/debug/gomoku-debug.apk` (~12.7 МБ). package `com.awac.gomoku`, min SDK 21, target SDK 33.

## AI и подсказки

`logic/ai/AiPlayer.kt` — single-ply heuristic, оценивает каждую кандидат-клетку как сумму атакующего score (создаёт собственные тройки/четвёрки) и защитного score (блокирует чужие). `Difficulty.EASY` — `RandomNearAi` (случайная клетка в радиусе 2 от занятых). `MID` — heuristic с радиусом 1 и opponentBias 0.9. `HARD` — радиус 2, bias 1.0 (увеличивает кругозор). Полноценный minimax не делал — single-ply на этих весах достаточно сильно играет (блокирует все стандартные угрозы и строит свои). Подсказки (`topMoves`) используют ту же эвристику.

В `GameScene` пользователь — BLACK, AI — WHITE. После хода игрока в AI-режиме планируется `aiTurn()` через `launch { delay(450); ... }`. Пока AI «думает», `aiThinking` блокирует клики (через `isHumanTurn()`). Undo в AI-режиме откатывает 2 хода (свой + AI), чтобы вернуть очередь игроку.

## Settings persistence

`SettingsStore` (singleton) хранит `Settings` (data class, `@Serializable`) и при каждом изменении через `update { it.copy(...) }` запускает `launchImmediately(EmptyCoroutineContext) { save() }`. Файл — `applicationDataVfs/<APP_ID>/settings.json` (на macOS это `~/Library/Preferences/gomoku-kintsugi/settings.json`). Загрузка — один раз в `main.kt` через `SettingsStore.load()` до первого `changeTo`. Если файл отсутствует или битый — остаются дефолты, ловится try/catch.

`Theme.dark` теперь — getter над `SettingsStore.current.dark`, не отдельный mutable. Это устраняет дрейф при загрузке настроек.

## Дизайн (Kintsugi)

Источник истины — `docs/design/`:
- `DESIGN_SYSTEM.md` — принципы (тишина, золото, sumi-е), палитра, типографика, spacing.
- `SCREENS.md` — точная вёрстка пяти экранов в логических 360×720 px.
- `VEINS.md` — каталог золотых жил по экранам (координаты, jitter, seed, branches).
- `tokens/{colors,typography,spacing,components}.json` — токены, нормативный JSON.

Базовый принцип: один акцент — золото. Киноварь только на маркер последнего хода. Углы 0 (никаких radius). Все отступы кратны 4. Ничего не добавляй в дизайн «от себя» — если задача требует нового элемента, сверься с системой.

При изменении токена синхронизируй между `docs/design/tokens/colors.json` и `ui/Theme.kt` (`KIN_LIGHT`/`KIN_DARK`). JSON — нормативный.
