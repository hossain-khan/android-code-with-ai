package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier that detects secondary pointer presses (such as mouse right-click or trackpad secondary tap)
 * and invokes [onSecondaryClick] with the click position [Offset].
 *
 * Normal primary clicks, touch interactions, and gestures are not consumed and flow through naturally.
 *
 * @param enabled Whether secondary click detection is active.
 * @param onSecondaryClick Callback invoked with the pointer event offset when a secondary click occurs.
 */
fun Modifier.onSecondaryClick(
    enabled: Boolean = true,
    onSecondaryClick: (Offset) -> Unit,
): Modifier =
    if (enabled) {
        pointerInput(onSecondaryClick) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                        event.changes.forEach { it.consume() }
                        onSecondaryClick(position)
                    }
                }
            }
        }
    } else {
        this
    }
