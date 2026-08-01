package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val NanfengPageBackground = Color(0xFFF5F5F3)
internal val NanfengCard = Color(0xFFFFFFFF)
internal val NanfengNavigation = Color(0xFF101513)
internal val NanfengNavigationMuted = Color(0xFF929994)
internal val NanfengGreen = Color(0xFF356B4C)
internal val NanfengOrange = Color(0xFFD97936)
internal val NanfengGold = Color(0xFFB99A58)

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
    onBackground = Color(0xFF1C211E),
    surface = NanfengCard,
    onSurface = Color(0xFF1C211E),
    surfaceVariant = Color(0xFFE8EDE9),
    onSurfaceVariant = Color(0xFF58615B),
    outline = Color(0xFFC8D0CA),
    outlineVariant = Color(0xFFDDE3DE),
    error = Color(0xFFB3261E),
)

private val NanfengBaziShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
internal fun NanfengBaziTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NanfengBaziColorScheme,
        typography = Typography(),
        shapes = NanfengBaziShapes,
        content = content,
    )
}
