package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal enum class PillarEditPart {
    STEM,
    BRANCH,
}

/** 四柱录入与反查入口共用的“柱位—天干—地支”展示，不允许按入口各自着色。 */
@Composable
internal fun FourPillarsInputGrid(
    pillars: List<String>,
    selectedPillarIndex: Int? = null,
    activeEditPart: PillarEditPart? = null,
    onEditPartSelected: ((Int, PillarEditPart) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillarLabels.forEachIndexed { index, label ->
            val value = pillars.getOrNull(index).orEmpty()
            val stem = value.getOrNull(0)
            val branch = value.getOrNull(1)
            val isSelectedPillar = selectedPillarIndex == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (selectedPillarIndex != null) {
                            Modifier.testTag("lookup_pillar_summary_$label")
                        } else {
                            Modifier
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedPillarIndex == index) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (selectedPillarIndex == index) {
                        NanfengGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                BaziCharacterSlot(
                    character = stem,
                    label = "${label}天干",
                    isActive = isSelectedPillar && activeEditPart == PillarEditPart.STEM,
                    isInSelectedPillar = isSelectedPillar,
                    tag = "lookup_pillar_${label}_stem_slot",
                    onClick = onEditPartSelected?.let { callback ->
                        { callback(index, PillarEditPart.STEM) }
                    },
                )
                BaziCharacterSlot(
                    character = branch,
                    label = "${label}地支",
                    isActive = isSelectedPillar && activeEditPart == PillarEditPart.BRANCH,
                    isInSelectedPillar = isSelectedPillar,
                    tag = "lookup_pillar_${label}_branch_slot",
                    onClick = onEditPartSelected?.let { callback ->
                        { callback(index, PillarEditPart.BRANCH) }
                    },
                )
            }
        }
    }
}

@Composable
private fun BaziCharacterSlot(
    character: Char?,
    label: String,
    isActive: Boolean,
    isInSelectedPillar: Boolean,
    tag: String,
    onClick: (() -> Unit)?,
) {
    val elementColor = character?.let(::baziElementColor) ?: NanfengNavigationMuted
    val haloColor by animateColorAsState(
        targetValue = if (isActive) elementColor.copy(alpha = 0.13f) else Color.Transparent,
        label = "pillar-slot-halo",
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            isActive -> elementColor.copy(alpha = 0.52f)
            isInSelectedPillar -> elementColor.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        label = "pillar-slot-ring",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isInSelectedPillar && character != null) {
            baziElementSelectedContainerColor(character)
        } else {
            baziElementContainerColor(character)
        },
        label = "pillar-slot-container",
    )
    val elevation by animateDpAsState(
        targetValue = if (isActive) 4.dp else 0.dp,
        label = "pillar-slot-elevation",
    )
    val slotModifier = Modifier
        .size(60.dp)
        .then(
            if (onClick != null) {
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = label
                        stateDescription = if (isActive) "正在编辑" else "可编辑"
                    }
                    .testTag(tag)
            } else {
                Modifier
            },
        )
    Box(modifier = slotModifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(haloColor, CircleShape),
        )
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, ringColor),
            shadowElevation = elevation,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    character?.toString().orEmpty(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = character?.let(::baziElementColor)
                        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                )
            }
        }
    }
}
