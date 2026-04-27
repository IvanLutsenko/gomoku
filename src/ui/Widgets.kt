package ui

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*

// Тонкие ребра 1 px цветом line_firm — рамка кнопки/сегмента/поля.
private fun Container.borderRect(width: Double, height: Double, color: RGBA) {
    solidRect(width, 1.0, color)
    solidRect(width, 1.0, color) { y = height - 1.0 }
    solidRect(1.0, height, color)
    solidRect(1.0, height, color) { x = width - 1.0 }
}

// ───────── Buttons ─────────

fun Container.kinButton(
    width: Double,
    label: String,
    primary: Boolean = false,
    small: Boolean = false,
    rightArrow: Boolean = false,
    centered: Boolean = small,
    enabled: Boolean = true,
    theme: KinPalette = Theme.colors,
    onPress: () -> Unit,
): Container = container {
    val height = if (small) 44.0 else 52.0
    val padH = if (small) 16.0 else 24.0
    val typo = if (small) Type.buttonSmall else Type.button

    val bgColor = if (primary) theme.ink else Colors.TRANSPARENT
    val fgColor = if (primary) theme.paper else theme.ink

    solidRect(width, height, bgColor)
    if (!primary) borderRect(width, height, theme.lineFirm)

    val mainText = text(label, typo.size, fgColor, font = typo.font()) {
        alignment = if (centered) TextAlignment.MIDDLE_CENTER else TextAlignment.MIDDLE_LEFT
    }
    mainText.position(
        if (centered) width / 2.0 else padH,
        height / 2.0,
    )

    if (rightArrow) {
        val arrowAlpha = if (primary) 0.7 else 0.5
        val arrowColor = RGBA(fgColor.r, fgColor.g, fgColor.b, (255 * arrowAlpha).toInt())
        text("→", 14.0, arrowColor, font = Fonts.ui) {
            alignment = TextAlignment.MIDDLE_RIGHT
            position(width - padH, height / 2.0)
        }
    }

    if (enabled) {
        onClick { onPress() }
    } else {
        alpha = 0.4
    }
}

// «← Назад», «Тема» — простые тексты-ссылки.
fun Container.kinTextButton(
    label: String,
    color: RGBA = Theme.colors.muted,
    onPress: () -> Unit,
): Text = text(label, Type.body.size, color, font = Type.body.font()).apply {
    onClick { onPress() }
}

// Круглая 36×36 кнопка для переключения темы (☀/☾).
fun Container.kinIconRound(
    icon: String,
    theme: KinPalette = Theme.colors,
    onPress: () -> Unit,
): Container = container {
    val size = 36.0
    circle(size / 2.0, Colors.TRANSPARENT, stroke = theme.lineFirm, strokeThickness = 1.0) {
        position(size / 2.0, size / 2.0)
    }
    text(icon, 14.0, theme.ink, font = Fonts.ui) {
        alignment = TextAlignment.MIDDLE_CENTER
        position(size / 2.0, size / 2.0)
    }
    // hit-target — невидимый прямоугольник на всю площадь
    solidRect(size, size, Colors.TRANSPARENT).onClick { onPress() }
}

// ───────── Toggle 44×24 ─────────

fun Container.kinToggle(
    initialOn: Boolean = false,
    theme: KinPalette = Theme.colors,
    onChange: (Boolean) -> Unit,
): Container = container {
    var on = initialOn
    val w = 44.0
    val h = 24.0

    val trackOff = if (theme.isDark) rgba255(255, 253, 245, 0.18) else rgba255(0, 0, 0, 0.15)

    val track = roundRect(Size(w, h), RectCorners(h / 2.0), fill = if (on) theme.ink else trackOff)
    val thumb = circle(10.0, theme.paper) {
        position(if (on) 32.0 else 12.0, 12.0)
    }
    // shadow
    circle(10.0, RGBA(0, 0, 0, 50)).apply {
        position(if (on) 32.0 else 12.0, 13.0)
        sendChildToBack(this)  // appears under thumb
    }

    solidRect(w, h, Colors.TRANSPARENT).onClick {
        on = !on
        track.colorMul = if (on) theme.ink else trackOff
        thumb.x = if (on) 32.0 else 12.0
        onChange(on)
    }
}

// ───────── Segmented control ─────────

fun Container.kinSegmented(
    items: List<String>,
    initialIndex: Int = 0,
    totalWidth: Double,
    theme: KinPalette = Theme.colors,
    onChange: (Int) -> Unit,
): Container = container {
    val height = 42.0
    val itemWidth = totalWidth / items.size
    var current = initialIndex

    val bgs = mutableListOf<SolidRect>()
    val txts = mutableListOf<Text>()

    items.forEachIndexed { i, label ->
        val isActive = i == current
        val bg = solidRect(itemWidth, height, if (isActive) theme.ink else Colors.TRANSPARENT) {
            x = i * itemWidth
        }
        bgs += bg
        if (i > 0) {
            solidRect(1.0, height, theme.lineFirm) { x = i * itemWidth }
        }
        val tx = text(label, Type.caption.size, if (isActive) theme.paper else theme.ink, font = Fonts.uiMedium) {
            alignment = TextAlignment.MIDDLE_CENTER
            position(i * itemWidth + itemWidth / 2.0, height / 2.0)
        }
        txts += tx
    }

    // Внешняя рамка поверх
    borderRect(totalWidth, height, theme.lineFirm)

    // Hit-targets поверх (после фонов и рамки чтобы клики не блокировались)
    items.forEachIndexed { i, _ ->
        solidRect(itemWidth, height, Colors.TRANSPARENT) {
            x = i * itemWidth
            onClick {
                if (current == i) return@onClick
                current = i
                bgs.forEachIndexed { j, b -> b.color = if (j == current) theme.ink else Colors.TRANSPARENT }
                txts.forEachIndexed { j, t -> t.color = if (j == current) theme.paper else theme.ink }
                onChange(i)
            }
        }
    }
}

// ───────── Field label (caps) и ряд настроек ─────────

fun Container.kinFieldLabel(text: String, theme: KinPalette = Theme.colors): Text =
    text(capsTracked(text, 1), Type.labelCaps.size, theme.muted, font = Fonts.uiMedium)

fun Container.kinRow(
    width: Double,
    label: String,
    last: Boolean = false,
    theme: KinPalette = Theme.colors,
    rightSlot: Container.() -> Unit,
): Container = container {
    val rowH = 48.0
    text(label, Type.body.size, theme.ink, font = Fonts.ui) {
        position(0.0, rowH / 2.0)
        alignment = TextAlignment.MIDDLE_LEFT
    }
    container {
        position(width, rowH / 2.0)
        rightSlot()
        // Сместим внутренний контент так, чтобы его правый край был на width.
        // Каждый child выравниваем по правому краю.
        forEachChild { ch ->
            ch.x -= ch.unscaledWidth
            ch.y -= ch.unscaledHeight / 2.0
        }
    }
    if (!last) {
        solidRect(width, 1.0, theme.line) { y = rowH }
    }
}
