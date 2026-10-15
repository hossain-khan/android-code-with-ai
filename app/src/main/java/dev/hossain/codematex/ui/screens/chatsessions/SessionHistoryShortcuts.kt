package dev.hossain.codematex.ui.screens.chatsessions

import android.view.KeyEvent

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
): Boolean = isCtrlOrMetaPressed && keyCode == KeyEvent.KEYCODE_F
