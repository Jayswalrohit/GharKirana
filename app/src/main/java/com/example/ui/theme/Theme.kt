package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = KiranaGreenAccent,
    onPrimary = Color.Black,
    primaryContainer = KiranaGreenDark,
    onPrimaryContainer = KiranaGreenLight,
    secondary = KiranaAmber,
    onSecondary = Color.Black,
    background = Color(0xFF121820),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = KiranaGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = KiranaGreenLight,
    onPrimaryContainer = KiranaGreenDark,
    secondary = KiranaAmber,
    onSecondary = Color.White,
    secondaryContainer = KiranaAmberLight,
    onSecondaryContainer = KiranaAmberDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = OutlineLight,
    error = ErrorRed,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
