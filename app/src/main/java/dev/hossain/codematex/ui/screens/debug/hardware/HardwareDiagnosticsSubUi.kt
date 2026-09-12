package dev.hossain.codematex.ui.screens.debug.hardware

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun HardwareDiagnosticsSubUi(
    state: HardwareDiagnosticsSubState,
    modifier: Modifier = Modifier,
) {
    HardwareDiagnosticsCard(
        deviceInfo = state.deviceInfo,
        runtimeSpecs = state.runtimeSpecs,
        eligibility = state.eligibility,
        isDevMode = state.isDevMode,
        modifier = modifier,
    )
}

@Composable
internal fun HardwareDiagnosticsCard(
    deviceInfo: Map<String, String>,
    runtimeSpecs: Map<String, String>,
    eligibility: HardwareEligibility,
    isDevMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row: Title + Eligibility Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Hardware & Runtime Specs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val (badgeLabel, badgeBg, badgeFg) =
                    when (eligibility) {
                        is HardwareEligibility.Eligible -> {
                            if (isDevMode) {
                                Triple(
                                    "Eligible (Dev Mode)",
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            } else {
                                Triple(
                                    "Hardware Eligible",
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }

                        is HardwareEligibility.Ineligible -> {
                            Triple(
                                "Ineligible",
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                Surface(shape = CircleShape, color = badgeBg) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Ineligibility Warning Banner if applicable
            if (eligibility is HardwareEligibility.Ineligible) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = eligibility.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Sub-section: Device & Architecture Specs
            Text(
                text = "Device & Architecture",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            deviceInfo.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Sub-section: LiteRT-LM Runtime & Acceleration Delegates
            Text(
                text = "LiteRT-LM Runtime & Delegates",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            runtimeSpecs.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class HardwareDiagnosticsSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is HardwareDiagnosticsSubScreen -> {
                SubUi<HardwareDiagnosticsSubState> { state, modifier ->
                    HardwareDiagnosticsSubUi(state, modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun HardwareDiagnosticsSubUiPreview() {
    CodeWithAIAppTheme {
        Surface {
            HardwareDiagnosticsSubUi(
                state =
                    HardwareDiagnosticsSubState(
                        deviceInfo =
                            mapOf(
                                "Device Model" to "Pixel 8 Pro",
                                "Android OS" to "Android 14 (API 34)",
                                "CPU Cores" to "8 cores",
                                "Authoritative RAM" to "12.00 GB",
                            ),
                        runtimeSpecs =
                            mapOf(
                                "Inference Runtime" to "Google LiteRT-LM",
                                "Active Backend" to "GPU",
                            ),
                        eligibility = HardwareEligibility.Eligible,
                        isDevMode = false,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
