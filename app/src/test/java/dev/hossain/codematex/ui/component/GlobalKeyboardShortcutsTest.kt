package dev.hossain.codematex.ui.component

import androidx.compose.ui.input.key.KeyEventType
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
    fun `evaluateGlobalShortcut with Ctrl or Meta + N returns NewChatOrHome`() {
        val command =
            evaluateGlobalShortcut(
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = true,
                canPopBack = false,
            )

        assertThat(command).isEqualTo(GlobalShortcutCommand.NewChatOrHome)
    }

    @Test
    fun `evaluateGlobalShortcut with Ctrl or Meta + Comma returns OpenSettings`() {
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

    // --- handleGlobalKeyEvent Tests ---

    @Test
    fun `handleGlobalKeyEvent with KeyUp returns false and invokes no callbacks`() {
        var backCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyUp,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
                onNavigateBack = { backCalled = true },
                onNewChatOrHome = {},
                onOpenSettings = {},
            )

        assertThat(handled).isFalse()
        assertThat(backCalled).isFalse()
    }

    @Test
    fun `handleGlobalKeyEvent with repeatCount greater than zero returns false and does not pop`() {
        var backCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 1,
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
                onNavigateBack = { backCalled = true },
                onNewChatOrHome = {},
                onOpenSettings = {},
            )

        assertThat(handled).isFalse()
        assertThat(backCalled).isFalse()
    }

    @Test
    fun `handleGlobalKeyEvent with Escape when canPopBack is true invokes onNavigateBack and returns true`() {
        var backCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
                onNavigateBack = { backCalled = true },
                onNewChatOrHome = {},
                onOpenSettings = {},
            )

        assertThat(handled).isTrue()
        assertThat(backCalled).isTrue()
    }

    @Test
    fun `handleGlobalKeyEvent with Escape when canPopBack is false returns false`() {
        var backCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_ESCAPE,
                isCtrlOrMetaPressed = false,
                canPopBack = false,
                onNavigateBack = { backCalled = true },
                onNewChatOrHome = {},
                onOpenSettings = {},
            )

        assertThat(handled).isFalse()
        assertThat(backCalled).isFalse()
    }

    @Test
    fun `handleGlobalKeyEvent with Ctrl+N invokes onNewChatOrHome and returns true`() {
        var newChatCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = true,
                canPopBack = true,
                onNavigateBack = {},
                onNewChatOrHome = { newChatCalled = true },
                onOpenSettings = {},
            )

        assertThat(handled).isTrue()
        assertThat(newChatCalled).isTrue()
    }

    @Test
    fun `handleGlobalKeyEvent with Ctrl+Comma invokes onOpenSettings and returns true`() {
        var settingsCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_COMMA,
                isCtrlOrMetaPressed = true,
                canPopBack = true,
                onNavigateBack = {},
                onNewChatOrHome = {},
                onOpenSettings = { settingsCalled = true },
            )

        assertThat(handled).isTrue()
        assertThat(settingsCalled).isTrue()
    }

    @Test
    fun `handleGlobalKeyEvent with unmodified key typing returns false and invokes no callbacks`() {
        var anyCalled = false
        val handled =
            handleGlobalKeyEvent(
                type = KeyEventType.KeyDown,
                repeatCount = 0,
                keyCode = AndroidKeyEvent.KEYCODE_N,
                isCtrlOrMetaPressed = false,
                canPopBack = true,
                onNavigateBack = { anyCalled = true },
                onNewChatOrHome = { anyCalled = true },
                onOpenSettings = { anyCalled = true },
            )

        assertThat(handled).isFalse()
        assertThat(anyCalled).isFalse()
    }
}
