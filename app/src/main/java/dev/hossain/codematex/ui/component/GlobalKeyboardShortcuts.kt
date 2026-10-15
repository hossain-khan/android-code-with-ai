package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
 * Pure evaluation function that determines if a key combination corresponds to
 * the session history screen filter shortcut (Ctrl / Cmd + F).
 *
 * @param keyCode Key code corresponding to [android.view.KeyEvent.getKeyCode].
 * @param isCtrlOrMetaPressed Whether Ctrl or Meta (Cmd) modifier is active.
 * @return True if the key combination matches Ctrl/Cmd + F, false otherwise.
 */
internal fun evaluateSessionHistoryShortcut(
    keyCode: Int,
    isCtrlOrMetaPressed: Boolean,
): Boolean = isCtrlOrMetaPressed && keyCode == android.view.KeyEvent.KEYCODE_F

/**
 * Handles global application keyboard shortcuts.
 *
 * Supported shortcuts:
 * - [Key.Escape]: Navigates back if [canPopBack] is true.
 * - Ctrl / Cmd + [Key.N]: Navigates to HomeScreen / new chat via [onNewChatOrHome].
 * - Ctrl / Cmd + [Key.Comma]: Navigates to Settings via [onOpenSettings].
 *
 * Normal typing (unmodified keys like 'n', ',', etc.) is never intercepted and passes through.
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
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    val isModifierActive = event.isCtrlPressed || event.isMetaPressed
    val command =
        evaluateGlobalShortcut(
            keyCode = event.nativeKeyEvent.keyCode,
            isCtrlOrMetaPressed = isModifierActive,
            canPopBack = canPopBack,
        ) ?: return false

    return when (command) {
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
    }
}

/**
 * Modifier that intercepts global keyboard shortcuts (Ctrl / Cmd + N, Ctrl / Cmd + Comma, Escape)
 * and dispatches them to navigation handlers.
 */
internal fun Modifier.globalKeyboardShortcuts(
    canPopBack: Boolean,
    onNavigateBack: () -> Unit,
    onNewChatOrHome: () -> Unit,
    onOpenSettings: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        handleGlobalKeyEvent(
            event = event,
            canPopBack = canPopBack,
            onNavigateBack = onNavigateBack,
            onNewChatOrHome = onNewChatOrHome,
            onOpenSettings = onOpenSettings,
        )
    }
