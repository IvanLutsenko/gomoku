import korlibs.math.geom.Point
import ui.KinRng
import ui.buildPoints
import ui.samplePolyline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KinSeamTest {

    @Test fun rng_is_deterministic_per_seed() {
        val a = KinRng(42)
        val b = KinRng(42)
        repeat(100) {
            assertEquals(a.nextDouble(), b.nextDouble(), "step $it")
        }
    }

    @Test fun rng_different_seeds_diverge() {
        val a = KinRng(1)
        val b = KinRng(2)
        // Хотя бы за 10 шагов должны разойтись.
        var diverged = false
        repeat(10) {
            if (a.nextDouble() != b.nextDouble()) diverged = true
        }
        assertTrue(diverged)
    }

    @Test fun rng_output_in_unit_interval() {
        val rng = KinRng(7)
        repeat(500) {
            val v = rng.nextDouble()
            assertTrue(v >= 0.0 && v < 1.0, "value $v out of [0,1)")
        }
    }

    @Test fun rng_zero_seed_handled() {
        // KinRng(0) внутри использует max(seed, 1) — не должно зацикливаться.
        val rng = KinRng(0)
        val v1 = rng.nextDouble()
        val v2 = rng.nextDouble()
        assertNotEquals(v1, v2)
    }

    @Test fun buildPoints_endpoints_intact() {
        val p1 = Point(0.0, 0.0)
        val p2 = Point(100.0, 0.0)
        val pts = buildPoints(p1, p2, jitter = 5.0, segments = 8, seed = 1)
        assertEquals(p1, pts.first())
        assertEquals(p2, pts.last())
    }

    @Test fun buildPoints_count_matches_segments_plus_one() {
        val pts = buildPoints(Point(0.0, 0.0), Point(50.0, 50.0), 3.0, segments = 8, seed = 5)
        assertEquals(9, pts.size) // segments + 1 (start + intermediate + end)
    }

    @Test fun buildPoints_zero_length_short_circuits() {
        val p = Point(10.0, 10.0)
        val pts = buildPoints(p, p, jitter = 5.0, segments = 8, seed = 1)
        assertEquals(2, pts.size)
        assertEquals(p, pts[0])
        assertEquals(p, pts[1])
    }

    @Test fun buildPoints_deterministic_per_seed() {
        val a = buildPoints(Point(0.0, 0.0), Point(100.0, 30.0), 5.0, 10, seed = 99)
        val b = buildPoints(Point(0.0, 0.0), Point(100.0, 30.0), 5.0, 10, seed = 99)
        assertEquals(a, b)
    }

    @Test fun buildPoints_different_seeds_produce_different_paths() {
        val a = buildPoints(Point(0.0, 0.0), Point(100.0, 30.0), 5.0, 10, seed = 1)
        val b = buildPoints(Point(0.0, 0.0), Point(100.0, 30.0), 5.0, 10, seed = 2)
        // Endpoints совпадают, но хотя бы одна внутренняя точка должна
        // отличаться.
        assertEquals(a.first(), b.first())
        assertEquals(a.last(), b.last())
        val differs = (1 until a.size - 1).any { a[it] != b[it] }
        assertTrue(differs)
    }

    @Test fun samplePolyline_yields_dense_curve() {
        val pts = buildPoints(Point(0.0, 0.0), Point(80.0, 0.0), 0.0, segments = 8, seed = 1)
        val poly = samplePolyline(pts, samplesPerSegment = 8)
        // 8 segments × 8 samples + 1 endpoint = 65
        assertEquals(8 * 8 + 1, poly.size)
        assertEquals(pts.first(), poly.first())
        assertEquals(pts.last(), poly.last())
    }

    @Test fun samplePolyline_handles_too_few_input_points() {
        assertEquals(emptyList(), samplePolyline(emptyList()))
        assertEquals(emptyList(), samplePolyline(listOf(Point(0.0, 0.0))))
    }
}
