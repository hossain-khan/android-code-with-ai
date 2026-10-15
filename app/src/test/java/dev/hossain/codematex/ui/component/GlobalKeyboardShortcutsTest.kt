package dev.hossain.codematex.ui.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import android.view.KeyEvent as AndroidKeyEvent

class GlobalKeyboardShortcutsTest {
    @Test
    fun `evaluateGlobalShortcut with Escape when canPopBack is true returns NavigateBack`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
            )

        assertThat(command).isEqualTo(GlobalShortcutCommand.NavigateBack)
    }

    @Test
    fun `evaluateGlobalShortcut with Escape when canPopBack is false returns null`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = false,
            )

        assertThat(command).isNull()
    }

    @Test
    fun `evaluateGlobalShortcut with Ctrl+N returns NewChatOrHome`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = true,
                canPopBack = false,
            )

        assertThat(command).isEqualTo(GlobalShortcutCommand.NewChatOrHome)
    }

    @Test
    fun `evaluateGlobalShortcut with Meta+N returns NewChatOrHome`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = true,
                canPopBack = true,
            )

        assertThat(command).isEqualTo(GlobalShortcutCommand.NewChatOrHome)
    }

    @Test
    fun `evaluateGlobalShortcut with Ctrl+Comma returns OpenSettings`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_COMMA,
                isCtrlOrMetaPressed = true,
                canPopBack = false,
            )

        assertThat(command).isEqualTo(GlobalShortcutCommand.OpenSettings)
    }

    @Test
    fun `evaluateGlobalShortcut with unmodified N returns null`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
            )

        assertThat(command).isNull()
    }

    @Test
    fun `evaluateGlobalShortcut with unmodified Comma returns null`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_COMMA,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
            )

        assertThat(command).isNull()
    }

    @Test
    fun `evaluateGlobalShortcut with unrelated key returns null`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_A,
                isCtrlOrMetaPressed = true,
                canPopBack = true,
            )

        assertThat(command).isNull()
    }

    // --- Session History Screen Shortcut Tests ---

    @Test
    fun `evaluateSessionHistoryShortcut with Ctrl+F returns true`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_F,
                isCtrlOrMetaPressed = true,
            )

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `evaluateSessionHistoryShortcut with unmodified F returns false`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_F,
                isCtrlOrMetaPressed = false,
            )

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `evaluateSessionHistoryShortcut with Ctrl and other key returns false`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_A,
                isCtrlOrMetaPressed = true,
            )

        assertThat(isHandled).isFalse()
    }
}
