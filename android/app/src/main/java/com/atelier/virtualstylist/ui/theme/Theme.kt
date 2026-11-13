package com.atelier.virtualstylist.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modern Gen Z Mobile Palette
val GalleryPaper = Color(0xFFF6F4EC)       // Warm Chalk Canvas
val GalleryCard = Color(0xFFFFFFFF)        // Crisp White Card
val GalleryStone = Color(0xFFECE8DC)       // Soft Putty Surface
val GalleryBorder = Color(0xFF1A1A1A)      // Clean Ink Outline
val GalleryHairline = Color(0xFFE0DDD2)    // Subtle Divider
val GalleryInk = Color(0xFF111111)         // Deep Obsidian
val GalleryMuted = Color(0xFF636058)       // Balanced Slate Gray
val GalleryBrass = Color(0xFF2B4BFF)       // Electric Cobalt Accent
val NeonLime = Color(0xFFD4FF38)           // Acid Lime Signature
val HotCoral = Color(0xFFFF3D6E)           // Hyper Coral Accent
val GalleryErrorBg = Color(0xFFFFE7EE)
val GalleryErrorInk = Color(0xFF9F1239)

val CardRadius = 20.dp
val CardShape = RoundedCornerShape(20.dp)
val PillShape = RoundedCornerShape(999.dp)
val InnerShape = RoundedCornerShape(14.dp)

private val AtelierColorScheme = lightColorScheme(
    primary = GalleryInk,
    onPrimary = NeonLime,
    secondary = GalleryBrass,
    onSecondary = Color.White,
    tertiary = HotCoral,
    onTertiary = Color.White,
    background = GalleryPaper,
    onBackground = GalleryInk,
    surface = GalleryCard,
    onSurface = GalleryInk,
    surfaceVariant = GalleryStone,
    onSurfaceVariant = GalleryMuted,
    outline = GalleryHairline,
    error = GalleryErrorInk
)

private val AtelierTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
        color = GalleryInk
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        color = GalleryInk
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
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
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = GalleryInk
    )
)

@Composable
fun AtelierSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = GalleryCard,
    borderColor: Color = GalleryHairline,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = CardShape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(BorderStroke(borderWidth, borderColor), shape),
        content = content
    )
}

@Composable
fun AtelierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtelierColorScheme,
        typography = AtelierTypography,
        content = content
    )
}
