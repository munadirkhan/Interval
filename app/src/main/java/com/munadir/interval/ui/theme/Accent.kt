package com.munadir.interval.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Accent presets. One accent family per theme, four to choose from.
 *
 * Each carries its own on-colors and container pair rather than deriving them, so a preset can
 * be tuned by eye instead of by formula.
 */
enum class Accent(
    val label: String,
    val swatch: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color
) {
    AMBER(
        label = "Amber",
        swatch = Color(0xFFFFA726),
        darkPrimary = Color(0xFFFFB95C),
        darkOnPrimary = Color(0xFF3A2400),
        darkContainer = Color(0xFF4A3213),
        darkOnContainer = Color(0xFFFFE0B2),
        lightPrimary = Color(0xFFB26A00),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFFE6C4),
        lightOnContainer = Color(0xFF4A2D00)
    ),
    INK_BLUE(
        label = "Ink Blue",
        swatch = Color(0xFF4C7DF0),
        darkPrimary = Color(0xFF9CBBFF),
        darkOnPrimary = Color(0xFF002E69),
        darkContainer = Color(0xFF1F3C77),
        darkOnContainer = Color(0xFFD6E2FF),
        lightPrimary = Color(0xFF2C5BD4),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDCE4FF),
        lightOnContainer = Color(0xFF00184A)
    ),
    SAGE(
        label = "Sage",
        swatch = Color(0xFF6FA287),
        darkPrimary = Color(0xFF9DD3B4),
        darkOnPrimary = Color(0xFF003824),
        darkContainer = Color(0xFF1F4D38),
        darkOnContainer = Color(0xFFBBF0D0),
        lightPrimary = Color(0xFF356B4F),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFD1EEDD),
        lightOnContainer = Color(0xFF00210F)
    ),
    MAGENTA(
        label = "Magenta",
        swatch = Color(0xFFD64C9B),
        darkPrimary = Color(0xFFFFAFD6),
        darkOnPrimary = Color(0xFF5A0037),
        darkContainer = Color(0xFF7C2456),
        darkOnContainer = Color(0xFFFFD8E8),
        lightPrimary = Color(0xFFA5356F),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFFD8E8),
        lightOnContainer = Color(0xFF3D0025)
    );

    companion object {
        fun from(name: String?): Accent =
            entries.firstOrNull { it.name == name } ?: AMBER
    }
}
