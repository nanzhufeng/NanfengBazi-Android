package com.nanzhufeng.nanfengbazi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 四柱录入与反查入口共用的“柱位—天干—地支”展示，不允许按入口各自着色。 */
@Composable
internal fun FourPillarsInputGrid(
    pillars: List<String>,
    selectedPillarIndex: Int? = null,
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
            Column(
                modifier = Modifier.weight(1f),
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
                        NanfengNavigation
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                BaziCharacterSlot(stem)
                BaziCharacterSlot(branch)
            }
        }
    }
}

@Composable
private fun BaziCharacterSlot(character: Char?) {
    Surface(
        modifier = Modifier.size(54.dp),
        shape = CircleShape,
        color = baziElementContainerColor(character),
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
