package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF5EEAD4),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MedTealPrimary,
    onPrimary = Color.White,
    primaryContainer = MedTealLight,
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF0D9488),
    background = MedBackgroundLight,
    surface = MedSurfaceLight,
    onBackground = MedSlateDark,
    onSurface = MedSlateDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = MedSlateMedium,
    outline = Color(0xFFCBD5E1),
    error = MedErrorRed,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val target = if (darkTheme) DarkColorScheme else LightColorScheme
  val animSpec = tween<Color>(durationMillis = 200)

  val primary by animateColorAsState(target.primary, animSpec, label = "primary")
  val onPrimary by animateColorAsState(target.onPrimary, animSpec, label = "onPrimary")
  val primaryContainer by animateColorAsState(target.primaryContainer, animSpec, label = "primaryContainer")
  val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animSpec, label = "onPrimaryContainer")
  val secondary by animateColorAsState(target.secondary, animSpec, label = "secondary")
  val onSecondary by animateColorAsState(target.onSecondary, animSpec, label = "onSecondary")
  val background by animateColorAsState(target.background, animSpec, label = "background")
  val onBackground by animateColorAsState(target.onBackground, animSpec, label = "onBackground")
  val surface by animateColorAsState(target.surface, animSpec, label = "surface")
  val onSurface by animateColorAsState(target.onSurface, animSpec, label = "onSurface")
  val surfaceVariant by animateColorAsState(target.surfaceVariant, animSpec, label = "surfaceVariant")
  val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
  val outline by animateColorAsState(target.outline, animSpec, label = "outline")

  val animatedScheme =
    target.copy(
      primary = primary,
      onPrimary = onPrimary,
      primaryContainer = primaryContainer,
      onPrimaryContainer = onPrimaryContainer,
      secondary = secondary,
      onSecondary = onSecondary,
      background = background,
      onBackground = onBackground,
      surface = surface,
      onSurface = onSurface,
      surfaceVariant = surfaceVariant,
      onSurfaceVariant = onSurfaceVariant,
      outline = outline,
    )

  MaterialTheme(colorScheme = animatedScheme, typography = Typography, content = content)
}


