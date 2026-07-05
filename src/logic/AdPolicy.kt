package logic

// Правила показа рекламы (ревизия 2026-07, см. docs/ADS.md):
// 1. Баннер (MREC) на экране окончания партии — всегда, с первого запуска.
// 2. Полноэкранный interstitial — раз в 9 завершённых партий, показывается
//    ПЕРЕД экраном окончания.
// 3. Покупка «Тишина» убирает всё.
// ponytail: SDK ещё не подключён — гейты готовы заранее, чтобы счётчики
// копились с текущих версий.
object AdPolicy {
    const val INTERSTITIAL_EVERY = 9

    /** Баннер на экране финала: всегда, если не куплена «Тишина». */
    fun bannerAllowed(settings: Settings): Boolean = !settings.adsRemoved

    /** Interstitial перед экраном финала: каждая 9-я завершённая партия. */
    fun interstitialDue(settings: Settings, gamesFinished: Int): Boolean =
        bannerAllowed(settings) &&
            gamesFinished > 0 &&
            gamesFinished % INTERSTITIAL_EVERY == 0
}
