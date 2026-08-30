package dev.hossain.codematex.ui.screens.settings.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.DeveloperExperienceLevel
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.DeveloperProfilePreset
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeveloperProfileSettingsScreen::class, AppScope::class)
@Composable
fun DeveloperProfileSettingsScreenUi(
    state: DeveloperProfileSettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is DeveloperProfileSettingsScreen.State.Content -> DeveloperProfileSettingsContent(state, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperProfileSettingsContent(
    state: DeveloperProfileSettingsScreen.State.Content,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Developer Context",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeveloperProfileSettingsScreen.Event.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { state.eventSink(DeveloperProfileSettingsScreen.Event.ResetClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
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
            // Hero / Informational Banner
            HeroExplainerCard()

            // Master Toggle Card
            MasterToggleCard(
                enabled = state.profile.enabled,
                onToggle = { state.eventSink(DeveloperProfileSettingsScreen.Event.EnabledToggled(it)) },
            )

            // One-Tap Starter Presets
            StarterPresetsSection(
                onPresetSelected = { state.eventSink(DeveloperProfileSettingsScreen.Event.PresetApplied(it)) },
            )

            // Section 1: Experience Level
            SectionHeader(title = "Experience Level", icon = Icons.Default.School)
            ExperienceLevelCard(
                selectedLevel = state.profile.experienceLevel,
                onSelectLevel = { state.eventSink(DeveloperProfileSettingsScreen.Event.ExperienceLevelSelected(it)) },
            )

            // Section 2: Primary Tech Stack
            SectionHeader(title = "Primary Tech Stack & Languages", icon = Icons.Default.Code)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "List your primary languages, frameworks, or architecture patterns:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.profile.primaryStack,
                        onValueChange = { state.eventSink(DeveloperProfileSettingsScreen.Event.PrimaryStackChanged(it)) },
                        placeholder = {
                            Text(
                                "e.g. Kotlin, Jetpack Compose, Coroutines, KMP, Python",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                    )
                }
            }

            // Section 3: Custom Directives & Preferences
            SectionHeader(title = "Custom Guidelines & Preferences", icon = Icons.Default.Description)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Specific instructions for how the AI tutor should format answers:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.profile.customDirectives,
                        onValueChange = { state.eventSink(DeveloperProfileSettingsScreen.Event.CustomDirectivesChanged(it)) },
                        placeholder = {
                            Text(
                                "e.g. Skip basic syntax explanations. Provide code-first solutions with idiomatic Kotlin Coroutines. Focus on memory safety and lifecycle trade-offs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                    )
                }
            }

            // Section 4: Live Generated Prompt Preview
            SectionHeader(title = "Generated System Prompt Preview", icon = Icons.Default.AutoAwesome)
            PromptPreviewCard(
                promptSnippet = state.generatedPromptSnippet,
                enabled = state.profile.enabled,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroExplainerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = "Personalized AI Tutoring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tailored explanations without prompt repetition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text =
                    "Define your experience level, tech stack, and preferences once. " +
                        "The on-device model factors this context into its system prompt across all chat sessions and guided courses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "100% on-device & private. Stored locally in DataStore.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(!enabled) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Custom Developer Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (enabled) "Profile directives are active across all AI chats" else "Vanilla tutoring prompts will be used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun StarterPresetsSection(
    onPresetSelected: (DeveloperProfilePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = "Quick Starter Presets", icon = Icons.Default.AutoAwesome)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeveloperProfilePreset.entries.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.title, style = MaterialTheme.typography.labelMedium) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

@Composable
private fun ExperienceLevelCard(
    selectedLevel: DeveloperExperienceLevel,
    onSelectLevel: (DeveloperExperienceLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column {
            DeveloperExperienceLevel.entries.forEachIndexed { index, level ->
                val isSelected = level == selectedLevel
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLevel(level) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectLevel(level) },
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = level.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < DeveloperExperienceLevel.entries.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptPreviewCard(
    promptSnippet: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "System Prompt Addendum",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (enabled) "Active" else "Inactive (Disabled)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            ) {
                Text(
                    text = promptSnippet.ifBlank { "=== USER DEVELOPER PROFILE ===\n(No custom stack or directives configured)" },
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@ThemePreviews
@Composable
private fun DeveloperProfileSettingsScreenUiPreview() {
    CodeWithAIAppTheme {
        Surface {
            DeveloperProfileSettingsScreenUi(
                state =
                    DeveloperProfileSettingsScreen.State.Content(
                        profile =
                            DeveloperProfile(
                                enabled = true,
                                experienceLevel = DeveloperExperienceLevel.SENIOR,
                                primaryStack = "Kotlin, Jetpack Compose, Coroutines",
                                customDirectives = "Provide idiomatic Kotlin code with modern Compose best practices.",
                            ),
                        generatedPromptSnippet =
                            """=== USER DEVELOPER PROFILE ===
                            |- Experience Level: Senior (Experienced in production systems, clean architecture, and performance tuning.)
                            |- Primary Tech Stack: Kotlin, Jetpack Compose, Coroutines
                            |- Custom Directives & Preferences: Provide idiomatic Kotlin code with modern Compose best practices.
                            """.trimMargin(),
                        eventSink = {},
                    ),
            )
        }
    }
}
