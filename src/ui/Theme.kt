package ui

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.file.std.*

// Design tokens — see docs/design/DESIGN_SYSTEM.md and tokens/colors.json.
// Palette mirrors the JSON source-of-truth exactly.

data class KinPalette(
    val paper: RGBA,
    val paperDeep: RGBA,
    val surface: RGBA,
    val ink: RGBA,
    val inkSoft: RGBA,
    val muted: RGBA,
    val line: RGBA,
    val lineFirm: RGBA,
    val vermillion: RGBA,
    val gold: RGBA,
    val goldSoft: RGBA,
    val isDark: Boolean,
)

private fun rgba(hex: String, alpha: Double = 1.0): RGBA {
    val c = Colors[hex]
    return RGBA(c.r, c.g, c.b, (alpha.coerceIn(0.0, 1.0) * 255).toInt())
}

val KIN_LIGHT = KinPalette(
    paper      = rgba("#f5f0e6"),
    paperDeep  = rgba("#ede5d4"),
    surface    = rgba("#faf6ec"),
    ink        = rgba("#1a1814"),
    inkSoft    = rgba("#3a3530"),
    muted      = rgba("#8a8378"),
    line       = rgba("#1a1814", 0.10),
    lineFirm   = rgba("#1a1814", 0.32),
    vermillion = rgba("#c1442a"),
    gold       = rgba("#b48a3c"),
    goldSoft   = rgba("#d9b46a"),
    isDark     = false,
)

val KIN_DARK = KinPalette(
    paper      = rgba("#14130f"),
    paperDeep  = rgba("#2a2722"),
    surface    = rgba("#0e0d0a"),
    ink        = rgba("#f0ede4"),
    inkSoft    = rgba("#bdb8ad"),
    muted      = rgba("#6a6760"),
    line       = rgba("#fffdf5", 0.08),
    lineFirm   = rgba("#fffdf5", 0.22),
    vermillion = rgba("#d35a3e"),
    gold       = rgba("#d4a652"),
    goldSoft   = rgba("#e9c476"),
    isDark     = true,
)

// Stones (theme-dependent — black-on-dark needs special treatment).
data class StonePalette(
    val outer: RGBA,
    val inner: RGBA,
    val border: RGBA?,
    val needsLightOutline: Boolean = false,
)

fun blackStonePalette(theme: KinPalette) = if (theme.isDark) {
    StonePalette(
        outer = rgba("#1a1814"),
        inner = rgba("#9a8f80"),
        border = null,
        needsLightOutline = true,
    )
} else {
    StonePalette(
        outer = rgba("#0d0c0a"),
        inner = rgba("#4a4640"),
        border = null,
    )
}

fun whiteStonePalette(@Suppress("UNUSED_PARAMETER") theme: KinPalette) = StonePalette(
    outer = rgba("#d8d2c4"),
    inner = rgba("#ffffff"),
    border = rgba("#000000", 0.18),
)

// Тема — view над SettingsStore. Источник истины — `SettingsStore.current.dark`,
// чтобы при загрузке настроек не нужно было синхронизировать руками.
// Сцены не подписываются на изменения — любой переключатель темы делает
// `Theme.toggle()` + `Nav.goXxx()`, новая сцена читает свежие значения.
object Theme {
    val dark: Boolean get() = logic.SettingsStore.current.dark
    val colors: KinPalette get() = if (dark) KIN_DARK else KIN_LIGHT

    fun set(dark: Boolean) {
        if (this.dark == dark) return
        logic.SettingsStore.update { it.copy(dark = dark) }
    }

    fun toggle() = set(!dark)
}

// Loaded once at app start. All scenes read from here.
object Fonts {
    lateinit var serif: Font
    lateinit var serifItalic: Font
    lateinit var serifSemi: Font
    lateinit var serifJp: Font
    lateinit var ui: Font
    lateinit var uiMedium: Font
    lateinit var uiSemiBold: Font

    private var loaded = false

    suspend fun loadOnce() {
        if (loaded) return
        serif        = resourcesVfs["fonts/NotoSerif-Regular.ttf"].readFont()
        serifItalic  = resourcesVfs["fonts/NotoSerif-Italic.ttf"].readFont()
        serifSemi    = resourcesVfs["fonts/NotoSerif-SemiBold.ttf"].readFont()
        serifJp      = resourcesVfs["fonts/NotoSerifJP-Regular.ttf"].readFont()
        ui           = resourcesVfs["fonts/Inter-Regular.ttf"].readFont()
        uiMedium     = resourcesVfs["fonts/Inter-Medium.ttf"].readFont()
        uiSemiBold   = resourcesVfs["fonts/Inter-SemiBold.ttf"].readFont()
        loaded = true
    }
}

// Type styles from tokens/typography.json. Rendered through text(text, size, color, font).
data class KinType(val font: () -> Font, val size: Double)

object Type {
    val display       get() = KinType({ Fonts.serifJp },     56.0)
    val title         get() = KinType({ Fonts.serif },       36.0)
    val subtitle      get() = KinType({ Fonts.serif },       22.0)
    val body          get() = KinType({ Fonts.ui },          14.0)
    val bodyStrong    get() = KinType({ Fonts.uiSemiBold },  14.0)
    val button        get() = KinType({ Fonts.uiMedium },    16.0)
    val buttonSmall   get() = KinType({ Fonts.uiMedium },    14.0)
    val caption       get() = KinType({ Fonts.ui },          13.0)
    val captionItalic get() = KinType({ Fonts.serifItalic }, 13.0)
    val meta          get() = KinType({ Fonts.uiMedium },    11.0)
    val labelCaps     get() = KinType({ Fonts.uiMedium },    12.0)
    val trackingBrand get() = KinType({ Fonts.uiMedium },    12.0)
    val ideogram      get() = KinType({ Fonts.serifJp },     28.0)
}

object Spacing {
    const val XS = 4.0
    const val SM = 8.0
    const val MD = 14.0
    const val LG = 24.0
    const val XL = 32.0
    const val XXL = 40.0
    const val SCREEN_PAD_H = 24.0
    const val SCREEN_PAD_TOP = 20.0
    const val SCREEN_PAD_BOTTOM = 28.0
}

object BoardSpec {
    const val SIZE_CELLS = 15
    const val CELL = 22.0
    const val PADDING = 14.0
    const val STONE_RADIUS_RATIO = 0.42
    const val HOSHI_RADIUS = 1.8
    val HOSHI = listOf(3 to 3, 3 to 11, 7 to 7, 11 to 3, 11 to 11)
    const val GRID_STROKE = 0.6
    const val WIN_OUTLINE = 1.5
    const val TOTAL: Double = PADDING * 2 + CELL * (SIZE_CELLS - 1)
}

object Viewport {
    const val W = 360
    const val H = 720
}

// Caps tracking helper — KorGE Text doesn't expose letter-spacing, so we
// emulate the "G O M O K U" effect by inserting spaces. For tracking values
// like 1.5/2 we just use a single space; the design uses much wider spacing
// only in the brand mark itself, which already has explicit spaces.
fun capsTracked(text: String, intensity: Int = 1): String =
    text.uppercase().map { ch -> ch.toString() }
        .joinToString(" ".repeat(intensity))
