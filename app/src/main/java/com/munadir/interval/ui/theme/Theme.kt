package com.munadir.interval.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paper black. Not grey-blue, not Material default -- an ink ground that lets one warm accent
// carry the whole interface.
private val PaperBlack = Color(0xFF0B0A0C)
private val PaperRaised = Color(0xFF161519)
private val PaperCard = Color(0xFF1E1D22)
private val PaperText = Color(0xFFF2F0EC)
private val PaperTextDim = Color(0xFF9B9792)
private val PaperLine = Color(0xFF2F2D33)

private val DayWhite = Color(0xFFFAF8F5)
private val DayRaised = Color(0xFFF1EEE8)
private val DayCard = Color(0xFFFFFFFF)
private val DayText = Color(0xFF16141A)
private val DayTextDim = Color(0xFF6B6761)
private val DayLine = Color(0xFFDFDAD2)

private val Danger = Color(0xFFE5484D)

@Composable
fun IntervalTheme(
    darkTheme: Boolean = true,
    accent: Accent = Accent.AMBER,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            secondary = accent.darkPrimary,
            onSecondary = accent.darkOnPrimary,
            background = PaperBlack,
            onBackground = PaperText,
            surface = PaperRaised,
            onSurface = PaperText,
            surfaceVariant = PaperCard,
            onSurfaceVariant = PaperTextDim,
            surfaceContainerHighest = PaperCard,
            outline = PaperLine,
            outlineVariant = PaperLine,
            error = Danger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            secondary = accent.lightPrimary,
            onSecondary = accent.lightOnPrimary,
            background = DayWhite,
            onBackground = DayText,
            surface = DayRaised,
            onSurface = DayText,
            surfaceVariant = DayCard,
            onSurfaceVariant = DayTextDim,
            surfaceContainerHighest = DayCard,
            outline = DayLine,
            outlineVariant = DayLine,
            error = Danger,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
