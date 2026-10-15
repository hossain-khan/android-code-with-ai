package dev.hossain.codematex.ui.screens.chatsessions

import android.view.KeyEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionHistoryShortcutsTest {
    @Test
    fun `evaluateSessionHistoryShortcut with Ctrl+F returns true`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = KeyEvent.KEYCODE_F,
                isCtrlOrMetaPressed = true,
            )

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `evaluateSessionHistoryShortcut with Meta+F returns true`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = KeyEvent.KEYCODE_F,
                isCtrlOrMetaPressed = true,
            )

        assertThat(isHandled).isTrue()
    }

    @Test
    fun `evaluateSessionHistoryShortcut with unmodified F returns false`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = KeyEvent.KEYCODE_F,
                isCtrlOrMetaPressed = false,
            )

        assertThat(isHandled).isFalse()
    }

    @Test
    fun `evaluateSessionHistoryShortcut with Ctrl and another key returns false`() {
        val isHandled =
            evaluateSessionHistoryShortcut(
                keyCode = KeyEvent.KEYCODE_A,
                isCtrlOrMetaPressed = true,
            )

        assertThat(isHandled).isFalse()
    }
}
