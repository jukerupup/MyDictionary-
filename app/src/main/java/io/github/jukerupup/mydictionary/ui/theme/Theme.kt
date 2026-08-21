package io.github.jukerupup.mydictionary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = ActionLight,
    onPrimary = OnActionLight,
    primaryContainer = ActionContainerLight,
    onPrimaryContainer = OnActionContainerLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = OutlineLight,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = ActionDark,
    onPrimary = OnActionDark,
    primaryContainer = ActionContainerDark,
    onPrimaryContainer = OnActionContainerDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = OutlineDark,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorDark,
)

private val DictionaryShapeSet = Shapes(
    small = DictionaryShapes.Small,
    medium = DictionaryShapes.Medium,
    large = DictionaryShapes.Large,
)

private val LocalDictionaryExtendedColors = staticCompositionLocalOf {
    ExtendedColorsLight
}

object DictionaryTheme {
    val colors: DictionaryExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDictionaryExtendedColors.current
}

@Composable
fun MyDictionaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) ExtendedColorsDark else ExtendedColorsLight
    CompositionLocalProvider(LocalDictionaryExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DictionaryTypography,
            shapes = DictionaryShapeSet,
            content = content,
        )
    }
}
