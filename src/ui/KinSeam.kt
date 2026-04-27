package ui

import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.korge.view.*
import korlibs.korge.view.vector.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlin.math.*

// Kintsugi gold seam — port of godot-handoff/godot/kin_seam.gd.
// Draws a smooth Catmull-Rom curve between two points with tapered jitter,
// in three layers (underglow / main / highlight). Optional branches and a
// gold pool. See docs/design/VEINS.md for placement specs.

// internal вместо private — чтобы можно было прогнать unit-тесты на детерминизм.
internal class KinRng(seed: Int) {
    private var s: Long = maxOf(seed, 1).toLong()
    fun nextDouble(): Double {
        s = (s * 9301 + 49297) % 233280
        return s / 233280.0
    }
}

internal fun buildPoints(
    p1: Point, p2: Point, jitter: Double, segments: Int, seed: Int,
): List<Point> {
    val rng = KinRng(seed)
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 0.001) return listOf(p1, p2)
    val px = -dy / len
    val py = dx / len
    val out = mutableListOf(p1)
    for (i in 1 until segments) {
        val t = i.toDouble() / segments
        val taper = sin(t * PI)
        val off = (rng.nextDouble() - 0.5) * 2.0 * jitter * taper
        out += Point(p1.x + dx * t + px * off, p1.y + dy * t + py * off)
    }
    out += p2
    return out
}

// Catmull-Rom interpolation through points → dense polyline.
internal fun samplePolyline(pts: List<Point>, samplesPerSegment: Int = 8): List<Point> {
    if (pts.size < 2) return emptyList()
    val out = mutableListOf<Point>()
    for (i in 0 until pts.size - 1) {
        val p0 = if (i > 0) pts[i - 1] else pts[i]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = if (i + 2 < pts.size) pts[i + 2] else p2
        for (s in 0 until samplesPerSegment) {
            val t = s.toDouble() / samplesPerSegment
            val t2 = t * t
            val t3 = t2 * t
            val x = 0.5 * ((2.0 * p1.x) + (-p0.x + p2.x) * t +
                (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 +
                (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3)
            val y = 0.5 * ((2.0 * p1.y) + (-p0.y + p2.y) * t +
                (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 +
                (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3)
            out += Point(x, y)
        }
    }
    out += pts.last()
    return out
}

private fun RGBA.scaleAlpha(k: Double): RGBA =
    RGBA(r, g, b, (a * k.coerceIn(0.0, 1.0)).toInt().coerceIn(0, 255))

private fun VectorBuilder.polyline(line: List<Point>) {
    if (line.isEmpty()) return
    moveTo(line[0])
    for (i in 1 until line.size) lineTo(line[i])
}

fun Container.kinSeam(
    x1: Double, y1: Double, x2: Double, y2: Double,
    width: Double = 1.4,
    jitter: Double = 2.5,
    seed: Int = 1,
    branches: Boolean = false,
    opacity: Double = 1.0,
    segments: Int = 8,
    color: RGBA = Theme.colors.gold,
    colorSoft: RGBA = Theme.colors.goldSoft,
): Container = container {
    val pts = buildPoints(Point(x1, y1), Point(x2, y2), jitter, segments, seed)
    val line = samplePolyline(pts, 8)

    val main = color.scaleAlpha(opacity)
    val soft = colorSoft.scaleAlpha(opacity)
    val glow = color.scaleAlpha(0.18 * opacity)

    val shape = buildShape {
        // 1) Underglow
        stroke(
            ColorPaint(glow),
            info = StrokeInfo(
                thickness = width * 2.6,
                startCap = LineCap.ROUND, endCap = LineCap.ROUND, join = LineJoin.ROUND,
            ),
        ) { polyline(line) }

        // 2) Main vein
        stroke(
            ColorPaint(main),
            info = StrokeInfo(
                thickness = width,
                startCap = LineCap.ROUND, endCap = LineCap.ROUND, join = LineJoin.ROUND,
            ),
        ) { polyline(line) }

        // 3) Highlight thread
        stroke(
            ColorPaint(soft),
            info = StrokeInfo(
                thickness = max(0.4, width * 0.4),
                startCap = LineCap.ROUND, endCap = LineCap.ROUND, join = LineJoin.ROUND,
            ),
        ) { polyline(line) }

        if (branches && pts.size >= 5) {
            val rng = KinRng(seed + 7)

            val bp1 = pts[2]
            val bp2 = pts[(pts.size * 0.66).toInt()]

            val dirRef = pts[3] - pts[1]
            val dirLen = max(0.001, dirRef.length)
            val ux = dirRef.x / dirLen
            val uy = dirRef.y / dirLen

            // Branch 1 — perpendicular up-ish, slightly randomized.
            val a1 = -PI / 2.0 - rng.nextDouble() * 0.4
            val d1x = ux * cos(a1) - uy * sin(a1)
            val d1y = ux * sin(a1) + uy * cos(a1)
            val l1 = 6.0 + rng.nextDouble() * 4.0
            val b1End = Point(bp1.x + d1x * l1, bp1.y + d1y * l1)
            stroke(
                ColorPaint(main),
                info = StrokeInfo(thickness = width * 0.7, startCap = LineCap.ROUND, endCap = LineCap.ROUND),
            ) { moveTo(bp1); lineTo(b1End) }

            // Branch 2 — opposite side.
            val a2 = PI / 2.0 + rng.nextDouble() * 0.4
            val d2x = ux * cos(a2) - uy * sin(a2)
            val d2y = ux * sin(a2) + uy * cos(a2)
            val l2 = 5.0 + rng.nextDouble() * 3.0
            val b2End = Point(bp2.x + d2x * l2, bp2.y + d2y * l2)
            stroke(
                ColorPaint(main.scaleAlpha(0.85)),
                info = StrokeInfo(thickness = width * 0.6, startCap = LineCap.ROUND, endCap = LineCap.ROUND),
            ) { moveTo(bp2); lineTo(b2End) }

            // Pool.
            val pool = pts[pts.size / 2]
            fill(ColorPaint(main)) { circle(pool, width * 0.9) }
            fill(ColorPaint(soft)) { circle(Point(pool.x - 0.5, pool.y - 0.5), width * 0.35) }
        }
    }
    cpuGraphics(shape)
}

// Convenience: vein with both endpoints relative to local container.
fun Container.kinVein(
    width: Double = 120.0,
    weight: Double = 1.4,
    seed: Int = 1,
    branches: Boolean = false,
    opacity: Double = 1.0,
): Container {
    val h = 16.0
    return kinSeam(
        x1 = 1.0, y1 = h / 2.0, x2 = width - 1.0, y2 = h / 2.0,
        width = weight, jitter = 2.5, seed = seed,
        branches = branches, opacity = opacity,
    )
}
