package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FameBookDarkColorScheme = darkColorScheme(
  primary = AmberGold,
  onPrimary = ObsidianBlack,
  primaryContainer = AmberGoldContainer,
  onPrimaryContainer = AmberGold,
  secondary = LensCyan,
  onSecondary = ObsidianBlack,
  secondaryContainer = LensCyanContainer,
  onSecondaryContainer = LensCyan,
  tertiary = RecRed,
  onTertiary = PureWhite,
  background = ObsidianBlack,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkCard,
  onSurfaceVariant = TextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorderSubtle,
  error = RecRed,
  onError = PureWhite
)

// FameBook is strictly designed as a dark-first cinematic platform
@Composable
fun FameBookTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = FameBookDarkColorScheme,
    typography = Typography,
    content = content
  )
}
