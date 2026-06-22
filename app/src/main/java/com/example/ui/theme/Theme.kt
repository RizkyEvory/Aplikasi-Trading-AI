package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Gold,
    secondary = GoldLight,
    tertiary = PositiveGreen,
    background = DarkBackground,
    surface = CharcoalSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for premium look
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve gold branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
