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

- `src/main.kt` — точка входа (`suspend fun main() = Korge { ... }`), монтирует `sceneContainer` и `Nav`. Грузит `Fonts` и `SettingsStore`, стартует сразу в `MenuScene`.
- `src/scenes/` — экраны (`MenuScene`, `GameScene`, `SettingsScene`, `HelpScene`, `VictoryScene`) + `Nav` (роутинг) + `GameSession` (текущая партия).
- `src/ui/` — дизайн-система Kintsugi: `Theme.kt` (палитры/типы/spacing, читает `dark` из `SettingsStore`), `KinSeam.kt` (золотая жила), `Stone.kt`, `Enso.kt`, `Widgets.kt` (кнопки/тогглы/сегменты + `kinText` DPI-резкий текст + `kinPaperBackground`), `KinBoard.kt` (доска со слоем подсказок), `Strings.kt` (**все пользовательские строки** — object `Str`, задел под локализацию; новые UI-тексты добавляй только туда).
- `src/logic/` — пакет `logic`. `GameLogic.kt` (правила), `Settings.kt` (`Settings` + `SettingsStore` с JSON-персистом в `applicationDataVfs`).
- `src/logic/ai/AiPlayer.kt` — `Difficulty` enum, `AiFactory`, `RandomNearAi`, `HeuristicAi` (single-ply scoring threats). `topMoves(...)` для подсказок.
- `src/model/` — пакет `model` (модель данных).
- `test/` — тесты (пакет `test` для скриптовых, корневой пакет для `ViewsForTesting`).
- `resources/fonts/` — Noto Serif, Noto Serif JP, Inter (TTF, бандлятся в сборку).
- `resources/sounds/stone.wav` — синтезированный клик камня (играется на каждый ход, гейт — `Settings.sound`).
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

## Тесты

Все тесты в `test/` — `kotlin.test`, гоняются через `./gradlew jvmTest` (и в CI, `.github/workflows/gradle.yml`). Логические — обычные классы с `@Test` (`GomokuTest.kt`, `AiPlayerTest.kt` и др.), сценовые — `ViewsForTesting` (`test/test.kt`). Новые тесты на KorGE-сцены пиши в стиле `ViewsForTesting`, логику — обычным `kotlin.test`.

## Зависимости и конфигурация

- `gradle/libs.versions.toml` — версия `korge = 6.0.0`.
- `build.gradle.kts`: `id = "com.awac.gomoku"`, включены `targetJvm/Js/WasmJs/Ios/Android` + `serializationJson()` + `androidPermission("android.permission.VIBRATE")`.
- `deps.kproject.yml` — все KorGE-аддоны (fleks, ldtk, compose, dragonbones, swf, spine, box2d) закомментированы, активных kproject-зависимостей нет.
- `gradle.properties`: `-Xmx4g`, configuration cache включён (`org.gradle.configuration-cache=true`).
- `local.properties` хранит `sdk.dir` для Android — генерируется локально, не коммитить.

## DEVELOPMENT_PROMPT.md

Это историческая «дорожная карта» (фазы 1–5). Реализованы фазы 1–4 целиком и фаза 5: доска, ходы, победа/ничья, AI (3 уровня), меню, настройки, помощь, экран победы, дизайн-система Kintsugi, persistence настроек, crossfade-анимация переходов сцен, звук хода (`resources/sounds/stone.wav`) и haptic feedback на Android (через `NativeVibration`). Размер окна — 360×720 (мобильный портрет), не 512×512 как в этом документе.

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

В `GameScene` пользователь — BLACK, AI — WHITE. После хода игрока в AI-режиме планируется `aiTurn()` через `launch { delay(450); ... }`. Пока AI «думает», `aiThinking` блокирует клики (через `isHumanTurn()`) **и кнопку Undo** (`busy` в `renderControls`) — иначе undo мутировал бы `Board`, который `chooseMove` читает на `Dispatchers.Default`. После `chooseMove` состояние перепроверяется перед применением хода. Undo в AI-режиме откатывает 2 хода (свой + AI), чтобы вернуть очередь игроку; undo доступен и после победы — `undoMove` возвращает партию в PLAYING.

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
