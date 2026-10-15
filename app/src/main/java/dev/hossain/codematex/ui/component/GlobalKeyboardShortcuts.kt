package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Commands triggered by global keyboard shortcuts.
 */
internal enum class GlobalShortcutCommand {
    NavigateBack,
    NewChatOrHome,
    OpenSettings,
}

/**
 * Pure evaluation function that determines if a key combination triggers a global shortcut command.
 *
 * @param keyCode Key code corresponding to [android.view.KeyEvent.getKeyCode].
 * @param isCtrlOrMetaPressed Whether Ctrl or Meta (Cmd) modifier is active.
 * @param canPopBack Whether back navigation is currently possible (e.g. backstack size > 1).
 * @return The corresponding [GlobalShortcutCommand], or null if unhandled.
 */
internal fun evaluateGlobalShortcut(
    keyCode: Int,
    isCtrlOrMetaPressed: Boolean,
    canPopBack: Boolean,
): GlobalShortcutCommand? =
    when {
        keyCode == android.view.KeyEvent.KEYCODE_ESCAPE && canPopBack -> {
            GlobalShortcutCommand.NavigateBack
        }

        isCtrlOrMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_N -> {
            GlobalShortcutCommand.NewChatOrHome
        }

        isCtrlOrMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_COMMA -> {
            GlobalShortcutCommand.OpenSettings
        }

        else -> {
            null
        }
    }

/**
 * Dispatches a [GlobalShortcutCommand] to the appropriate navigation callback.
 *
 * @return True if a command was handled, false otherwise.
 */
internal fun dispatchGlobalShortcutCommand(
    command: GlobalShortcutCommand?,
    onNavigateBack: () -> Unit,
    onNewChatOrHome: () -> Unit,
    onOpenSettings: () -> Unit,
): Boolean =
    when (command) {
        GlobalShortcutCommand.NavigateBack -> {
            onNavigateBack()
            true
        }

        GlobalShortcutCommand.NewChatOrHome -> {
            onNewChatOrHome()
            true
        }

        GlobalShortcutCommand.OpenSettings -> {
            onOpenSettings()
            true
        }

        null -> {
            false
        }
    }

/**
 * Evaluates and dispatches global shortcuts using primitive parameters.
 *
 * Normal typing (unmodified keys like 'n', ',', etc.) is never intercepted.
 * Repeated key down events (repeatCount > 0) are ignored to prevent rapid multi-pop or repeated navigation loops.
 */
internal fun handleGlobalKeyEvent(
    type: KeyEventType,
    repeatCount: Int,
    keyCode: Int,
    isCtrlOrMetaPressed: Boolean,
    canPopBack: Boolean,
    onNavigateBack: () -> Unit,
    onNewChatOrHome: () -> Unit,
    onOpenSettings: () -> Unit,
): Boolean {
    if (type != KeyEventType.KeyDown || repeatCount > 0) return false

    val command =
        evaluateGlobalShortcut(
            keyCode = keyCode,
            isCtrlOrMetaPressed = isCtrlOrMetaPressed,
            canPopBack = canPopBack,
        )

    return dispatchGlobalShortcutCommand(
        command = command,
        onNavigateBack = onNavigateBack,
        onNewChatOrHome = onNewChatOrHome,
        onOpenSettings = onOpenSettings,
    )
}

/**
 * Handles global application keyboard shortcuts.
 *
 * Supported shortcuts:
 * - [Key.Escape]: Navigates back if [canPopBack] is true.
 * - Ctrl / Cmd + [Key.N]: Navigates to HomeScreen / new chat via [onNewChatOrHome].
 * - Ctrl / Cmd + [Key.Comma]: Navigates to Settings via [onOpenSettings].
 *
 * Normal typing (unmodified keys like 'n', ',', etc.) is never intercepted and passes through.
 * Repeated key down events (repeatCount > 0) are ignored to prevent multi-pop or repeated navigation loops.
 *
 * @param event The key event to evaluate.
 * @param canPopBack Whether back navigation is currently possible (e.g. backstack size > 1).
 * @param onNavigateBack Invoked when [Key.Escape] is pressed and [canPopBack] is true.
 * @param onNewChatOrHome Invoked when Ctrl / Cmd + [Key.N] is pressed.
 * @param onOpenSettings Invoked when Ctrl / Cmd + [Key.Comma] is pressed.
 * @return True if the key event was consumed as a global shortcut, false otherwise.
 */
internal fun handleGlobalKeyEvent(
    event: KeyEvent,
    canPopBack: Boolean,
    onNavigateBack: () -> Unit,
    onNewChatOrHome: () -> Unit,
    onOpenSettings: () -> Unit,
): Boolean =
    handleGlobalKeyEvent(
        type = event.type,
        repeatCount = event.nativeKeyEvent.repeatCount,
        keyCode = event.nativeKeyEvent.keyCode,
        isCtrlOrMetaPressed = event.isCtrlPressed || event.isMetaPressed,
        canPopBack = canPopBack,
        onNavigateBack = onNavigateBack,
        onNewChatOrHome = onNewChatOrHome,
        onOpenSettings = onOpenSettings,
    )

/**
 * Modifier that intercepts global keyboard shortcuts (Ctrl / Cmd + N, Ctrl / Cmd + Comma, Escape)
 * during the bubble phase and dispatches them to navigation handlers.
 */
internal fun Modifier.globalKeyboardShortcuts(
    canPopBack: Boolean,
    onNavigateBack: () -> Unit,
    onNewChatOrHome: () -> Unit,
    onOpenSettings: () -> Unit,
): Modifier =
    onKeyEvent { event ->
        handleGlobalKeyEvent(
            event = event,
            canPopBack = canPopBack,
            onNavigateBack = onNavigateBack,
            onNewChatOrHome = onNewChatOrHome,
            onOpenSettings = onOpenSettings,
        )
    }

/**
 * Modifier that intercepts global keyboard shortcuts using a pre-constructed key event handler.
 */
internal fun Modifier.globalKeyboardShortcuts(onKeyEvent: (KeyEvent) -> Boolean): Modifier = onKeyEvent(onKeyEvent)
