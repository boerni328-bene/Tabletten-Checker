package com.tablettenchecker.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ruhige Palette in Petrol; Bernstein für „bald nachkaufen“, Rot für „aufgebraucht“.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00696B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F2),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA1),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFF4FBFA),
    onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FBFA),
    onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F4),
    surfaceContainer = Color(0xFFE9EFEE),
    surfaceContainerHigh = Color(0xFFE3E9E8),
    surfaceContainerHighest = Color(0xFFDDE4E3),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC9C8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D6),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F51),
    onPrimaryContainer = Color(0xFF9CF1F2),
    secondary = Color(0xFFB0CCCB),
    onSecondary = Color(0xFF1B3435),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFF5BF48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA1),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF2F3636),
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3F4948),
)

/** Farben (Hintergrund, Schrift) der Einnahme-Marken: morgens Himmelblau, mittags Grün, abends Violett. */
@Composable
fun doseColors(slot: DoseFilter): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when (slot) {
        DoseFilter.NOON ->
            if (dark) Color(0xFF2E4B25) to Color(0xFFD3F2CB) else Color(0xFFD8F3D0) to Color(0xFF0F3A0A)
        DoseFilter.EVENING ->
            if (dark) Color(0xFF3D3570) to Color(0xFFE3DEFF) else Color(0xFFE5DEFF) to Color(0xFF22155E)
        else ->
            if (dark) Color(0xFF1E4A63) to Color(0xFFCDEBFF) else Color(0xFFD3EEFF) to Color(0xFF003549)
    }
}

@Composable
fun TablettenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
