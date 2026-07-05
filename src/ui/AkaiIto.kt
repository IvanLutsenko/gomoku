package ui

import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.vector.*

// Акай ито — красная нить судьбы. Тонкая киноварная кривая: связывает
// соперников на игровом экране (горизонталь) и ведёт вдоль правого поля
// экрана победы (вертикаль). Пути — из дизайн-макета, масштабируются по длине.

private fun threadColor(theme: KinPalette): RGBA =
    RGBA(theme.vermillion.r, theme.vermillion.g, theme.vermillion.b, (255 * 0.4).toInt())

// Горизонтальная нить, макет 230×12.
fun Container.kinAkaiItoH(width: Double = 230.0, theme: KinPalette = Theme.colors): Container = container {
    val k = width / 230.0
    val shape = buildShape {
        stroke(
            ColorPaint(threadColor(theme)),
            info = StrokeInfo(thickness = 0.8, startCap = LineCap.ROUND, endCap = LineCap.ROUND),
        ) {
            moveTo(4.0 * k, 6.0)
            cubicTo(42.0 * k, 2.0, 78.0 * k, 10.0, 115.0 * k, 6.0)
            cubicTo(152.0 * k, 2.0, 192.0 * k, 2.0, 226.0 * k, 6.0)
        }
    }
    cpuGraphics(shape)
}

// Вертикальная нить, макет 16×380.
fun Container.kinAkaiItoV(height: Double = 380.0, theme: KinPalette = Theme.colors): Container = container {
    val k = height / 380.0
    val shape = buildShape {
        stroke(
            ColorPaint(RGBA(theme.vermillion.r, theme.vermillion.g, theme.vermillion.b, (255 * 0.3).toInt())),
            info = StrokeInfo(thickness = 0.8, startCap = LineCap.ROUND, endCap = LineCap.ROUND),
        ) {
            moveTo(8.0, 0.0)
            cubicTo(3.0, 60.0 * k, 13.0, 120.0 * k, 8.0, 190.0 * k)
            cubicTo(3.0, 260.0 * k, 4.0, 300.0 * k, 8.0, 380.0 * k)
        }
    }
    cpuGraphics(shape)
}
