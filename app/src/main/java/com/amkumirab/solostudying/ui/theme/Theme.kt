package com.amkumirab.solostudying.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonBlueAccent,
    secondary = NeonBlueSecondary,
    tertiary = RpgGold,
    background = BlackFantasyBackground,
    surface = DarkFantasySurface,
    onBackground = TextWhite,
    onSurface = TextWhite
  )

@Composable
fun SoloStudyingTheme(content: @Composable () -> Unit) =
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content,
  )
