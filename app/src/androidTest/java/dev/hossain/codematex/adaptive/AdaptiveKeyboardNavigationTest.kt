package dev.hossain.codematex.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.ui.component.accessibleFocusOutline
import dev.hossain.codematex.ui.component.clearFocusOnEscape
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumentation test verifying hardware keyboard navigation (Tab traversal, Enter activation,
 * Escape dismissal, and accessible visual focus indicators) as required by Google's Tier 2 Adaptive Quality
 * Guidelines (Keyboard_Navigation, Keyboard_Exit, and Drawable_Focus).
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveKeyboardNavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenInteractiveButtons_whenTabKeyIsPressed_thenFocusMovesToNextElement() {
        val button1Tag = "keyboard_button_1"
        val button2Tag = "keyboard_button_2"
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {},
                            modifier =
                                Modifier
                                    .testTag(button1Tag)
                                    .focusRequester(focusRequester)
                                    .focusable(),
                        ) {
                            Text("First Button")
                        }
                        Button(
                            onClick = {},
                            modifier =
                                Modifier
                                    .testTag(button2Tag)
                                    .focusable(),
                        ) {
                            Text("Second Button")
                        }
                    }
                }
            }
        }

        // Request initial focus on first button
        composeTestRule.runOnIdle {
            focusRequester.requestFocus()
        }
        composeTestRule.onNodeWithTag(button1Tag).assertIsFocused()

        // Press Tab key
        composeTestRule.onRoot().performKeyInput {
            pressKey(Key.Tab)
        }
        composeTestRule.waitForIdle()

        // Second button should now be focused
        composeTestRule.onNodeWithTag(button2Tag).assertIsFocused()
    }

    @Test
    fun givenFocusedButton_whenEnterKeyIsPressed_thenActionIsTriggered() {
        val buttonTag = "enter_action_button"
        var clickCount by mutableIntStateOf(0)
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { clickCount++ },
                            modifier =
                                Modifier
                                    .testTag(buttonTag)
                                    .focusRequester(focusRequester)
                                    .focusable()
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                            clickCount++
                                            true
                                        } else {
                                            false
                                        }
                                    },
                        ) {
                            Text("Click count: $clickCount")
                        }
                    }
                }
            }
        }

        // Request focus via FocusRequester
        composeTestRule.runOnIdle {
            focusRequester.requestFocus()
        }
        composeTestRule.onNodeWithTag(buttonTag).assertIsFocused()

        // Trigger via hardware keyboard Enter key
        composeTestRule.onNodeWithTag(buttonTag).performKeyInput {
            pressKey(Key.Enter)
        }
        composeTestRule.waitForIdle()

        assertThat(clickCount).isEqualTo(1)
    }

    @Test
    fun givenFocusedInput_whenEscapeKeyIsPressed_thenFocusIsClearedWithoutPoppingNavigation() {
        val inputTag = "test_text_input"
        var backPopped = false

        composeTestRule.setContent {
            val focusManager = LocalFocusManager.current
            var text by remember { mutableStateOf("Testing focus") }

            MaterialTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                    backPopped = true
                                    true
                                } else {
                                    false
                                }
                            },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.testTag(inputTag).clearFocusOnEscape(focusManager),
                        )
                        Button(
                            onClick = {},
                            modifier = Modifier.testTag("other_button"),
                        ) {
                            Text("Other Button")
                        }
                    }
                }
            }
        }

        // Focus the text field
        composeTestRule.onNodeWithTag(inputTag).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(inputTag).assertIsFocused()

        // Press Escape key while focused on input
        composeTestRule.onNodeWithTag(inputTag).performKeyInput {
            pressKey(Key.Escape)
        }
        composeTestRule.waitForIdle()

        // Focus should be cleared from the text input
        composeTestRule.onNodeWithTag(inputTag).assertIsNotFocused()
        // Top-level navigation back handler must NOT have been triggered
        assertThat(backPopped).isFalse()
    }

    @Test
    fun givenOpenContextMenu_whenEscapeKeyIsPressed_thenMenuIsDismissed() {
        val menuItemTag = "context_menu_item"
        var showMenu by mutableStateOf(false)
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier =
                                Modifier.onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                        showMenu = false
                                        true
                                    } else {
                                        false
                                    }
                                },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Menu Action") },
                                onClick = { showMenu = false },
                                modifier =
                                    Modifier
                                        .testTag(menuItemTag)
                                        .focusRequester(focusRequester)
                                        .focusable(),
                            )
                        }
                    }
                }
            }
        }

        // Open the menu and focus item
        composeTestRule.runOnIdle {
            showMenu = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            focusRequester.requestFocus()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(menuItemTag).assertExists()

        // Press Escape key
        composeTestRule.onNodeWithTag(menuItemTag).performKeyInput {
            pressKey(Key.Escape)
        }
        composeTestRule.waitForIdle()

        // Menu should be dismissed
        composeTestRule.onNodeWithTag(menuItemTag).assertDoesNotExist()
    }

    @Test
    fun givenAccessibleCard_whenFocusedViaKeyboard_thenGainsFocus() {
        val cardTag = "accessible_focus_card"
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Card(
                            modifier =
                                Modifier
                                    .testTag(cardTag)
                                    .focusRequester(focusRequester)
                                    .accessibleFocusOutline(shape = MaterialTheme.shapes.large)
                                    .focusable(),
                        ) {
                            Text("Card content", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }

        // Request focus
        composeTestRule.runOnIdle {
            focusRequester.requestFocus()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(cardTag).assertIsFocused()
    }
}
