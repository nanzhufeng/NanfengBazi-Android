package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
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
internal val NanfengSolarTermRed = Color(0xFFC63D3A)
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

private const val AppFontAssetPath = "fonts/noto_sans_sc_variable.ttf"

@Composable
private fun rememberNanfengBaziTypography(): Typography {
    val context = LocalContext.current
    val fontFamily = remember(context) {
        FontFamily(
            appFont(context.assets, FontWeight.Normal),
            appFont(context.assets, FontWeight.Medium),
            appFont(context.assets, FontWeight.SemiBold),
            appFont(context.assets, FontWeight.Bold),
        )
    }
    return remember(fontFamily) {
        Typography(
            headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
            headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
            titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
            titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
            titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
            bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
            labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp),
            labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
        )
    }
}

private fun appFont(
    assetManager: android.content.res.AssetManager,
    weight: FontWeight,
) = Font(
    path = AppFontAssetPath,
    assetManager = assetManager,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@Composable
internal fun NanfengBaziTheme(content: @Composable () -> Unit) {
    val typography = rememberNanfengBaziTypography()
    MaterialTheme(
        colorScheme = NanfengBaziColorScheme,
        typography = typography,
        shapes = NanfengBaziShapes,
        content = content,
    )
}
