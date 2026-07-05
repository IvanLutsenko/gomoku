package scenes

import korlibs.image.text.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.*
import ui.*
import kotlin.math.PI
import kotlin.math.sin

// Splash — бумага, камень го с золотой жилой, имя. «Тихо дышит» и через
// ~1.2 с уступает меню (identity-kintsugi: KinSplash).
class SplashScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()
        val cx = w / 2.0

        kinPaperBackground(theme)

        // Мотив: камень r=44 + жила сквозь него + две боковые жилы
        val motif = container {
            kinStone(isBlack = true, radius = 44.0, theme = theme)
            kinSeam(
                x1 = -48.0, y1 = 14.0, x2 = 48.0, y2 = -20.0,
                jitter = 4.5, seed = 7, width = 2.6, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
            kinSeam(
                x1 = -96.0, y1 = 30.0, x2 = -42.0, y2 = 12.0,
                jitter = 3.0, seed = 12, width = 1.4,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
            kinSeam(
                x1 = 42.0, y1 = -18.0, x2 = 98.0, y2 = -34.0,
                jitter = 3.0, seed = 19, width = 1.4,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(cx, h * 0.40)

        // дыхание
        var t = 0.0
        motif.addUpdater { dt ->
            t += dt.milliseconds / 2600.0
            val k = (sin(t * 2.0 * PI) + 1.0) / 2.0
            motif.scale(1.0 + 0.015 * k)
            motif.alpha = 0.94 + 0.06 * k
        }

        kinText(Str.SPLASH_TITLE, 34.0, theme.ink, Fonts.serif) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, h * 0.40 + 44.0 + 26.0)
        }
        kinText(capsTracked(Str.BRAND_VERTICAL, 2), 12.0, theme.muted, Fonts.serifJp) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, h * 0.40 + 44.0 + 76.0)
        }
        kinText(Str.SPLASH_LOADING, 10.0, theme.muted, Fonts.uiMedium) {
            alignment = TextAlignment.TOP_CENTER
            position(cx, h - 36.0)
            alpha = 0.7
        }

        launch {
            delay(1200)
            Nav.goMenu()
        }
    }
}
