package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Modifier that displays a pointing hand cursor ([PointerIcon.Hand]) when hovered
 * by a mouse or trackpad pointer.
 *
 * @param enabled Whether the hand cursor should be displayed. Defaults to true.
 */
fun Modifier.handHoverIcon(enabled: Boolean = true): Modifier = if (enabled) pointerHoverIcon(PointerIcon.Hand) else this

/**
 * Preferred placement of a tooltip relative to its anchor element.
 */
enum class TooltipPosition {
    Above,
    Below,
}

/**
 * An [IconButton] wrapped in a Material 3 [TooltipBox] that displays a plain tooltip
 * on mouse/trackpad hover or long-press.
 *
 * Automatically displays [PointerIcon.Hand] when hovered.
 *
 * @param tooltip The text to display inside the tooltip.
 * @param onClick Callback invoked when the button is clicked.
 * @param modifier Modifier applied to the outer [TooltipBox].
 * @param enabled Whether the icon button is interactive.
 * @param colors The colors to use for the [IconButton].
 * @param position The preferred tooltip anchor placement relative to the icon button. Defaults to [TooltipPosition.Below].
 * @param content The icon or content to display inside the icon button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnchoredTooltip(
    tooltip: String,
    position: TooltipPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val anchorPosition =
        when (position) {
            TooltipPosition.Above -> TooltipAnchorPosition.Above
            TooltipPosition.Below -> TooltipAnchorPosition.Below
        }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = anchorPosition),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * An [IconButton] wrapped in a Material 3 [TooltipBox] that displays a plain tooltip
 * on mouse/trackpad hover or long-press.
 *
 * Automatically displays [PointerIcon.Hand] when hovered.
 *
 * @param tooltip The text to display inside the tooltip.
 * @param onClick Callback invoked when the button is clicked.
 * @param modifier Modifier applied to the outer [TooltipBox].
 * @param enabled Whether the icon button is interactive.
 * @param colors The colors to use for the [IconButton].
 * @param position The preferred tooltip anchor placement relative to the icon button. Defaults to [TooltipPosition.Below].
 * @param content The icon or content to display inside the icon button.
 */
@Composable
fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    position: TooltipPosition = TooltipPosition.Below,
    content: @Composable () -> Unit,
) {
    AnchoredTooltip(
        tooltip = tooltip,
        position = position,
        modifier = modifier,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            modifier = Modifier.handHoverIcon(enabled = enabled),
        ) {
            content()
        }
    }
}

/**
 * Wraps [content] in a Material 3 [TooltipBox] that displays [tooltip]
 * on mouse/trackpad hover or long-press.
 *
 * @param tooltip The text to display inside the plain tooltip.
 * @param modifier Modifier applied to the outer [TooltipBox].
 * @param position The preferred tooltip anchor placement. Defaults to [TooltipPosition.Below].
 * @param content The composable content to display inside the tooltip box.
 */
@Composable
fun ComponentTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    position: TooltipPosition = TooltipPosition.Below,
    content: @Composable () -> Unit,
) {
    AnchoredTooltip(
        tooltip = tooltip,
        position = position,
        modifier = modifier,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemePreviews
@Composable
private fun TooltipIconButtonPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            TooltipIconButton(
                tooltip = "Back",
                onClick = {},
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemePreviews
@Composable
private fun ComponentTooltipPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ComponentTooltip(
                tooltip = "Information details",
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                )
            }
        }
    }
}
