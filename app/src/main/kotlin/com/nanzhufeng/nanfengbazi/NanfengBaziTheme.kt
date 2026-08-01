package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val NanfengPageBackground = Color(0xFFF4F4F2)
internal val NanfengCard = Color(0xFFFFFFFF)
internal val NanfengNavigation = Color(0xFF101513)
internal val NanfengNavigationMuted = Color(0xFF929994)
internal val NanfengInk = Color(0xFF222522)
internal val NanfengGreen = Color(0xFF3D7052)
internal val NanfengOrange = Color(0xFFD9823E)
internal val NanfengGold = Color(0xFFB99A58)
internal val NanfengGoldLight = Color(0xFFF0D7A4)
internal val NanfengWarmTint = Color(0xFFF8F5EF)
internal val NanfengControlSurface = Color(0xFFF3F3F2)

private val NanfengBaziColorScheme = lightColorScheme(
    primary = NanfengGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBDF),
    onPrimaryContainer = Color(0xFF183A28),
    secondary = NanfengOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4D2),
    onSecondaryContainer = Color(0xFF542A11),
    background = NanfengPageBackground,
    onBackground = NanfengInk,
    surface = NanfengCard,
    onSurface = NanfengInk,
    surfaceVariant = Color(0xFFEDEDEB),
    onSurfaceVariant = Color(0xFF656A66),
    outline = Color(0xFFD3D5D2),
    outlineVariant = Color(0xFFE7E8E6),
    error = Color(0xFFB3261E),
)

private val NanfengBaziShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val NanfengBaziTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(
        fontSize = 22.sp,
        lineHeight = 29.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontSize = 20.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Typography().titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
internal fun NanfengBaziTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NanfengBaziColorScheme,
        typography = NanfengBaziTypography,
        shapes = NanfengBaziShapes,
        content = content,
    )
}
