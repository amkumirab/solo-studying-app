package com.amkumirab.solostudying.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonBlueAccent,
    onPrimary = OnAccent,
    primaryContainer = InfoContainer,
    onPrimaryContainer = TextWhite,
    secondary = NeonBlueSecondary,
    onSecondary = OnAccent,
    secondaryContainer = SurfaceInteractive,
    onSecondaryContainer = TextWhite,
    tertiary = RpgGold,
    onTertiary = OnAccent,
    tertiaryContainer = WarningContainer,
    onTertiaryContainer = TextWhite,
    error = RpgRuby,
    onError = OnAccent,
    errorContainer = DangerContainer,
    onErrorContainer = TextWhite,
    background = BlackFantasyBackground,
    onBackground = TextWhite,
    surface = DarkFantasySurface,
    onSurface = TextWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = DarkCardBorder,
    outlineVariant = StrongCardBorder,
    scrim = Color.Black,
  )

@Composable
fun SoloStudyingTheme(content: @Composable () -> Unit) =
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content,
  )
