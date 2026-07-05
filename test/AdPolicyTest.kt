package test

import logic.*
import kotlin.test.*

class AdPolicyTest {

    @Test
    fun bannerAlwaysUnlessSilencePurchased() {
        assertTrue(AdPolicy.bannerAllowed(Settings()))
        assertFalse(AdPolicy.bannerAllowed(Settings(adsRemoved = true)))
    }

    @Test
    fun interstitialEveryNinthGame() {
        val s = Settings()
        assertFalse(AdPolicy.interstitialDue(s, 0))
        assertFalse(AdPolicy.interstitialDue(s, 8))
        assertTrue(AdPolicy.interstitialDue(s, 9))
        assertFalse(AdPolicy.interstitialDue(s, 10))
        assertTrue(AdPolicy.interstitialDue(s, 18))
    }

    @Test
    fun interstitialRespectsPurchase() {
        assertFalse(AdPolicy.interstitialDue(Settings(adsRemoved = true), 9))
    }
}
