# Реклама: правила показа и выбор стека

Статус: исследование + клиентские гейты (2026-07). SDK не подключён.
Гейты уже в коде: `logic/AdPolicy.kt` (+ `AdPolicyTest`), счётчик партий —
`RecordsStore.stats.games`, флаг «Тишины» — `Settings.adsRemoved`.

## Правила показа (ревизия 2026-07, решение владельца)

1. **Баннер на экране окончания партии — всегда, с первого запуска**
   (никаких grace-периодов). Формат — **MREC 300×250**, не тонкая полоска
   320×50: тот же слот по площади платит в разы больше, и он уже
   зарезервирован вёрсткой ad-состояния экрана победы (нижняя карточка).
2. **Полноэкранный interstitial — раз в 9 завершённых партий**, показывается
   **перед** экраном окончания (финал партии → interstitial → Victory/Defeat).
   Обычный системный interstitial из SDK — стандартная механика индустрии,
   без кастомных «тихих» суррогатов.
3. Реклама по-прежнему **никогда** не появляется на доске и во время партии.
4. Покупка **«Тишина»** в Чайной (`Settings.adsRemoved`) убирает всё.
5. Печать + баннер на экране победы сосуществуют: используется компактная
   ad-вёрстка из дизайна (мини-доска 84 px, жилы скрыты), оттиск печати
   остаётся.

Прежняя идея «нативная васи-карточка + тихий интерстишл раз в 3/9 партий»
отменена как нестандартная — история в git.

## Выбор стека: AppLovin MAX (медиация с bidding)

Рынок — международный. При стандартных форматах (interstitial + MREC)
максимум выгоды даёт не одна сеть, а **аукцион сетей**: медиация с in-app
bidding поднимает blended eCPM на десятки процентов против любой одиночной
сети (типичный ориентир из индустрии — +30–35% против single-network).

**Рекомендация: AppLovin MAX** как медиатор, в demand-стек подключить:
AdMob (Google bidding), Mintegral, Unity Ads, Liftoff, Pangle, Meta AN.

Почему MAX, а не альтернативы:
- **iOS**: AppLovin — доминирующая сеть (39% рынка ad-monetization по
  Tenjin Q1 2026), собственный demand сильнейший; медиатор с родным
  demand-ом №1 — естественный выбор.
- **Android**: рынок дробный (AdMob 24%, AppLovin 19%, Mintegral 17%,
  Unity 12%) — ровно та ситуация, где bidding-аукцион и добирает деньги.
- Interstitial в tier-1 гео: ~$5–15 eCPM (бенчмарки AppLovin 2025);
  MREC заметно выше классического баннера.
- **Unity LevelPlay** — равноценная альтернатива (выбрать её, если появится
  зависимость от Unity-стека); **AdMob-медиация** — вариант «попроще», но
  слабее по bidding-конкуренции.
- Старт «в одну сеть» (только AdMob) допустим как этап интеграции, но
  оставляет на столе ~треть дохода — переезд на MAX закладывать сразу.

RU-примечание: международный стек в РФ не показывает рекламу (Google demand
выключен с 2022, RU-паблишеры отрезаны с 2024; с 2025 разрешён только
third-party waterfall). RU не цель — RU-пользователи просто не видят рекламу.
Если решение изменится — RuStore-сборка с Yandex Mobile Ads SDK.

## Интеграция с KorGE (когда дойдёт до кода)

- KorGE Android рендерит в GL-surface внутри `KorgwActivity`; баннер (MREC)
  и interstitial — Android-view поверх сцены, expect/actual мост из
  common-кода (no-op на JVM/JS/iOS до iOS-интеграции).
- Точки вызова уже определены:
  - предзагрузка interstitial — на старте `GameScene`;
  - `AdPolicy.interstitialDue(...)` — в `applyMoveRef` при финале, показ
    перед `Nav.goVictory()/goDefeat()`;
  - `AdPolicy.bannerAllowed(...)` — в `VictoryScene`/`DefeatScene`, MREC в
    нижнем слоте компактной вёрстки.
- GDPR/ATT: MAX включает Google UMP-совместимый consent-флоу — обязателен
  для EEA/UK и iOS ATT; учесть при интеграции.

## Источники

- [Tenjin Ad Monetization Benchmark 2026](https://tenjin.com/blog/ad-mon-gaming-2026/) — доли сетей iOS/Android Q1 2026
- [Unity LevelPlay vs AppLovin MAX (AdReact, 2026)](https://adreact.com/blog/unity-ads-vs-applovin-max-mediation-comparison/)
- [LevelPlay vs MAX — выбор медиации (bidlogic)](https://bidlogic.io/2025/08/29/unity-levelplay-vs-applovin-max-2025-how-to-choose-the-best-ad-mediation-platform/)
- [Unity Ads vs AdMob для игр (Bizzware)](https://bizzware.in/blog/unity-ads-vs-admob/)
- [Regional Monetization Pause Policies (Google)](https://support.google.com/publisherpolicies/answer/15766875?hl=en) — статус РФ
