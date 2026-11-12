package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BalooDisplayFontFamily = FontFamily(
  Font(R.font.baloo_2, FontWeight.Normal),
  Font(R.font.baloo_2, FontWeight.Bold),
  Font(R.font.baloo_2, FontWeight.ExtraBold),
)

val RubikBodyFontFamily = FontFamily(
  Font(R.font.rubik, FontWeight.Normal),
  Font(R.font.rubik, FontWeight.Medium),
  Font(R.font.rubik, FontWeight.Bold),
)

val RoachTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = BalooDisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    letterSpacing = 0.5.sp,
  ),
  displayMedium = TextStyle(
    fontFamily = BalooDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 36.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = BalooDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = BalooDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = RubikBodyFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = RubikBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = RubikBodyFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = RubikBodyFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = RubikBodyFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
)
