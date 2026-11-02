package dev.nullcode.mediamagnet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class TonalPalette(private val hue: Double, private val chroma: Double) {
    private val cache = HashMap<Int, Color>()

    operator fun get(tone: Int): Color = cache.getOrPut(tone) { lchToColor(tone.toDouble(), chroma, hue) }
}

class AppPalettes(hue: Float, monochrome: Boolean) {
    private val h = ((hue % 360f) + 360f) % 360f
    private val primaryChroma = if (monochrome) 0.0 else 58.0
    private val secondaryChroma = if (monochrome) 0.0 else 22.0
    private val tertiaryChroma = if (monochrome) 0.0 else 56.0

    val primary = TonalPalette(h.toDouble(), primaryChroma)
    val secondary = TonalPalette(h.toDouble(), secondaryChroma)
    val tertiary = TonalPalette(((h + 96f) % 360f).toDouble(), tertiaryChroma)
    val neutral = TonalPalette(h.toDouble(), 4.0)
    val neutralVariant = TonalPalette(h.toDouble(), 10.0)
    val error = TonalPalette(30.0, 58.0)
}

fun AppPalettes.lightScheme(): ColorScheme = lightColorScheme(
    primary = primary[40],
    onPrimary = primary[100],
    primaryContainer = primary[90],
    onPrimaryContainer = primary[10],
    inversePrimary = primary[80],

    secondary = secondary[40],
    onSecondary = secondary[100],
    secondaryContainer = secondary[90],
    onSecondaryContainer = secondary[10],

    tertiary = tertiary[40],
    onTertiary = tertiary[100],
    tertiaryContainer = tertiary[90],
    onTertiaryContainer = tertiary[10],

    background = neutral[99],
    onBackground = neutral[10],
    surface = neutral[99],
    onSurface = neutral[10],
    surfaceVariant = neutralVariant[90],
    onSurfaceVariant = neutralVariant[30],
    surfaceTint = primary[40],

    inverseSurface = neutral[20],
    inverseOnSurface = neutral[95],

    error = error[40],
    onError = error[100],
    errorContainer = error[90],
    onErrorContainer = error[10],

    outline = neutralVariant[50],
    outlineVariant = neutralVariant[80],
    scrim = neutral[0],

    surfaceBright = neutral[98],
    surfaceDim = neutral[87],
    surfaceContainerLowest = neutral[100],
    surfaceContainerLow = neutral[96],
    surfaceContainer = neutral[94],
    surfaceContainerHigh = neutral[92],
    surfaceContainerHighest = neutral[90],
)

fun AppPalettes.darkScheme(amoled: Boolean): ColorScheme = darkColorScheme(
    primary = primary[80],
    onPrimary = primary[20],
    primaryContainer = primary[30],
    onPrimaryContainer = primary[90],
    inversePrimary = primary[40],

    secondary = secondary[80],
    onSecondary = secondary[20],
    secondaryContainer = secondary[30],
    onSecondaryContainer = secondary[90],

    tertiary = tertiary[80],
    onTertiary = tertiary[20],
    tertiaryContainer = tertiary[30],
    onTertiaryContainer = tertiary[90],

    background = if (amoled) neutral[0] else neutral[6],
    onBackground = neutral[90],
    surface = if (amoled) neutral[0] else neutral[6],
    onSurface = neutral[90],
    surfaceVariant = neutralVariant[30],
    onSurfaceVariant = neutralVariant[80],
    surfaceTint = primary[80],

    inverseSurface = neutral[90],
    inverseOnSurface = neutral[20],

    error = error[80],
    onError = error[20],
    errorContainer = error[30],
    onErrorContainer = error[90],

    outline = neutralVariant[60],
    outlineVariant = neutralVariant[30],
    scrim = neutral[0],

    surfaceBright = neutral[24],
    surfaceDim = if (amoled) neutral[0] else neutral[6],
    surfaceContainerLowest = if (amoled) neutral[0] else neutral[4],
    surfaceContainerLow = if (amoled) neutral[4] else neutral[10],
    surfaceContainer = if (amoled) neutral[6] else neutral[12],
    surfaceContainerHigh = if (amoled) neutral[10] else neutral[17],
    surfaceContainerHighest = if (amoled) neutral[12] else neutral[22],
)

// --- CIELAB -> sRGB -------------------------------------------------------

private fun lchToColor(l: Double, c: Double, h: Double): Color {
    // Reduce chroma until the color fits inside sRGB, keeping lightness and hue fixed.
    var low = 0.0
    var high = c
    var best = doubleArrayOf(0.0, 0.0, 0.0)
    repeat(24) {
        val mid = (low + high) / 2.0
        val rgb = labToLinearRgb(l, mid * cos(Math.toRadians(h)), mid * sin(Math.toRadians(h)))
        if (rgb.all { it >= -1e-4 && it <= 1.0 + 1e-4 }) {
            best = rgb
            low = mid
        } else {
            high = mid
        }
    }
    return Color(
        red = encode(best[0]).toFloat(),
        green = encode(best[1]).toFloat(),
        blue = encode(best[2]).toFloat(),
        alpha = 1f,
    )
}

private fun labToLinearRgb(l: Double, a: Double, b: Double): DoubleArray {
    val fy = (l + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0
    val x = 0.95047 * invert(fx)
    val y = 1.00000 * invert(fy)
    val z = 1.08883 * invert(fz)
    return doubleArrayOf(
        3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
        -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
        0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
    )
}

private fun invert(t: Double): Double =
    if (t * t * t > 0.008856) t * t * t else (t - 16.0 / 116.0) / 7.787

private fun encode(c: Double): Double {
    val clamped = c.coerceIn(0.0, 1.0)
    return if (clamped <= 0.0031308) 12.92 * clamped
    else 1.055 * clamped.pow(1.0 / 2.4) - 0.055
}
