package dev.nullcode.mediamagnet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nullcode.mediamagnet.data.AccentMode
import dev.nullcode.mediamagnet.data.ThemeMode

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

const val DefaultAccentHue = 322f
val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val AppMotionScheme: MotionScheme = MotionScheme.expressive()
private val AppTypography: Typography = Typography()

@Composable
fun MediaMagnetTheme(
    themeMode: String = ThemeMode.System,
    amoled: Boolean = false,
    accentMode: String = AccentMode.System,
    accentHue: Float = DefaultAccentHue,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = rememberColorScheme(darkTheme, amoled, accentMode, accentHue)

    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = AppMotionScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}

@Composable
private fun rememberColorScheme(
    darkTheme: Boolean,
    amoled: Boolean,
    accentMode: String,
    accentHue: Float,
): ColorScheme {
    val context = LocalContext.current
    val useSystemAccent = accentMode == AccentMode.System

    return remember(darkTheme, amoled, useSystemAccent, accentHue, context) {
        val dynamic = useSystemAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        when {
            dynamic && darkTheme -> dynamicDarkColorScheme(context).withAmoled(amoled)
            dynamic -> dynamicLightColorScheme(context)
            else -> {
                val palettes = AppPalettes(accentHue, monochrome = false)
                if (darkTheme) palettes.darkScheme(amoled) else palettes.lightScheme()
            }
        }
    }
}

private fun ColorScheme.withAmoled(amoled: Boolean): ColorScheme {
    if (!amoled) return this
    return copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
    )
}

@Composable
fun MediaMagnetPreviewTheme(content: @Composable () -> Unit) {
    val palettes = remember { AppPalettes(DefaultAccentHue, monochrome = false) }
    MaterialTheme(
        colorScheme = palettes.lightScheme(),
        motionScheme = AppMotionScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
