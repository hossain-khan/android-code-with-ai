package dev.hossain.codematex.ui.screens.settings.code

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodeFontSize
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCodeDefaults
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(CodeBlockSettingsScreen::class, AppScope::class)
@Composable
fun CodeBlockSettingsScreenUi(
    state: CodeBlockSettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is CodeBlockSettingsScreen.State.Content -> CodeBlockSettingsContent(state, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHighlightApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CodeBlockSettingsContent(
    state: CodeBlockSettingsScreen.State.Content,
    modifier: Modifier = Modifier,
) {
    val (lightTheme, darkTheme) =
        remember(state.settings.theme) {
            state.settings.theme.resolveHighlightThemes()
        }

    val baseStyle =
        if (state.settings.preset == CodeBlockPreset.COMPACT) {
            CodeBlockStyle.Compact
        } else {
            CodeBlockStyle.Default
        }

    val effectiveStyle =
        remember(baseStyle, state.settings.fontSize) {
            baseStyle.copy(
                textStyle =
                    baseStyle.textStyle.copy(
                        fontSize = state.settings.fontSize.sizeSp.sp,
                        lineHeight = (state.settings.fontSize.sizeSp * 1.35f).sp,
                    ),
            )
        }

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Code Block Display",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(CodeBlockSettingsScreen.Event.BackClicked) }) {
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
            // Live Preview Section
            SectionHeader(title = "Live Preview", icon = Icons.Default.Palette)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    HighlightThemeProvider(
                        lightHighlightTheme = lightTheme,
                        darkHighlightTheme = darkTheme,
                    ) {
                        StreamingSyntaxHighlightedCode(
                            code = state.previewCode,
                            language = "kotlin",
                            showLineNumbers = state.settings.showLineNumbers,
                            style = effectiveStyle,
                            languageLabel =
                                if (state.settings.showLanguageLabel) {
                                    null // Uses Default Language Label
                                } else {
                                    { }
                                },
                            copyButton =
                                if (state.settings.showCopyButton) {
                                    null // Uses Default Copy Button
                                } else {
                                    { }
                                },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Section 1: Syntax Themes
            SectionHeader(title = "Syntax Highlighting Theme", icon = Icons.Default.Palette)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    CodeTheme.entries.forEachIndexed { index, theme ->
                        val isSelected = theme == state.settings.theme
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { state.eventSink(CodeBlockSettingsScreen.Event.ThemeSelected(theme)) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { state.eventSink(CodeBlockSettingsScreen.Event.ThemeSelected(theme)) },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = theme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (index < CodeTheme.entries.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }

            // Section 2: Header & Actions
            SectionHeader(title = "Header & Top Bar", icon = Icons.AutoMirrored.Filled.Label)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SwitchRow(
                        icon = Icons.AutoMirrored.Filled.Label,
                        title = "Language Identifier Badge",
                        subtitle = "Displays programming language badge (e.g. 'kotlin', 'python') in code header",
                        checked = state.settings.showLanguageLabel,
                        onCheckedChange = { state.eventSink(CodeBlockSettingsScreen.Event.LanguageLabelToggled(it)) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    SwitchRow(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy Code Button",
                        subtitle = "One-tap button in code header to copy snippet to clipboard",
                        checked = state.settings.showCopyButton,
                        onCheckedChange = { state.eventSink(CodeBlockSettingsScreen.Event.CopyButtonToggled(it)) },
                    )
                }
            }

            // Section 3: Line Numbers & Layout Density
            SectionHeader(title = "Layout & Line Numbers", icon = Icons.Default.FormatListNumbered)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column {
                    SwitchRow(
                        icon = Icons.Default.FormatListNumbered,
                        title = "Show Line Numbers",
                        subtitle = "Display left-side line numbering gutter in code blocks",
                        checked = state.settings.showLineNumbers,
                        onCheckedChange = { state.eventSink(CodeBlockSettingsScreen.Event.LineNumbersToggled(it)) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Block Spacing & Padding",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Adjust padding density around code content",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CodeBlockPreset.entries.forEach { preset ->
                                val isSelected = preset == state.settings.preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { state.eventSink(CodeBlockSettingsScreen.Event.PresetSelected(preset)) },
                                    label = { Text(preset.displayName) },
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Font Sizing
            SectionHeader(title = "Code Font Size", icon = Icons.Default.FormatSize)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Text scale within code blocks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CodeFontSize.entries.forEach { size ->
                            val isSelected = size == state.settings.fontSize
                            FilterChip(
                                selected = isSelected,
                                onClick = { state.eventSink(CodeBlockSettingsScreen.Event.FontSizeSelected(size)) },
                                label = { Text(size.displayName) },
                                modifier = Modifier.weight(1f),
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun SwitchRow(
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

@ThemePreviews
@Composable
private fun CodeBlockSettingsScreenUiPreview() {
    CodeWithAIAppTheme {
        Surface {
            CodeBlockSettingsScreenUi(
                state =
                    CodeBlockSettingsScreen.State.Content(
                        settings = CodeBlockSettings(),
                        eventSink = {},
                    ),
            )
        }
    }
}
