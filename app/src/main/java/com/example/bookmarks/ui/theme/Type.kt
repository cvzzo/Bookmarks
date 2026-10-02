package com.example.bookmarks.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bookmarks.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val Default = Typography()

// Monospace everywhere; body text slightly smaller because mono glyphs are wider.
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = JetBrainsMono),
    displayMedium = Default.displayMedium.copy(fontFamily = JetBrainsMono),
    displaySmall = Default.displaySmall.copy(fontFamily = JetBrainsMono),
    headlineLarge = Default.headlineLarge.copy(fontFamily = JetBrainsMono),
    headlineMedium = Default.headlineMedium.copy(fontFamily = JetBrainsMono),
    headlineSmall = Default.headlineSmall.copy(fontFamily = JetBrainsMono),
    titleLarge = Default.titleLarge.copy(fontFamily = JetBrainsMono, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = Default.titleMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium),
    titleSmall = Default.titleSmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    bodyLarge = Default.bodyLarge.copy(fontFamily = JetBrainsMono, fontSize = 15.sp, letterSpacing = 0.sp),
    bodyMedium = Default.bodyMedium.copy(fontFamily = JetBrainsMono, fontSize = 13.sp, letterSpacing = 0.sp),
    bodySmall = Default.bodySmall.copy(fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 0.sp),
    labelLarge = Default.labelLarge.copy(fontFamily = JetBrainsMono),
    labelMedium = Default.labelMedium.copy(fontFamily = JetBrainsMono),
    labelSmall = Default.labelSmall.copy(fontFamily = JetBrainsMono),
)
