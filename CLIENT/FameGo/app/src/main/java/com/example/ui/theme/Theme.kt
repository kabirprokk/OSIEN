package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FameGoDarkColorScheme = darkColorScheme(
  primary = FameGoGold,
  onPrimary = FameGoBg,
  primaryContainer = FameGoGoldContainer,
  onPrimaryContainer = FameGoGold,
  secondary = FameGoAccentCyan,
  onSecondary = FameGoBg,
  secondaryContainer = FameGoCardElevated,
  onSecondaryContainer = FameGoTextPrimary,
  tertiary = FameGoSuccessGreen,
  onTertiary = FameGoBg,
  background = FameGoBg,
  onBackground = FameGoTextPrimary,
  surface = FameGoSurface,
  onSurface = FameGoTextPrimary,
  surfaceVariant = FameGoCard,
  onSurfaceVariant = FameGoTextSecondary,
  outline = FameGoBorder,
  error = FameGoLiveRed,
  onError = FameGoWhite,
)

@Composable
fun FameGoTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = FameGoDarkColorScheme,
    typography = Typography,
    content = content,
  )
}

// Backward-compatible wrapper for template tests
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  FameGoTheme(content = content)
}

