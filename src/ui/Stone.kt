package ui

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.view.*
import korlibs.math.geom.*

// Камень рисуется один раз в Bitmap32 с настоящим радиальным градиентом
// (Context2d.createRadialGradient) и кешируется по теме/радиусу — все
// 200+ камней на доске и в UI-индикаторах используют один битмап.
private val stoneCache = mutableMapOf<String, Bitmap>()

private fun stoneBitmap(palette: StonePalette, radius: Double): Bitmap {
    val key = buildString {
        append(palette.outer.value); append('_')
        append(palette.inner.value); append('_')
        append(palette.border?.value ?: 0); append('_')
        append(if (palette.needsLightOutline) 1 else 0); append('_')
        append((radius * 10).toInt())
    }
    stoneCache[key]?.let { return it }

    val pad = 6.0
    val size = ((radius + pad) * 2).toInt().coerceAtLeast(8)
    val cx = size / 2.0
    val cy = size / 2.0

    val bm = Bitmap32Context2d(size, size, true) {
        // Soft drop shadow
        fill(
            createRadialGradient(
                cx, cy + 1.5, 0.0, cx, cy + 1.5, radius * 1.15,
            ).also {
                it.addColorStop(0.0, RGBA(0, 0, 0, 90))
                it.addColorStop(0.65, RGBA(0, 0, 0, 70))
                it.addColorStop(1.0, RGBA(0, 0, 0, 0))
            },
        ) {
            circle(Point(cx, cy + 1.5), radius * 1.15)
        }

        // Main body — radial gradient with light from upper-left
        val hx = cx - radius * 0.30
        val hy = cy - radius * 0.32
        fill(
            createRadialGradient(
                hx, hy, 0.0, cx, cy, radius,
            ).also {
                it.addColorStop(0.0, palette.inner)
                it.addColorStop(0.55, mix(palette.inner, palette.outer, 0.55))
                it.addColorStop(1.0, palette.outer)
            },
        ) {
            circle(Point(cx, cy), radius)
        }

        // Specular highlight — small bright dot upper-left
        fill(
            createRadialGradient(
                hx, hy, 0.0, hx, hy, radius * 0.42,
            ).also {
                it.addColorStop(0.0, RGBA(255, 255, 255, 90))
                it.addColorStop(1.0, RGBA(255, 255, 255, 0))
            },
        ) {
            circle(Point(hx, hy), radius * 0.42)
        }

        // Border (white stones)
        palette.border?.let { borderColor ->
            stroke(
                ColorPaint(borderColor),
                lineWidth =0.7,
            ) {
                circle(Point(cx, cy), radius - 0.3)
            }
        }

        // Light outline for dark-theme black stones (читаемость на ink-фоне)
        if (palette.needsLightOutline) {
            stroke(
                ColorPaint(rgba255(255, 253, 245, 0.45)),
                lineWidth =0.5,
            ) {
                circle(Point(cx, cy), radius - 0.25)
            }
        }
    }

    stoneCache[key] = bm
    return bm
}

fun Container.kinStone(
    isBlack: Boolean,
    radius: Double,
    isLast: Boolean = false,
    isWin: Boolean = false,
    theme: KinPalette = Theme.colors,
): Container = container {
    val palette = if (isBlack) blackStonePalette(theme) else whiteStonePalette(theme)
    val bm = stoneBitmap(palette, radius)
    image(bm).apply {
        anchor(0.5, 0.5)
    }

    if (isWin) {
        circle(
            radius = radius + 1.0,
            fill = Colors.TRANSPARENT,
            stroke = theme.gold,
            strokeThickness = 1.6,
        ).apply { position(-radius - 1.0, -radius - 1.0) }
    }

    if (isLast && !isWin) {
        val dotColor = if (isBlack) theme.paper else theme.vermillion
        circle(radius * 0.20, dotColor).apply {
            position(-radius * 0.20, -radius * 0.20)
        }
    }
}

fun rgba255(r: Int, g: Int, b: Int, alpha: Double): RGBA =
    RGBA(r, g, b, (alpha.coerceIn(0.0, 1.0) * 255).toInt())

internal fun mix(a: RGBA, b: RGBA, t: Double): RGBA {
    val k = t.coerceIn(0.0, 1.0)
    return RGBA(
        (a.r + (b.r - a.r) * k).toInt(),
        (a.g + (b.g - a.g) * k).toInt(),
        (a.b + (b.b - a.b) * k).toInt(),
        (a.a + (b.a - a.a) * k).toInt(),
    )
}
