package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

internal val NanfengPageBackground = Color(0xFFF4F4F2)
internal val NanfengCard = Color(0xFFFFFFFF)
internal val NanfengNavigation = Color(0xFF101513)
internal val NanfengNavigationMuted = Color(0xFF929994)
internal val NanfengInk = Color(0xFF222522)
internal val NanfengGreen = Color(0xFF3D7052)
internal val NanfengOrange = Color(0xFFD9823E)
internal val NanfengSolarTermRed = Color(0xFFC63D3A)
internal val NanfengGold = Color(0xFFB99A58)
internal val NanfengGoldText = Color(0xFF856526)
internal val NanfengGoldLight = Color(0xFFF0D7A4)
internal val NanfengWarmTint = Color(0xFFF8F5EF)
internal val NanfengControlSurface = Color(0xFFF3F3F2)

internal val LocalBaziSkin = staticCompositionLocalOf { BaziSkin.INK_STAR_CHART }
internal val LocalBaziSkinTokens = staticCompositionLocalOf { BaziSkin.INK_STAR_CHART.tokens }
internal val LocalBaziSkinVisualRecipe = staticCompositionLocalOf { BaziSkin.INK_STAR_CHART.visualRecipe }

private fun baziColorScheme(tokens: BaziSkinTokens) = lightColorScheme(
    primary = tokens.primary,
    onPrimary = Color.White,
    primaryContainer = tokens.primary.copy(alpha = 0.16f),
    onPrimaryContainer = tokens.textPrimary,
    secondary = tokens.secondary,
    onSecondary = Color.White,
    secondaryContainer = tokens.secondary.copy(alpha = 0.16f),
    onSecondaryContainer = tokens.textPrimary,
    background = tokens.background,
    onBackground = tokens.textPrimary,
    surface = tokens.surface,
    onSurface = tokens.textPrimary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = tokens.surface,
    surfaceContainer = tokens.surface,
    surfaceContainerHigh = tokens.surfaceRaised,
    surfaceContainerHighest = tokens.surfaceRaised,
    surfaceVariant = tokens.surfaceRaised,
    onSurfaceVariant = tokens.textSecondary,
    // 分隔只承担层级，不参与卡片和按钮的外轮廓；避免主题切换后出现黑色描边。
    outline = tokens.textPrimary.copy(alpha = 0.10f),
    outlineVariant = tokens.textPrimary.copy(alpha = 0.08f),
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
internal fun NanfengBaziTheme(
    skin: BaziSkin = BaziSkin.INK_STAR_CHART,
    content: @Composable () -> Unit,
) {
    val typography = rememberNanfengBaziTypography()
    CompositionLocalProvider(
        LocalBaziSkin provides skin,
        LocalBaziSkinTokens provides skin.tokens,
        LocalBaziSkinVisualRecipe provides skin.visualRecipe,
    ) {
        MaterialTheme(
            colorScheme = baziColorScheme(skin.tokens),
            typography = typography,
            shapes = NanfengBaziShapes,
            content = content,
        )
    }
}

@Composable
internal fun BoxScope.NanfengWhiteDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var anchorBounds by remember { mutableStateOf(IntRect(0, 0, 0, 0)) }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                anchorBounds = IntRect(
                    left = bounds.left.roundToInt(),
                    top = bounds.top.roundToInt(),
                    right = bounds.right.roundToInt(),
                    bottom = bounds.bottom.roundToInt(),
                )
            },
    )
    if (!expanded || anchorBounds.width == 0 || anchorBounds.height == 0) return
    val menuGapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    var popupOriginInWindow by remember { mutableStateOf(IntOffset.Zero) }
    Popup(
        popupPositionProvider = FullScreenPopupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = true,
        ),
    ) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    popupOriginInWindow = IntOffset(
                        bounds.left.roundToInt(),
                        bounds.top.roundToInt(),
                    )
                }
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onTap = { onDismissRequest() })
                },
            content = {
                Surface(
                    modifier = modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 112.dp, max = 280.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        content = content,
                    )
                }
            },
        ) { measurables, constraints ->
            val menu = measurables.single().measure(
                constraints.copy(minWidth = 0, minHeight = 0),
            )
            val position = anchoredMenuPosition(
                anchorBounds = anchorBounds,
                windowSize = IntSize(constraints.maxWidth, constraints.maxHeight),
                layoutDirection = layoutDirection,
                menuSize = IntSize(menu.width, menu.height),
                gapPx = menuGapPx,
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                menu.place(
                    x = position.x - popupOriginInWindow.x,
                    y = position.y - popupOriginInWindow.y,
                )
            }
        }
    }
}

@Composable
internal fun NanfengOverflowMenuItem(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val foreground = if (enabled) accent else accent.copy(alpha = 0.38f)
    DropdownMenuItem(
        text = { Text(label, color = foreground) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
            )
        },
        enabled = enabled,
        modifier = modifier,
    )
}

private object FullScreenPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

private fun anchoredMenuPosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    menuSize: IntSize,
    gapPx: Int,
): IntOffset {
    val preferredX = if (layoutDirection == LayoutDirection.Ltr) {
        anchorBounds.right - menuSize.width
    } else {
        anchorBounds.left
    }
    val maxX = (windowSize.width - menuSize.width - gapPx).coerceAtLeast(gapPx)
    val x = preferredX.coerceIn(gapPx, maxX)

    val below = anchorBounds.bottom + gapPx
    val above = anchorBounds.top - menuSize.height - gapPx
    val maxY = (windowSize.height - menuSize.height - gapPx).coerceAtLeast(gapPx)
    val y = if (below <= maxY) below else above.coerceIn(gapPx, maxY)
    return IntOffset(x, y)
}
