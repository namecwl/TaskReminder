package com.example.taskreminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B63F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EBFF),
    onPrimaryContainer = Color(0xFF17245C),
    secondary = Color(0xFF159D8E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F7F2),
    onSecondaryContainer = Color(0xFF07473F),
    tertiary = Color(0xFFF08C4A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6D8),
    onTertiaryContainer = Color(0xFF5D2410),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE7E7),
    onErrorContainer = Color(0xFF6D1A1D),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF171A21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171A21),
    surfaceVariant = Color(0xFFEEF1F7),
    onSurfaceVariant = Color(0xFF656B78),
    outline = Color(0xFFC7CBD4),
    outlineVariant = Color(0xFFE2E5EB),
    inverseSurface = Color(0xFF2E3138),
    inverseOnSurface = Color(0xFFF4F5F7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9B8FF),
    onPrimary = Color(0xFF17245C),
    primaryContainer = Color(0xFF34498F),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFF67D8C8),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF075E54),
    onSecondaryContainer = Color(0xFFD1F7F1),
    tertiary = Color(0xFFFFB27A),
    onTertiary = Color(0xFF55200A),
    tertiaryContainer = Color(0xFF753514),
    onTertiaryContainer = Color(0xFFFFDCC7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D22),
    onErrorContainer = Color(0xFFFFDAD8),
    background = Color(0xFF0F1116),
    onBackground = Color(0xFFE6E8ED),
    surface = Color(0xFF191C24),
    onSurface = Color(0xFFE6E8ED),
    surfaceVariant = Color(0xFF252A34),
    onSurfaceVariant = Color(0xFFB8BECA),
    outline = Color(0xFF737986),
    outlineVariant = Color(0xFF353A45),
    inverseSurface = Color(0xFFE2E4EA),
    inverseOnSurface = Color(0xFF2B2E35)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * 应用主题入口。
 *
 * 使用固定的蓝色系品牌色，而不是动态取色，以保证不同手机上外观一致；
 * 同时提供完整的浅色和深色配色。
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}




