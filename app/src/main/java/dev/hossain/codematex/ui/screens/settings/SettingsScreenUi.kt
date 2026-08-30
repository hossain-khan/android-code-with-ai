package dev.hossain.codematex.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.R
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SettingsScreen::class, AppScope::class)
@Composable
fun SettingsScreenUi(
    state: SettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SettingsScreen.State.Content -> SettingsContent(state, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsScreen.State.Content,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SettingsScreen.Event.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Section 1: AI Tutor & Persona
            SettingsSectionHeader(title = "AI Tutor & Persona")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.Person,
                        title = "Active Persona",
                        subtitle =
                            "${state.selectedPersona.iconGlyph} ${state.selectedPersona.displayName} • " +
                                state.selectedPersona.tagline,
                        onClick = { state.eventSink(SettingsScreen.Event.ShowPersonaDialog(true)) },
                    )
                }
            }

            // Section 2: Performance & Hardware
            SettingsSectionHeader(title = "Performance & Hardware")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.Speed,
                        title = "Background RAM Eviction Timeout",
                        subtitle = getRamEvictionSubtitle(state.ramEvictionMinutes),
                        onClick = { state.eventSink(SettingsScreen.Event.ShowRamEvictionDialog(true)) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Wifi,
                        title = "Download over Wi-Fi Only",
                        subtitle = "Prevents downloading 1.5–2.5 GB model files over mobile data networks",
                        checked = state.isWifiOnlyDownload,
                        onCheckedChange = { state.eventSink(SettingsScreen.Event.WifiOnlyToggled(it)) },
                    )
                }
            }

            // Section 3: Code & Tactile Feedback
            SettingsSectionHeader(title = "Code Display & Interactions")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.Code,
                        title = "Code Block Display & Themes",
                        subtitle = "Theme: ${state.codeTheme.displayName} • Line numbers: ${if (state.showLineNumbers) "On" else "Off"}",
                        onClick = { state.eventSink(SettingsScreen.Event.CodeBlockSettingsClicked) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on copy, quiz selections, and key actions",
                        checked = state.hapticFeedbackEnabled,
                        onCheckedChange = { state.eventSink(SettingsScreen.Event.HapticsToggled(it)) },
                    )
                }
            }

            // Section 4: Data & Storage
            SettingsSectionHeader(title = "Storage & Data Management")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.Default.Memory,
                        title = "Downloaded Models Storage",
                        subtitle = formatStorageSize(state.storageUsedBytes, state.downloadedModelCount),
                        onClick = { state.eventSink(SettingsScreen.Event.ManageModelsClicked) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    SettingsClickableRow(
                        icon = Icons.Default.DeleteOutline,
                        title = "Clear Chat History",
                        subtitle = "${state.sessionCount} saved session(s) in local storage",
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { state.eventSink(SettingsScreen.Event.ShowClearHistoryDialog(true)) },
                    )
                }
            }

            // Section 5: App & Feedback
            SettingsSectionHeader(title = "About & Feedback")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SettingsClickableRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Replay App Onboarding Tour",
                        subtitle = "View the 4-slide orientation guide anytime",
                        onClick = { state.eventSink(SettingsScreen.Event.ReplayTourClicked) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    SettingsClickableRowWithCustomIcon(
                        iconRes = R.drawable.github_logo,
                        title = "Share Feedback on GitHub",
                        subtitle = "Report bugs, request models, and join discussions",
                        onClick = { uriHandler.openUri(OnboardingScreen.GITHUB_ISSUES_URL) },
                    )
                }
            }

            // Footer version
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "CodeMateX ${state.appVersion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "100% Private On-Device AI • Powered by Google LiteRT-LM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (state.showPersonaDialog) {
        PersonaSelectionDialog(
            currentPersona = state.selectedPersona,
            onPersonaSelected = { state.eventSink(SettingsScreen.Event.PersonaSelected(it)) },
            onDismiss = { state.eventSink(SettingsScreen.Event.ShowPersonaDialog(false)) },
        )
    }

    if (state.showRamEvictionDialog) {
        RamEvictionDialog(
            currentMinutes = state.ramEvictionMinutes,
            onMinutesSelected = { state.eventSink(SettingsScreen.Event.RamEvictionSelected(it)) },
            onDismiss = { state.eventSink(SettingsScreen.Event.ShowRamEvictionDialog(false)) },
        )
    }

    if (state.showClearHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = { state.eventSink(SettingsScreen.Event.ShowClearHistoryDialog(false)) },
            title = { Text("Clear All Chat History?") },
            text = {
                Text(
                    "This will permanently delete all saved chat sessions and messages from your device. This action cannot be undone.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { state.eventSink(SettingsScreen.Event.ConfirmClearHistory) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { state.eventSink(SettingsScreen.Event.ShowClearHistoryDialog(false)) },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SettingsClickableRowWithCustomIcon(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun PersonaSelectionDialog(
    currentPersona: TutorPersona,
    onPersonaSelected: (TutorPersona) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tutor Persona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TutorPersona.entries.forEach { persona ->
                    val isSelected = persona == currentPersona
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().clickable { onPersonaSelected(persona) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onPersonaSelected(persona) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${persona.iconGlyph} ${persona.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = persona.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun RamEvictionDialog(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options =
        listOf(
            0 to "Immediate on exit (Max memory saving)",
            1 to "1 minute",
            3 to "3 minutes [Recommended]",
            5 to "5 minutes",
            10 to "10 minutes (Keeps warm longer)",
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RAM Eviction Timeout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Controls how long the multi-gigabyte neural network remains in RAM after backgrounding the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                options.forEach { (minutes, label) ->
                    val isSelected = minutes == currentMinutes
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onMinutesSelected(minutes) }
                                .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onMinutesSelected(minutes) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun getRamEvictionSubtitle(minutes: Int): String =
    when (minutes) {
        0 -> "Immediate on exit"
        1 -> "1 minute"
        3 -> "3 minutes [Recommended]"
        5 -> "5 minutes"
        10 -> "10 minutes"
        else -> "$minutes minutes"
    }

private fun formatStorageSize(
    bytes: Long,
    modelCount: Int,
): String {
    if (bytes <= 0L) return "0 MB used • 0 downloaded models"
    val gigabytes = bytes / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.2f GB used • %d downloaded model%s", gigabytes, modelCount, if (modelCount == 1) "" else "s")
}

@ThemePreviews
@Composable
private fun SettingsScreenUiPreview() {
    CodeWithAIAppTheme {
        Surface {
            SettingsScreenUi(
                state =
                    SettingsScreen.State.Content(
                        selectedPersona = TutorPersona.SENIOR_ENGINEER,
                        isWifiOnlyDownload = true,
                        showLineNumbers = true,
                        hapticFeedbackEnabled = true,
                        ramEvictionMinutes = 3,
                        storageUsedBytes = 2_700_000_000L,
                        downloadedModelCount = 1,
                        sessionCount = 3,
                        appVersion = "v1.16.3",
                        eventSink = {},
                    ),
            )
        }
    }
}
