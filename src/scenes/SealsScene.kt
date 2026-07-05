package scenes

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import logic.*
import ui.*

// Печати (ханко) — достижения. Девять киноварных печатей коллекции,
// легенда 和 (ничья) вне коллекции — скрыта до полного сбора, за полную
// коллекцию — мастерская печать 極 золотом.
class SealsScene : Scene() {

    private val tilt = listOf(-2.5, 1.5, -1.0, 2.0, 1.0, -1.5, 2.5, -2.0, 1.0)

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val stats = RecordsStore.current.stats
        val got = Seals.COLLECTION.count { it in stats.seals }
        val complete = Seals.isComplete(stats)

        kinPaperBackground(theme)
        onBackOrEscape { Nav.goMenu() }

        kinTextButton(Str.BACK, color = theme.muted) { Nav.goMenu() }.apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 22.0)
        }

        var y = 56.0
        kinText(Str.SEALS_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0
        container {
            kinSeam(
                x1 = 2.0, y1 = 6.0, x2 = 94.0, y2 = 6.0,
                jitter = 2.5, seed = 77, width = 1.3,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y)
        y += 18.0
        kinText(
            "${Str.SEALS_COLLECTED_PREFIX}$got${Str.SEALS_OF}${Seals.COLLECTION.size}",
            Type.labelCaps.size, theme.muted, Fonts.uiMedium,
        ) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 22.0

        // Прогресс: золотая жила заполняет тонкую линию
        val barW = w - 48.0
        solidRect(barW, 1.0, theme.line) { position(24.0, y + 6.0) }
        if (got > 0) {
            container {
                kinSeam(
                    x1 = 1.0, y1 = 6.0, x2 = barW * got / Seals.COLLECTION.size, y2 = 6.0,
                    jitter = 1.5, seed = 79, width = 1.2,
                    color = theme.gold, colorSoft = theme.goldSoft,
                )
            }.position(24.0, y)
        }
        y += 24.0

        // Мастерская печать — за полную коллекцию
        if (complete) {
            val cardH = 78.0
            roundRect(
                Size(barW, cardH), RectCorners(BtnSpec.RADIUS),
                fill = Colors.TRANSPARENT, stroke = theme.line, strokeThickness = 1.0,
            ).position(24.0, y)
            renderTile(this, 24.0 + 14.0, y + (cardH - 52.0) / 2.0, 52.0, Seals.MASTER, theme.gold, ink = true, theme = theme)
            kinText(Str.SEALS_MASTER_LABEL, 9.0, theme.gold, Fonts.uiMedium) {
                alignment = TextAlignment.TOP_LEFT
                position(24.0 + 80.0, y + 16.0)
            }
            kinText(Str.SEALS_MASTER_TITLE, 13.0, theme.ink, Fonts.uiSemiBold) {
                alignment = TextAlignment.TOP_LEFT
                position(24.0 + 80.0, y + 30.0)
            }
            kinText(Str.SEALS_MASTER_BODY, 11.0, theme.muted, Fonts.ui) {
                alignment = TextAlignment.TOP_LEFT
                position(24.0 + 80.0, y + 48.0)
            }
            y += cardH + 16.0
        }

        // Сетка 3×3
        val cellW = barW / 3.0
        val tileSize = 62.0
        Seals.COLLECTION.forEachIndexed { i, key ->
            val col = i % 3
            val rowI = i / 3
            val cx = 24.0 + col * cellW + cellW / 2.0
            val ty = y + rowI * 100.0
            val gotIt = key in stats.seals
            val tile = container { }
            addChild(tile)
            if (gotIt) {
                renderTile(tile, -tileSize / 2.0, 0.0, tileSize, key, theme.vermillion, ink = false, theme = theme)
                tile.rotation = tilt[i].degrees
            } else {
                tile.roundRect(
                    Size(tileSize, tileSize), RectCorners(6.0),
                    fill = Colors.TRANSPARENT, stroke = theme.line, strokeThickness = 1.0,
                ).position(-tileSize / 2.0, 0.0)
                tile.kinText(key, 30.0, theme.ink, Fonts.serifJp) {
                    alignment = TextAlignment.MIDDLE_CENTER
                    position(0.0, tileSize / 2.0)
                    alpha = 0.13
                }
            }
            tile.position(cx, ty)
            kinText(Str.sealName(key), 10.0, if (gotIt) theme.inkSoft else theme.muted, Fonts.ui) {
                alignment = TextAlignment.TOP_CENTER
                position(cx, ty + tileSize + 8.0)
                if (!gotIt) alpha = 0.65
            }
        }
        y += 3 * 100.0 + 6.0

        // Легенда — вне коллекции, скрыта до полного сбора
        solidRect(barW, 1.0, theme.line) { position(24.0, y) }
        y += 16.0
        val legendSize = 52.0
        roundRect(
            Size(legendSize, legendSize), RectCorners(6.0),
            fill = Colors.TRANSPARENT, stroke = theme.gold, strokeThickness = 1.0,
        ).apply {
            position(24.0, y)
            alpha = 0.85
        }
        if (complete) {
            kinText(Seals.LEGEND, 24.0, theme.gold, Fonts.serifJp) {
                alignment = TextAlignment.MIDDLE_CENTER
                position(24.0 + legendSize / 2.0, y + legendSize / 2.0 + 1.0)
                alpha = 0.75
            }
        } else {
            circle(4.0, theme.gold) {
                position(24.0 + legendSize / 2.0 - 4.0, y + legendSize / 2.0 - 4.0)
                alpha = 0.6
            }
        }
        val lx = 24.0 + legendSize + 14.0
        kinText(
            if (complete) Str.SEALS_LEGEND_LABEL_OUT else Str.SEALS_LEGEND_LABEL,
            9.0, theme.gold, Fonts.uiMedium,
        ) {
            alignment = TextAlignment.TOP_LEFT
            position(lx, y + 2.0)
        }
        kinText(
            if (complete) Str.SEALS_LEGEND_OPEN_TITLE else Str.SEALS_LEGEND_LOCKED_TITLE,
            13.0, theme.ink, Fonts.uiSemiBold,
        ) {
            alignment = TextAlignment.TOP_LEFT
            position(lx, y + 16.0)
        }
        kinText(
            if (complete) Str.SEALS_LEGEND_OPEN_BODY else Str.SEALS_LEGEND_LOCKED_BODY,
            11.0, theme.muted, Fonts.ui,
        ) {
            alignment = TextAlignment.TOP_LEFT
            position(lx, y + 34.0)
        }
        y += legendSize + 22.0

        kinText(
            if (complete) Str.SEALS_HINT_COMPLETE else Str.SEALS_HINT,
            Type.captionItalic.size, theme.muted, Fonts.serifItalic,
        ) {
            alignment = TextAlignment.TOP_CENTER
            position(w / 2.0, y)
        }
    }

    // Квадратная печать с иероглифом: киноварь (коллекция) или золото (極).
    private fun renderTile(
        host: Container, x: Double, y: Double, size: Double,
        key: String, bg: RGBA, ink: Boolean, theme: KinPalette,
    ) {
        host.roundRect(Size(size, size), RectCorners(6.0), fill = bg).position(x, y)
        host.kinText(
            key, size * 0.48,
            if (ink) Colors["#14130f"] else Colors["#f7f1e4"],
            Fonts.serifJp,
        ) {
            alignment = TextAlignment.MIDDLE_CENTER
            position(x + size / 2.0, y + size / 2.0 + 1.0)
        }
    }
}
