package scenes

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import ui.*

// «Чайная» — поддержка проекта: разовые покупки, без подписок.
// Правило дизайна: реклама никогда не появляется на доске и во время партии;
// «Тишина» убирает её навсегда.
// ponytail: покупки — заглушка (тихая строка «Покупки появятся в мобильной
// сборке»); подключить Google Play Billing / RuStore payments, когда появится
// платёжный SDK в мобильном таргете.
class TeaScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        kinPaperBackground(theme)
        onBackOrEscape { Nav.goMenu() }

        kinTextButton(Str.BACK, color = theme.muted) { Nav.goMenu() }.apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 22.0)
        }

        var y = 56.0
        kinText(Str.TEA_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0
        container {
            kinSeam(
                x1 = 2.0, y1 = 6.0, x2 = 94.0, y2 = 6.0,
                jitter = 2.5, seed = 91, width = 1.3,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y)
        y += 18.0
        kinText(Str.TEA_SECTION, Type.labelCaps.size, theme.muted, Fonts.uiMedium) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 24.0

        kinText(Str.TEA_INTRO, Type.captionItalic.size, theme.inkSoft, Fonts.serifItalic) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0

        // Тихая строка-заглушка вместо платёжного флоу
        val stub = kinText("", Type.caption.size, theme.muted, Fonts.ui) {
            alignment = TextAlignment.TOP_CENTER
            position(w / 2.0, h - 64.0)
        }

        Str.teaItems.forEachIndexed { i, item ->
            y = renderRow(this, y, w, theme, item, divider = i < Str.teaItems.size - 1) {
                stub.text = Str.TEA_STUB
            }
        }

        kinText(Str.TEA_FOOTER, Type.captionItalic.size, theme.muted, Fonts.serifItalic) {
            alignment = TextAlignment.TOP_CENTER
            position(w / 2.0, y + 14.0)
        }
    }

    private fun renderRow(
        host: Container, y: Double, w: Double, theme: KinPalette,
        item: TeaItem, divider: Boolean, onBuy: () -> Unit,
    ): Double {
        val rowH = 66.0
        val soon = item.tone == TeaTone.SOON
        val row = host.container { }.position(24.0, y)
        val rowW = w - 48.0
        if (soon) row.alpha = 0.6

        // Плитка-иероглиф
        val tile = 42.0
        val tileBg = when (item.tone) {
            TeaTone.VERMILLION -> theme.vermillion
            TeaTone.GOLD -> theme.gold
            TeaTone.SILVER -> Colors["#9aa0ab"]
            TeaTone.COPPER -> Colors["#a86b4c"]
            TeaTone.SOON -> Colors.TRANSPARENT
        }
        val tileY = (rowH - tile) / 2.0 - 4.0
        if (soon) {
            row.roundRect(
                Size(tile, tile), RectCorners(5.0),
                fill = Colors.TRANSPARENT, stroke = theme.lineFirm, strokeThickness = 1.0,
            ).position(0.0, tileY)
        } else {
            row.roundRect(Size(tile, tile), RectCorners(5.0), fill = tileBg).position(0.0, tileY)
        }
        val kanjiColor = when {
            soon -> theme.muted
            item.tone == TeaTone.GOLD -> Colors["#14130f"]
            else -> Colors["#f7f1e4"]
        }
        row.kinText(item.kanji, 20.0, kanjiColor, Fonts.serifJp) {
            alignment = TextAlignment.MIDDLE_CENTER
            position(tile / 2.0, tileY + tile / 2.0 + 1.0)
        }

        val textX = tile + 14.0
        row.kinText(item.name, 14.0, theme.ink, Fonts.uiSemiBold) {
            alignment = TextAlignment.TOP_LEFT
            position(textX, tileY + 4.0)
        }
        row.kinText(item.sub, 11.0, theme.muted, Fonts.ui) {
            alignment = TextAlignment.TOP_LEFT
            position(textX, tileY + 23.0)
        }

        if (soon) {
            row.kinText(Str.TEA_SOON, 11.0, theme.muted, Fonts.uiMedium) {
                alignment = TextAlignment.MIDDLE_RIGHT
                position(rowW, rowH / 2.0 - 4.0)
            }
        } else {
            val btnW = 74.0
            val btnH = 34.0
            val btn = row.container { }.position(rowW - btnW, (rowH - btnH) / 2.0 - 4.0)
            btn.roundRect(
                Size(btnW, btnH), RectCorners(BtnSpec.RADIUS - 2.0),
                fill = Colors.TRANSPARENT, stroke = theme.lineFirm, strokeThickness = 1.0,
            )
            btn.kinText(item.price ?: "", 12.5, theme.ink, Fonts.uiSemiBold) {
                alignment = TextAlignment.MIDDLE_CENTER
                position(btnW / 2.0, btnH / 2.0)
            }
            btn.solidRect(btnW, btnH, Colors.TRANSPARENT).onClick { onBuy() }
        }

        if (divider) {
            host.solidRect(rowW, 1.0, theme.line) { position(24.0, y + rowH - 6.0) }
        }
        return y + rowH
    }
}
