package dev.hossain.codematex.ui.screens.aimodels.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.ui.component.handHoverIcon
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import java.text.DecimalFormat

/**
 * Banner displaying the device's physical and available memory information,
 * and offering an unload action when an LLM model is actively loaded in RAM.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceMemoryBanner(
    deviceMemoryInfo: DeviceMemoryInfo,
    modifier: Modifier = Modifier,
    availableMemoryGb: Float = 0f,
    isModelLoadedInMemory: Boolean = false,
    activeBackend: LlmEngine.Backend? = null,
    isUnloadingModel: Boolean = false,
    onUnloadModel: () -> Unit = {},
) {
    val ramFormatter = remember { DecimalFormat("#,##0.0") }
    val memoryHeadline =
        if (availableMemoryGb > 0f) {
            "${ramFormatter.format(availableMemoryGb)} ${deviceMemoryInfo.displayLabel} Available • " +
                "${ramFormatter.format(deviceMemoryInfo.displayTotalGb)} ${deviceMemoryInfo.displayLabel} Total"
        } else {
            "${ramFormatter.format(deviceMemoryInfo.displayTotalGb)} ${deviceMemoryInfo.displayLabel} Total RAM"
        }

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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
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
                        memoryHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            if (isModelLoadedInMemory) {
                                "Model loaded in memory${activeBackend?.let { " (${it.name})" } ?: ""}. Unload to free up RAM."
                            } else {
                                "Models requiring more RAM than available may be disabled for stability."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isModelLoadedInMemory) {
                FilledTonalButton(
                    onClick = onUnloadModel,
                    enabled = !isUnloadingModel,
                    modifier = Modifier.fillMaxWidth().handHoverIcon(),
                ) {
                    if (isUnloadingModel) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUnloadingModel) "Unloading Model..." else "Unload Model from Memory")
                }
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
                availableMemoryGb = 4.8f,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun DeviceMemoryBannerLoadedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DeviceMemoryBanner(
                deviceMemoryInfo =
                    DeviceMemoryInfo(
                        totalBytes = 12_000_000_000L,
                        displayTotalGb = 11.5,
                        displayLabel = "GB",
                    ),
                availableMemoryGb = 2.4f,
                isModelLoadedInMemory = true,
                activeBackend = LlmEngine.Backend.GPU,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
