package com.atelier.virtualstylist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val GalleryPaper = Color(0xFFFAF9F6)
val GalleryCard = Color(0xFFFFFFFF)
val GalleryStone = Color(0xFFF4F3EE)
val GalleryBorder = Color(0xFFE5E3DC)
val GalleryInk = Color(0xFF141413)
val GalleryMuted = Color(0xFF68665F)
val GalleryBrass = Color(0xFF8A6A42)
val GalleryErrorBg = Color(0xFFFEF2F2)
val GalleryErrorInk = Color(0xFF7F1D1D)

private val AtelierColorScheme = lightColorScheme(
    primary = GalleryInk,
    onPrimary = GalleryPaper,
    secondary = GalleryBrass,
    onSecondary = GalleryPaper,
    background = GalleryPaper,
    onBackground = GalleryInk,
    surface = GalleryCard,
    onSurface = GalleryInk,
    surfaceVariant = GalleryStone,
    onSurfaceVariant = GalleryMuted,
    outline = GalleryBorder,
    error = GalleryErrorInk
)

private val AtelierTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        color = GalleryInk
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        color = GalleryInk
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        color = GalleryInk
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = GalleryInk
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = GalleryInk
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
        color = GalleryMuted
    )
)

@Composable
fun AtelierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtelierColorScheme,
        typography = AtelierTypography,
        content = content
    )
}
