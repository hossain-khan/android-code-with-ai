package dev.hossain.codematex.ui.screens.aimodels.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import java.text.DecimalFormat

/**
 * Banner displaying the device's physical memory information and LLM RAM requirement guidance.
 */
@Composable
fun DeviceMemoryBanner(
    deviceMemoryInfo: DeviceMemoryInfo,
    modifier: Modifier = Modifier,
) {
    val ramFormatter = remember { DecimalFormat("#,##0.0") }
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Device Memory",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${ramFormatter.format(deviceMemoryInfo.displayTotalGb)} ${deviceMemoryInfo.displayLabel} Total RAM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Models requiring more RAM than available may be disabled for stability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DeviceMemoryBannerPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DeviceMemoryBanner(
                deviceMemoryInfo =
                    DeviceMemoryInfo(
                        totalBytes = 12_000_000_000L,
                        displayTotalGb = 11.5,
                        displayLabel = "GB",
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
