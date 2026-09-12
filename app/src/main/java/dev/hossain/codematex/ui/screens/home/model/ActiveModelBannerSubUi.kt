package dev.hossain.codematex.ui.screens.home.model

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.ui.animation.ActiveModelBadgeSharedKey
import dev.hossain.codematex.ui.animation.ActiveModelCardSharedKey
import dev.hossain.codematex.ui.animation.ActiveModelTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.animation.sharedElementNav
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Composable rendering the active AI model hero banner on the home dashboard.
 */
@Composable
fun ActiveModelBannerSubUi(
    state: ActiveModelBannerSubState,
    modifier: Modifier = Modifier,
) {
    HeroBanner(
        state = state,
        modifier = modifier,
    )
}

/**
 * Hero card presenting the local AI tutor branding and live on-device LLM runtime telemetry.
 */
@Composable
internal fun HeroBanner(
    state: ActiveModelBannerSubState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    "On-Device AI Tutor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Ask & Learn Locally",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Text(
                "Run optimized LLMs locally on your device with zero cloud latency and complete code privacy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Dedicated Model & Memory Status Bar
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .then(
                            if (state.hasDownloadedModel) {
                                Modifier.sharedBoundsNav(ActiveModelCardSharedKey)
                            } else {
                                Modifier
                            },
                        ).clickable { state.eventSink(ActiveModelBannerUiEvent.ManageModels) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint =
                                if (state.hasDownloadedModel) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text =
                                if (state.hasDownloadedModel) {
                                    state.selectedModelName ?: "AI Model Ready"
                                } else {
                                    "No Model Selected"
                                },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                if (state.hasDownloadedModel) {
                                    Modifier.sharedBoundsNav(ActiveModelTitleSharedKey)
                                } else {
                                    Modifier
                                },
                        )
                    }

                    if (state.hasDownloadedModel) {
                        Surface(
                            shape = CircleShape,
                            color =
                                if (state.isModelInMemory) {
                                    Color(0xFF2E7D32).copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                                },
                            border =
                                BorderStroke(
                                    1.dp,
                                    if (state.isModelInMemory) {
                                        Color(0xFF2E7D32).copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    },
                                ),
                            modifier = Modifier.sharedElementNav(ActiveModelBadgeSharedKey),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.isModelInMemory) {
                                                    Color(0xFF2E7D32)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                },
                                            ),
                                )
                                Text(
                                    text =
                                        if (state.isModelInMemory) {
                                            "In RAM • ${state.memoryBackend ?: "GPU"}"
                                        } else {
                                            "Ready (On Storage)"
                                        },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color =
                                        if (state.isModelInMemory) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = "Setup required",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Factory creating [SubUi] instances for [ActiveModelBannerSubScreen].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class ActiveModelBannerSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is ActiveModelBannerSubScreen -> {
                SubUi<ActiveModelBannerSubState> { state, modifier ->
                    ActiveModelBannerSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun ActiveModelBannerSubUiLoadedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ActiveModelBannerSubUi(
                state =
                    ActiveModelBannerSubState(
                        hasDownloadedModel = true,
                        selectedModelName = "Gemma 2 2B IT",
                        isModelInMemory = true,
                        memoryBackend = "GPU",
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ActiveModelBannerSubUiNoModelPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ActiveModelBannerSubUi(
                state =
                    ActiveModelBannerSubState(
                        hasDownloadedModel = false,
                        selectedModelName = null,
                        isModelInMemory = false,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
