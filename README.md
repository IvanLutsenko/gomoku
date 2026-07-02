# Gomoku · 五目

Гомоку (пять в ряд) на **Kotlin Multiplatform** и движке **[KorGE 6](https://korge.org/)** с дизайн-системой **Kintsugi** — васи, тушь и золотые жилы: победная линия выкладывается золотом, как трещины в керамике кинцуги.

Таргеты: **JVM (desktop) · JS · WasmJS · Android · iOS**.

## Запуск

Требуется JDK 21.

```bash
./gradlew runJvm                      # desktop (основной вариант разработки)
./gradlew jsBrowserDevelopmentRun     # браузер (JS)
./gradlew assembleDebug               # Android APK → build/outputs/apk/debug/
./gradlew compileKotlinIosArm64       # iOS-таргет
./gradlew jvmTest                     # тесты
```

## Что внутри

- **Геймплей**: доска 15×15, победа от 5 камней в ряд (freestyle, overline засчитывается), отмена ходов (в том числе победного — партию можно продолжить), ничья.
- **AI**: три уровня. Легко — random-near; Средне/Сложно — эвристика угроз в один ход (open/closed three/four, защитный bias, случайный tie-break). Расчёт на фоновом потоке.
- **Режимы**: против AI или вдвоём за одним устройством.
- **UI Kintsugi**: светлая и тёмная темы, золотые жилы (Catmull-Rom + jitter, порт из дизайн-макета), камни с радиальным градиентом, DPI-резкий текст через bitmap-атласы.
- **Прочее**: подсказки ходов, звук камня + вибрация (Android), персист настроек в JSON, crossfade-переходы сцен.

Правила — freestyle Gomoku: без запретов Renju (3×3, 4×4), ряд длиннее пяти тоже выигрывает.

## Структура

```
src/main.kt        — точка входа
src/scenes/        — экраны (Menu, Game, Settings, Help, Victory) + Nav
src/ui/            — дизайн-система Kintsugi (Theme, KinSeam, KinBoard, Stone, Widgets)
src/logic/         — правила игры, настройки, AI
src/model/         — Board, модели (Position с нотацией A1–O15)
test/              — kotlin.test + KorGE ViewsForTesting
docs/design/       — дизайн-токены и спецификации (источник истины)
```

## Ограничения / планы

- AI — single-ply эвристика, не минимакс; не видит разрывные паттерны (`XX_XX`).
- Музыки нет, звук — один клик камня.
- Игрок всегда чёрные (первый ход) в режиме AI.

Backlog ведётся вне репозитория.
