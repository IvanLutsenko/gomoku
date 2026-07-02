package ui

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.math.hypot

// Резкий текст на любом DPI: TTF преобразуется в BitmapFont c physical-pixel
// размером (size × dpiScale), а Text-view рендерится с logical textSize. Atlas
// получается в native-resolution → 1:1 со screen-pixel-ом, без интерполяции.
//
// Default chars в `toBitmapFont` = LATIN_ALL без кириллицы. Расширяем:
// + кириллица оба регистра + ё + цифры/иероглифы/стрелки/спецсимволы из UI.
private val cyrillicAll = CharacterSet(
    "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
        "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
)
private val extraChars = CharacterSet("五目一二三四→☀☾·«»—–•★")
private val kinChars = CharacterSet.LATIN_ALL + cyrillicAll + extraChars

private val bitmapFontCache = mutableMapOf<Pair<Font, Double>, BitmapFont>()

private fun bitmapFontFor(font: Font, atlasSize: Double): BitmapFont {
    val key = font to atlasSize
    return bitmapFontCache.getOrPut(key) {
        font.toBitmapFont(fontSize = atlasSize, chars = kinChars)
    }
}

fun Container.kinText(
    label: String,
    size: Double,
    color: RGBA,
    font: Font,
    block: Text.() -> Unit = {},
): Text {
    val s = Display.dpiScale
    val atlasSize = size * s
    val bmpFont = bitmapFontFor(font, atlasSize)
    // textSize == bmpFont.fontSize → атлас отрисовывается 1:1 без внутреннего
    // scaling (метрики не плывут). Затем view.scale = 1/s даёт logical размер.
    return text(label, atlasSize, color, font = bmpFont).apply {
        scale = 1.0 / s
        smoothing = true
        block()
    }
}

fun Container.kinText(
    label: String,
    type: KinType,
    color: RGBA,
    block: Text.() -> Unit = {},
): Text = kinText(label, type.size, color, type.font(), block)

// ───────── Paper background ─────────
// Бумага + два мягких radial-wash-а (KinScreen из редизайна): свет — две тёмные
// «тени» по углам, тьма — тёплый блик и золотистое свечение. Битмап кешируется
// по теме, все сцены используют один.
private val paperCache = mutableMapOf<Boolean, Bitmap>()

fun Container.kinPaperBackground(theme: KinPalette = Theme.colors) {
    val w = Viewport.W.toDouble()
    val h = Viewport.H.toDouble()
    solidRect(w, h, theme.paper)
    val bm = paperCache.getOrPut(theme.isDark) {
        korlibs.image.bitmap.Bitmap32Context2d(Viewport.W, Viewport.H, true) {
            val radius = hypot(w, h) * 0.5
            fun wash(cx: Double, cy: Double, color: RGBA) {
                fill(
                    createRadialGradient(cx, cy, 0.0, cx, cy, radius).also {
                        it.addColorStop(0.0, color)
                        it.addColorStop(1.0, RGBA(color.r, color.g, color.b, 0))
                    },
                ) { rect(0.0, 0.0, w, h) }
            }
            if (theme.isDark) {
                wash(w * 0.2, h * 0.1, rgba255(255, 253, 245, 0.025))
                wash(w * 0.8, h * 0.9, rgba255(180, 138, 60, 0.05))
            } else {
                wash(w * 0.2, h * 0.1, rgba255(0, 0, 0, 0.025))
                wash(w * 0.8, h * 0.9, rgba255(0, 0, 0, 0.03))
            }
        }
    }
    image(bm)
}

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

    val mainText = kinText(label, typo, fgColor) {
        alignment = if (centered) TextAlignment.MIDDLE_CENTER else TextAlignment.MIDDLE_LEFT
    }
    mainText.position(
        if (centered) width / 2.0 else padH,
        height / 2.0,
    )

    if (rightArrow) {
        val arrowAlpha = if (primary) 0.7 else 0.5
        val arrowColor = RGBA(fgColor.r, fgColor.g, fgColor.b, (255 * arrowAlpha).toInt())
        kinText("→", 14.0, arrowColor, Fonts.ui) {
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
): Text = kinText(label, Type.body, color).apply {
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
    kinText(icon, 14.0, theme.ink, Fonts.ui) {
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
    val r = 10.0
    val cy = h / 2.0
    val cxOff = h / 2.0          // 12 — thumb-центр в left position
    val cxOn = w - h / 2.0       // 32 — thumb-центр в right position
    // shadow (рисуется первым, остаётся под thumb-ом)
    val shadow = circle(r, RGBA(0, 0, 0, 50)).apply {
        position((if (on) cxOn else cxOff) - r, cy - r + 1.0)
    }
    val thumb = circle(r, theme.paper).apply {
        position((if (on) cxOn else cxOff) - r, cy - r)
    }

    solidRect(w, h, Colors.TRANSPARENT).onClick {
        on = !on
        track.colorMul = if (on) theme.ink else trackOff
        val cx = if (on) cxOn else cxOff
        thumb.x = cx - r
        shadow.x = cx - r
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
        val tx = kinText(label, Type.caption.size, if (isActive) theme.paper else theme.ink, Fonts.uiMedium) {
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
    kinText(capsTracked(text, 1), Type.labelCaps.size, theme.muted, Fonts.uiMedium)

fun Container.kinRow(
    width: Double,
    label: String,
    last: Boolean = false,
    theme: KinPalette = Theme.colors,
    rightSlot: Container.() -> Unit,
): Container = container {
    val rowH = 48.0
    kinText(label, Type.body, theme.ink) {
        position(0.0, rowH / 2.0)
        alignment = TextAlignment.MIDDLE_LEFT
    }
    container {
        position(width, rowH / 2.0)
        rightSlot()
        // Сместим внутренний контент так, чтобы его правый край был на width.
        // Каждый child выравниваем по правому краю. scaledWidth/Height учитывают scale
        // (kinText scaled 1/dpi), поэтому именно их используем, а не unscaled*.
        forEachChild { ch ->
            ch.x -= ch.scaledWidth
            ch.y -= ch.scaledHeight / 2.0
        }
    }
    if (!last) {
        solidRect(width, 1.0, theme.line) { y = rowH }
    }
}
