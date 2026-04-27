package scenes

import korlibs.event.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import ui.*

private data class HelpEntry(val num: String, val title: String, val body: String)

private val HELP_ENTRIES = listOf(
    HelpEntry("一", "Цель игры", "Выстройте пять камней в ряд по горизонтали, вертикали или диагонали. Чёрные ходят первыми."),
    HelpEntry("二", "Управление", "Касание — поставить камень. Отменить последний ход можно из нижней панели."),
    HelpEntry("三", "Стратегия", "Блокируйте открытые тройки соперника и стройте «вилки» — двойные угрозы, которые нельзя закрыть одним ходом."),
    HelpEntry("四", "Центр", "Первый ход — традиционно тэнгэн, центр доски. Это даёт максимум направлений атаки."),
    HelpEntry("五", "Кинцуги", "Победная линия выкладывается золотом — как трещины в керамике, которые становятся украшением."),
)

class HelpScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        solidRect(w, h, theme.paper)

        keys.down {
            if (it.key == Key.BACK || it.key == Key.ESCAPE) Nav.goMenu()
        }

        kinTextButton("← Назад", color = theme.muted) { Nav.goMenu() }
            .apply {
                alignment = TextAlignment.TOP_LEFT
                position(24.0, 22.0)
            }

        var y = 56.0

        text("Помощь", Type.title.size, theme.ink, font = Fonts.serif) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0

        // Жила с ответвлением (Help-вариант)
        container {
            kinSeam(
                x1 = 2.0, y1 = 9.0, x2 = 108.0, y2 = 11.0,
                jitter = 3.0, seed = 53, width = 1.3, branches = true,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y - 4.0)
        y += 16.0

        text("ПРАВИЛА И СОВЕТЫ", Type.labelCaps.size, theme.muted, font = Fonts.uiMedium) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 36.0

        for (entry in HELP_ENTRIES) {
            renderEntry(this, y, entry, theme)
            y += entryHeight(entry, theme) + 16.0
        }
    }

    private fun renderEntry(host: Container, y: Double, entry: HelpEntry, theme: KinPalette) {
        // Иероглиф
        host.text(entry.num, Type.ideogram.size, theme.gold, font = Fonts.serifJp) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y - 2.0)
        }
        // Заголовок
        host.text(entry.title, 16.0, theme.ink, font = Fonts.uiSemiBold) {
            alignment = TextAlignment.TOP_LEFT
            position(72.0, y)
        }
        // Тело — простой word-wrap по словам, ширина 264
        val maxW = Viewport.W - 72.0 - 24.0
        val lines = wrap(entry.body, maxW.toInt(), perCharPx = 7)
        lines.forEachIndexed { i, line ->
            host.text(line, 14.0, theme.inkSoft, font = Fonts.ui) {
                alignment = TextAlignment.TOP_LEFT
                position(72.0, y + 26.0 + i * 22.0)
            }
        }
    }

    private fun entryHeight(entry: HelpEntry, theme: KinPalette): Double {
        val maxW = Viewport.W - 72.0 - 24.0
        val lines = wrap(entry.body, maxW.toInt(), perCharPx = 7)
        return 26.0 + lines.size * 22.0
    }
}

// Тривиальный word-wrap: предполагаем фиксированную среднюю ширину символа.
// Inter 14 ~ 7 px/char для русского текста. Хватает для дизайн-целей.
private fun wrap(text: String, maxPx: Int, perCharPx: Int): List<String> {
    val maxChars = maxPx / perCharPx
    val out = mutableListOf<String>()
    var line = StringBuilder()
    for (word in text.split(" ")) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (candidate.length > maxChars && line.isNotEmpty()) {
            out += line.toString()
            line = StringBuilder(word)
        } else {
            line = StringBuilder(candidate)
        }
    }
    if (line.isNotEmpty()) out += line.toString()
    return out
}
