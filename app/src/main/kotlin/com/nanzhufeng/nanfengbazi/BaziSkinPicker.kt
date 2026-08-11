package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val BaziSkinPickerShape = RoundedCornerShape(20.dp)
private val BaziSkinPickerCardHeight = 84.dp
private val BaziSkinPickerTrackHeight = 252.dp

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
    modifier: Modifier = Modifier,
) {
    val recipe = skin.visualRecipe
    val tokens = skin.tokens
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Box {
            BaziSkinArtwork(recipe, Modifier.fillMaxSize())
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
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 17.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "南枫八字",
                    color = recipe.headerContentColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    skin.headline,
                    color = recipe.headerContentColor.copy(alpha = 0.94f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    skin.subhead,
                    color = recipe.headerContentColor.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "八字",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface.copy(alpha = 0.22f))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                color = recipe.headerContentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
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
