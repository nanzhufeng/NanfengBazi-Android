package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private val BaziSkinPickerShape = RoundedCornerShape(20.dp)
private val BaziSkinPickerCardHeight = 84.dp
private val BaziSkinPickerTrackHeight = 252.dp
private val BaziHomeSkinHeaderShape = RoundedCornerShape(24.dp)
private val BaziHomeHeaderEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private const val BaziHomeHeaderArtworkOverscan = 1.06f

@Composable
internal fun BaziSkinArtwork(
    recipe: BaziSkinVisualRecipe,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(recipe.headerArtworkRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = recipe.artworkAlignment,
        modifier = modifier,
    )
}

@Composable
internal fun BaziHomeSkinHeader(
    skin: BaziSkin = LocalBaziSkin.current,
    height: Dp = 148.dp,
    modifier: Modifier = Modifier,
) {
    val recipe = skin.visualRecipe
    val tokens = skin.tokens
    var currentSolarTime by remember { mutableStateOf(baziHomeSolarDateTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentSolarTime = baziHomeSolarDateTime()
            delay(1_000L)
        }
    }
    val scope = rememberCoroutineScope()
    val flowProgress = remember(skin.id) { Animatable(0f) }
    val pulseProgress = remember(skin.id) { Animatable(0f) }
    val density = LocalDensity.current
    val artworkTravelPx = with(density) { 14.dp.toPx() }
    val textTravelPx = with(density) { 6.dp.toPx() }
    val activateFlow: () -> Unit = {
        scope.launch {
            pulseProgress.stop()
            pulseProgress.snapTo(0f)
            pulseProgress.animateTo(1f, tween(durationMillis = 150, easing = BaziHomeHeaderEaseOut))
            pulseProgress.animateTo(0f, tween(durationMillis = 260, easing = BaziHomeHeaderEaseOut))
        }
        Unit
    }
    Surface(
        onClick = activateFlow,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = "${skin.displayName}互动头图，点击或左右滑动可查看五行流转效果"
            },
        shape = BaziHomeSkinHeaderShape,
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier.pointerInput(skin.id) {
                detectHorizontalDragGestures(
                    onDragStart = { activateFlow() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            flowProgress.snapTo(
                                (flowProgress.value + dragAmount / 280f).coerceIn(-1f, 1f),
                            )
                        }
                    },
                    onDragEnd = {
                        if (flowProgress.value.absoluteValue >= 0.08f) activateFlow()
                        scope.launch {
                            flowProgress.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            flowProgress.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                )
            },
        ) {
            BaziSkinArtwork(
                recipe,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = flowProgress.value * artworkTravelPx
                        // 画面先统一外扩，再跟手移动，六套皮肤在左右流转时都不会露出裁切边缘。
                        scaleX = BaziHomeHeaderArtworkOverscan +
                            (flowProgress.value.absoluteValue + pulseProgress.value) * 0.014f
                        scaleY = 1.035f + pulseProgress.value * 0.01f
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                recipe.headerScrimColor.copy(alpha = recipe.headerScrimAlpha),
                                recipe.headerScrimColor.copy(alpha = recipe.headerScrimAlpha * 0.58f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            BaziHomeFlowOverlay(
                primary = tokens.primary,
                secondary = tokens.secondary,
                highlight = recipe.headerContentColor,
                progress = pulseProgress.value,
                horizontalFlow = flowProgress.value,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 17.dp)
                    .graphicsLayer {
                        translationX = -flowProgress.value * textTravelPx
                        translationY = -pulseProgress.value * with(density) { 4.dp.toPx() }
                        scaleX = 1f + pulseProgress.value * 0.035f
                        scaleY = 1f + pulseProgress.value * 0.035f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "南枫八字",
                    color = recipe.headerContentColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    skin.subhead,
                    color = recipe.headerContentColor.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                currentSolarTime,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 17.dp, vertical = 14.dp),
                color = recipe.headerContentColor.copy(alpha = 0.76f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "八字",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface.copy(alpha = 0.22f))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
                    .graphicsLayer {
                        scaleX = 1f + pulseProgress.value * 0.065f
                        scaleY = 1f + pulseProgress.value * 0.065f
                        rotationZ = flowProgress.value * 5f + pulseProgress.value * 2f
                        transformOrigin = TransformOrigin(1f, 1f)
                    },
                color = recipe.headerContentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun baziHomeSolarDateTime(): String = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 · HH:mm:ss"))

@Composable
private fun BaziHomeFlowOverlay(
    primary: Color,
    secondary: Color,
    highlight: Color,
    progress: Float,
    horizontalFlow: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val energy = maxOf(progress, horizontalFlow.absoluteValue)
        if (energy < 0.015f) return@Canvas

        val center = Offset(
            x = size.width * (0.73f + horizontalFlow * 0.025f),
            y = size.height * 0.5f,
        )
        val orbitWidth = size.minDimension * 0.32f
        val orbitHeight = orbitWidth * 0.66f
        val orbitTopLeft = Offset(center.x - orbitWidth / 2f, center.y - orbitHeight / 2f)
        val orbitSize = Size(orbitWidth, orbitHeight)
        val sweepCenter = size.width * (0.18f + progress * 0.50f + horizontalFlow * 0.18f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlight.copy(alpha = 0.12f * energy),
                    Color.Transparent,
                ),
                start = Offset(sweepCenter - size.width * 0.18f, 0f),
                end = Offset(sweepCenter + size.width * 0.18f, size.height),
            ),
            size = size,
        )
        val ribbonBaseY = size.height * 0.70f
        repeat(3) { index ->
            val ribbonOffset = index * size.height * 0.075f
            val ribbon = Path().apply {
                moveTo(size.width * 0.06f, ribbonBaseY + ribbonOffset)
                cubicTo(
                    size.width * 0.28f,
                    ribbonBaseY - size.height * (0.24f + index * 0.035f) + ribbonOffset,
                    size.width * 0.47f,
                    ribbonBaseY + size.height * (0.19f + index * 0.04f) + ribbonOffset,
                    center.x,
                    center.y + (index - 0.5f) * size.height * 0.16f,
                )
            }
            drawPath(
                path = ribbon,
                color = if (index == 0) primary else secondary,
                style = Stroke(width = 1.3f + energy * (2.6f - index * 0.25f)),
                alpha = 0.18f + energy * (0.32f - index * 0.035f),
            )
        }
        val arcAlpha = 0.14f + energy * 0.30f

        repeat(3) { index ->
            val phase = index * 72f + horizontalFlow * 28f + progress * 36f
            drawArc(
                color = primary.copy(alpha = arcAlpha * (1f - index * 0.14f)),
                startAngle = -76f + phase,
                sweepAngle = 158f,
                useCenter = false,
                topLeft = orbitTopLeft,
                size = orbitSize,
                style = Stroke(width = 1.1f + energy * 1.3f),
            )
        }
        repeat(5) { index ->
            val particleAngle = Math.toRadians(
                (-42f + index * 74f + horizontalFlow * 82f + progress * 128f).toDouble(),
            )
            val particleCenter = Offset(
                x = center.x + cos(particleAngle).toFloat() * orbitWidth * (0.24f + index * 0.045f),
                y = center.y + sin(particleAngle).toFloat() * orbitHeight * (0.24f + index * 0.045f),
            )
            drawCircle(
                color = if (index % 2 == 1) secondary else primary,
                radius = 11f + energy * 19f,
                center = particleCenter,
                alpha = 0.11f + energy * 0.15f,
            )
            drawCircle(
                color = highlight,
                radius = 2.6f + energy * 2.7f,
                center = particleCenter,
                alpha = 0.52f + energy * 0.24f,
            )
        }
        val particleAngle = Math.toRadians((-42f + horizontalFlow * 82f + progress * 128f).toDouble())
        val particleCenter = Offset(
            x = center.x + cos(particleAngle).toFloat() * orbitWidth * 0.43f,
            y = center.y + sin(particleAngle).toFloat() * orbitHeight * 0.43f,
        )
        drawCircle(
            color = secondary.copy(alpha = 0.16f + energy * 0.22f),
            radius = 18f + energy * 24f,
            center = particleCenter,
        )
        drawCircle(
            color = highlight.copy(alpha = 0.64f + energy * 0.26f),
            radius = 3.8f + energy * 2.8f,
            center = particleCenter,
        )
        val sealCenter = Offset(size.width * 0.89f, size.height * 0.78f)
        drawCircle(
            color = primary,
            radius = 25f + energy * 14f,
            center = sealCenter,
            alpha = 0.04f + energy * 0.12f,
        )
        drawArc(
            color = secondary,
            startAngle = -94f + progress * 86f + horizontalFlow * 22f,
            sweepAngle = 164f,
            useCenter = false,
            topLeft = Offset(sealCenter.x - 30f, sealCenter.y - 30f),
            size = Size(60f, 60f),
            style = Stroke(width = 1.4f + energy * 1.8f),
            alpha = 0.20f + energy * 0.36f,
        )
    }
}

@Composable
internal fun BaziSkinSettingRow(
    selectedSkin: BaziSkin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = selectedSkin.tokens
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .semantics {
                contentDescription = "当前皮肤：${selectedSkin.displayName}，点击选择皮肤"
            },
        shape = RoundedCornerShape(18.dp),
        color = preview.surface,
        shadowElevation = 2.dp,
    ) {
        Box {
            BaziSkinArtwork(selectedSkin.visualRecipe, Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(preview.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        "当前皮肤",
                        style = MaterialTheme.typography.labelSmall,
                        color = preview.textSecondary,
                    )
                    Text(
                        selectedSkin.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = preview.textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "›",
                    color = preview.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BaziSkinPickerDialog(
    selectedSkin: BaziSkin,
    onSkinPreview: (BaziSkin) -> Unit,
    onSkinCommitted: (BaziSkin) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val originalSkin = remember { selectedSkin }
    val initialPage = BaziSkin.entries.indexOf(originalSkin).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { BaziSkin.entries.size }
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHapticFeedback()
    val tokens = LocalBaziSkinTokens.current
    val dismissAndRestore = {
        onSkinPreview(originalSkin)
        onDismissRequest()
    }
    LaunchedEffect(pagerState) {
        var previousPage = initialPage
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != previousPage) {
                    haptics.perform(AppHapticEvent.SNAP)
                    onSkinPreview(BaziSkin.entries[page])
                    previousPage = page
                }
            }
    }
    Dialog(
        onDismissRequest = dismissAndRestore,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 640.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 17.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "选择视觉皮肤",
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "上下滑动逐套预览，停稳后立即应用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textSecondary,
                        )
                    }
                    Text(
                        "关闭",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = dismissAndRestore)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BaziSkinPickerTrackHeight),
                ) {
                    val cardWidth = minOf(maxWidth * 0.985f, 720.dp)
                    VerticalPager(
                        state = pagerState,
                        pageSize = PageSize.Fixed(BaziSkinPickerCardHeight),
                        contentPadding = PaddingValues(
                            vertical = (maxHeight - BaziSkinPickerCardHeight) / 2,
                        ),
                        pageSpacing = 12.dp,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val offset = {
                            ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .coerceIn(-1f, 1f)
                        }
                        BaziSkinPreviewCard(
                            skin = BaziSkin.entries[page],
                            selected = page == pagerState.currentPage,
                            pageOffset = offset,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(BaziSkinPickerCardHeight),
                            onClick = { scope.launch { pagerState.animateScrollToPage(page) } },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${BaziSkin.entries.size}",
                        modifier = Modifier.weight(1f),
                        color = tokens.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Button(
                        onClick = {
                            haptics.perform(AppHapticEvent.CONFIRM)
                            onSkinCommitted(BaziSkin.entries[pagerState.currentPage])
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.primary,
                            contentColor = Color.White,
                        ),
                    ) { Text("完成", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun BaziSkinPreviewCard(
    skin: BaziSkin,
    selected: Boolean,
    pageOffset: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = skin.tokens
    val selectedAccent = LocalBaziSkinTokens.current.primary
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            val signedOffset = pageOffset()
            val distance = signedOffset.absoluteValue
            scaleX = 1f - (0.14f * distance)
            scaleY = 1f - (0.22f * distance)
            alpha = 1f - (0.34f * distance)
            rotationX = signedOffset * 26f
            translationY = -signedOffset * 8f * density
            cameraDistance = 20f * density
            transformOrigin = TransformOrigin.Center
        },
        shape = BaziSkinPickerShape,
        color = preview.surface,
        shadowElevation = if (selected) 5.dp else 0.dp,
    ) {
        Box {
            BaziSkinArtwork(skin.visualRecipe, Modifier.fillMaxSize())
            Text(
                skin.displayName,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(preview.surface.copy(alpha = 0.90f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                color = preview.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            if (selected) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(preview.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(selectedAccent),
                    )
                    Text(
                        "当前预览",
                        color = selectedAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
