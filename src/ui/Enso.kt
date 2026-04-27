package ui

import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.vector.*

// Энсо — незамкнутая окружность из суми-э, основной бренд-знак.
// Path взят из исходного дизайна (viewbox 84×84): начинается сверху, идёт по
// круговой траектории, не доходит до начала ~30° — пересечение «пустоты».
fun Container.kinEnso(
    pixelSize: Double = 96.0,
    theme: KinPalette = Theme.colors,
    seamSeed: Int = 3,
): Container = container {
    val viewBox = 84.0
    val s = pixelSize / viewBox
    scale(s)

    val shape = buildShape {
        stroke(
            ColorPaint(theme.ink),
            info = StrokeInfo(
                thickness = 3.5,
                startCap = LineCap.ROUND,
                endCap = LineCap.ROUND,
                join = LineJoin.ROUND,
            ),
        ) {
            moveTo(42.0, 10.0)
            cubicTo(22.0, 10.0, 10.0, 26.0, 10.0, 44.0)
            cubicTo(10.0, 62.0, 26.0, 74.0, 44.0, 74.0)
            cubicTo(62.0, 74.0, 74.0, 60.0, 72.0, 44.0)
        }
    }
    cpuGraphics(shape)

    // Золотая жила пересекает энсо по диагонали
    kinSeam(
        x1 = 18.0, y1 = 56.0, x2 = 66.0, y2 = 28.0,
        jitter = 3.0, seed = seamSeed, width = 1.6, branches = true,
        color = theme.gold, colorSoft = theme.goldSoft,
    )
}
