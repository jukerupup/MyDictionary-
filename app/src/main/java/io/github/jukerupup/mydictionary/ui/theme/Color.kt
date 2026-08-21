package io.github.jukerupup.mydictionary.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val CanvasLight = Color(0xFFF7F5EF)
internal val SurfaceLight = Color(0xFFFFFFFB)
internal val SurfaceMutedLight = Color(0xFFEFECE4)
internal val InkLight = Color(0xFF24221F)
internal val InkSecondaryLight = Color(0xFF625E57)
internal val InkDisabledLight = Color(0xFF8C877E)
internal val OutlineLight = Color(0xFFC9C3B8)
internal val OutlineStrongLight = Color(0xFF817B72)
internal val ActionLight = Color(0xFF3159A7)
internal val OnActionLight = Color.White
internal val ActionContainerLight = Color(0xFFDFE7FA)
internal val OnActionContainerLight = Color(0xFF19366B)
internal val ExampleLight = Color(0xFF3E533E)
internal val ExampleContainerLight = Color(0xFFE8EBDD)
internal val ErrorLight = Color(0xFFB3261E)
internal val ErrorContainerLight = Color(0xFFF9DEDC)
internal val WarningLight = Color(0xFF7A4F00)
internal val WarningContainerLight = Color(0xFFF5E5BE)

internal val CanvasDark = Color(0xFF191816)
internal val SurfaceDark = Color(0xFF24231F)
internal val SurfaceMutedDark = Color(0xFF302E29)
internal val InkDark = Color(0xFFF2EEE5)
internal val InkSecondaryDark = Color(0xFFC9C3B8)
internal val InkDisabledDark = Color(0xFF8E887E)
internal val OutlineDark = Color(0xFF514D45)
internal val OutlineStrongDark = Color(0xFF756F65)
internal val ActionDark = Color(0xFFAEC6FF)
internal val OnActionDark = Color(0xFF123368)
internal val ActionContainerDark = Color(0xFF263E6B)
internal val OnActionContainerDark = Color(0xFFDFE7FA)
internal val ExampleDark = Color(0xFFB8D0B5)
internal val ExampleContainerDark = Color(0xFF283329)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorContainerDark = Color(0xFF5F1512)
internal val WarningDark = Color(0xFFF3C56B)
internal val WarningContainerDark = Color(0xFF493100)

@Immutable
data class DictionaryExtendedColors(
    val example: Color,
    val onExampleContainer: Color,
    val exampleContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val disabledContent: Color,
    val strongOutline: Color,
)

internal val ExtendedColorsLight = DictionaryExtendedColors(
    example = ExampleLight,
    onExampleContainer = ExampleLight,
    exampleContainer = ExampleContainerLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = Color(0xFF3A2500),
    disabledContent = InkDisabledLight,
    strongOutline = OutlineStrongLight,
)

internal val ExtendedColorsDark = DictionaryExtendedColors(
    example = ExampleDark,
    onExampleContainer = ExampleDark,
    exampleContainer = ExampleContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = Color(0xFFFFDEA6),
    disabledContent = InkDisabledDark,
    strongOutline = OutlineStrongDark,
)
