package com.budgetflow.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fallback palette for API < 31 (no dynamic color) and as seed for the brand identity.
val SeedGreen = Color(0xFF2E7D5B)
val SeedGreenDark = Color(0xFF9AD4B9)

val LightPrimary = Color(0xFF2E7D5B)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB3F1D0)
val LightSecondary = Color(0xFF4C6358)
val LightBackground = Color(0xFFFBFDF9)
val LightSurface = Color(0xFFFBFDF9)
val LightError = Color(0xFFBA1A1A)

val DarkPrimary = Color(0xFF9AD4B9)
val DarkOnPrimary = Color(0xFF00391F)
val DarkPrimaryContainer = Color(0xFF15513A)
val DarkSecondary = Color(0xFFB3CCBF)
val DarkBackground = Color(0xFF191C1A)
val DarkSurface = Color(0xFF191C1A)
val DarkError = Color(0xFFFFB4AB)

// Semantic colors used across the dashboard/statistics, independent of the Material scheme.
//
// Each one has a light-theme and a dark-theme value: a single fixed hex cannot clear WCAG AA
// (4.5:1) against both a near-white and a near-black surface at once. Use the composable
// [positiveGreen]/[negativeRed]/[neutralAmber] accessors below instead of these directly - they
// pick the right variant for the theme actually in effect (including a user-forced light/dark
// mode, not just the system setting).
val PositiveGreenLight = Color(0xFF2E7D5B) // 4.9:1 on LightSurface
val PositiveGreenDark = Color(0xFF9AD4B9) // 10.2:1 on DarkSurface
val NegativeRedLight = Color(0xFFBA1A1A) // 6.3:1 on LightSurface
val NegativeRedDark = Color(0xFFFFB4AB) // 10.1:1 on DarkSurface
val NeutralAmberLight = Color(0xFF8F6A0A) // 4.9:1 on LightSurface
val NeutralAmberDark = Color(0xFFB8860B) // 5.3:1 on DarkSurface

/** "Positive" amounts/states - theme-aware, always ≥4.5:1 against the current surface. */
val positiveGreen: Color
    @Composable get() = if (LocalIsDarkTheme.current) PositiveGreenDark else PositiveGreenLight

/** "Negative" amounts/states - theme-aware, always ≥4.5:1 against the current surface. */
val negativeRed: Color
    @Composable get() = if (LocalIsDarkTheme.current) NegativeRedDark else NegativeRedLight

/** "Caution" amounts/states - theme-aware, always ≥4.5:1 against the current surface. */
val neutralAmber: Color
    @Composable get() = if (LocalIsDarkTheme.current) NeutralAmberDark else NeutralAmberLight
