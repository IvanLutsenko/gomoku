package scenes

import korlibs.event.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import ui.*

class HelpScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        kinPaperBackground(theme)

        onBackOrEscape { Nav.goMenu() }

        kinTextButton(Str.BACK, color = theme.muted) { Nav.goMenu() }
            .apply {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, 22.0)
            }

        // Контент скроллится вертикальным drag-ом (6 секций не влезают в 720).
        val content = container { }
        var y = 56.0

        content.kinText(Str.HELP_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0

        // Жила с ответвлением (Help-вариант)
        content.container {
            kinSeam(
                x1 = 2.0, y1 = 9.0, x2 = 108.0, y2 = 11.0,
                jitter = 3.0, seed = 53, width = 1.3, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y - 4.0)
        y += 16.0

        content.kinText(Str.HELP_SECTION, Type.labelCaps, theme.muted) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 36.0

        for (entry in Str.helpEntries) {
            renderEntry(content, y, entry, theme)
            y += entryHeight(entry, theme) + 16.0
        }

        val contentH = y + 24.0
        val minY = (h - contentH).coerceAtMost(0.0)
        var baseY = 0.0
        onMouseDrag { info ->
            if (info.start) baseY = content.y
            content.y = (baseY + info.dy).coerceIn(minY, 0.0)
        }
    }

    private fun renderEntry(host: Container, y: Double, entry: HelpEntry, theme: KinPalette) {
        // Иероглиф
        host.kinText(entry.num, Type.ideogram, theme.gold) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y - 2.0)
        }
        // Заголовок
        host.kinText(entry.title, 16.0, theme.ink, Fonts.uiSemiBold) {
            alignment = TextAlignment.TOP_LEFT
            position(72.0, y)
        }
        // Тело — word-wrap по фактическим метрикам шрифта
        val maxW = Viewport.W - 72.0 - 24.0
        val lines = wrap(entry.body, maxW, Fonts.ui, 14.0)
        lines.forEachIndexed { i, line ->
            host.kinText(line, 14.0, theme.inkSoft, Fonts.ui) {
                alignment = TextAlignment.TOP_LEFT
                position(72.0, y + 26.0 + i * 22.0)
            }
        }
    }

    private fun entryHeight(entry: HelpEntry, theme: KinPalette): Double {
        val maxW = Viewport.W - 72.0 - 24.0
        val lines = wrap(entry.body, maxW, Fonts.ui, 14.0)
        return 26.0 + lines.size * 22.0
    }
}

// Word-wrap по фактической ширине строки в шрифте (Font.getTextBounds).
private fun wrap(text: String, maxPx: Double, font: Font, size: Double): List<String> {
    fun width(s: String): Double = font.getTextBounds(size, s).bounds.width
    val out = mutableListOf<String>()
    var line = StringBuilder()
    for (word in text.split(" ")) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (width(candidate) > maxPx && line.isNotEmpty()) {
            out += line.toString()
            line = StringBuilder(word)
        } else {
            line = StringBuilder(candidate)
        }
    }
    if (line.isNotEmpty()) out += line.toString()
    return out
}
