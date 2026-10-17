package dev.hossain.codematex.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Modifier that renders a high-contrast visual focus outline when the component is focused
 * via hardware keyboard or DPAD navigation, satisfying Google's Tier 2 Drawable_Focus guideline.
 *
 * @param shape The shape of the focus outline, matching the component's geometry.
 * @param color The outline color, defaulting to [ColorScheme.primary].
 * @param width The stroke width of the focus outline. Defaults to 2.dp.
 */
@Composable
fun Modifier.accessibleFocusOutline(
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 2.dp,
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) {
                Modifier.border(width, color, shape)
            } else {
                Modifier
            },
        )
}

/**
 * Modifier that clears keyboard focus when the physical [Key.Escape] key is pressed.
 *
 * This ensures that pressing Escape while typing in a text field, search bar, or input control
 * clears focus from that control rather than popping the navigation backstack or closing the screen,
 * satisfying Google's Tier 2 Keyboard_Exit guideline.
 * A subsequent Escape press after focus has already been cleared will trigger standard back navigation.
 */
fun Modifier.clearFocusOnEscape(focusManager: FocusManager): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            focusManager.clearFocus(force = true)
            true
        } else {
            false
        }
    }

// ==========================================
// Previews
// ==========================================

@ThemePreviews
@Composable
private fun AccessibleFocusIndicatorPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Focused Card State (Active Focus Ring):",
                    style = MaterialTheme.typography.labelMedium,
                )
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Interactive card with high-contrast focus outline",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Text(
                    text = "Unfocused Card State:",
                    style = MaterialTheme.typography.labelMedium,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Interactive card in default unfocused state",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
