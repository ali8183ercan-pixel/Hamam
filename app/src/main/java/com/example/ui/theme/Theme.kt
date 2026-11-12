package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RoachColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = MidnightSubway,
  primaryContainer = Color(0xFF004E5C),
  onPrimaryContainer = NeonCyan,
  secondary = GraffitiOrange,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF5A2700),
  onSecondaryContainer = Color(0xFFFFD1AD),
  tertiary = CoinGold,
  onTertiary = MidnightSubway,
  background = MidnightSubway,
  onBackground = SoftWhite,
  surface = DeepTunnel,
  onSurface = SoftWhite,
  surfaceVariant = ElevatedCard,
  onSurfaceVariant = MutedLavender,
  error = DangerRed,
  onError = Color.White,
)

private val RoachShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(26.dp),
)

@Composable
fun RoachRushTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = RoachColorScheme,
    typography = RoachTypography,
    shapes = RoachShapes,
    content = content,
  )
}
