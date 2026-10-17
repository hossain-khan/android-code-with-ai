package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Modifier that detects secondary pointer presses (such as mouse right-click or trackpad secondary tap)
 * and invokes [onSecondaryClick] with the click position [Offset].
 *
 * Normal primary clicks, touch interactions, and gestures are not consumed and flow through naturally.
 * Implemented via [ModifierNodeElement] to maintain a stable pointer event coroutine across recompositions.
 *
 * @param enabled Whether secondary click detection is active.
 * @param onSecondaryClick Callback invoked with the pointer event offset when a secondary click occurs.
 */
fun Modifier.onSecondaryClick(
    enabled: Boolean = true,
    onSecondaryClick: (Offset) -> Unit,
): Modifier = this then SecondaryClickElement(enabled = enabled, onSecondaryClick = onSecondaryClick)

/**
 * Container composable that attaches secondary-click (right-click or secondary tap)
 * context menu support to its [content] and displays [menuContent] anchored directly
 * beside the pointer click position.
 *
 * Anchors the [DropdownMenu] to a zero-sized Box positioned at the pointer offset so that
 * the menu opens at the click position while preserving window bounds and collision behavior.
 *
 * @param modifier The modifier to apply to the outer container.
 * @param enabled Whether secondary click detection is active.
 * @param menuContent The menu items to display, providing a dismiss callback to close the menu.
 * @param content The main composable content to display inside the container.
 */
@Composable
fun SecondaryClickContextMenuBox(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var showContextMenu by remember { mutableStateOf(false) }
    var clickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            modifier.onSecondaryClick(enabled = enabled) { offset ->
                clickOffset = offset
                showContextMenu = true
            },
    ) {
        content()

        if (showContextMenu) {
            val (xDp, yDp) =
                with(density) {
                    clickOffset.x.toDp() to clickOffset.y.toDp()
                }
            Box(
                modifier =
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(x = xDp, y = yDp)
                        .size(0.dp),
            ) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    modifier =
                        Modifier.onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                showContextMenu = false
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    menuContent { showContextMenu = false }
                }
            }
        }
    }
}

internal data class SecondaryClickElement(
    val enabled: Boolean,
    val onSecondaryClick: (Offset) -> Unit,
) : ModifierNodeElement<SecondaryClickNode>() {
    override fun create(): SecondaryClickNode = SecondaryClickNode(enabled = enabled, onSecondaryClick = onSecondaryClick)

    override fun update(node: SecondaryClickNode) {
        node.update(enabled = enabled, onSecondaryClick = onSecondaryClick)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onSecondaryClick"
        properties["enabled"] = enabled
        properties["onSecondaryClick"] = onSecondaryClick
    }
}

internal class SecondaryClickNode(
    var enabled: Boolean,
    var onSecondaryClick: (Offset) -> Unit,
) : DelegatingNode(),
    PointerInputModifierNode {
    private val pointerInputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (enabled && event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                            event.changes.forEach { it.consume() }
                            onSecondaryClick(position)
                        }
                    }
                }
            },
        )

    fun update(
        enabled: Boolean,
        onSecondaryClick: (Offset) -> Unit,
    ) {
        this.enabled = enabled
        this.onSecondaryClick = onSecondaryClick
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }
}
