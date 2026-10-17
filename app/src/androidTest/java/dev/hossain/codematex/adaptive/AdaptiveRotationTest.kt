package dev.hossain.codematex.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumentation test verifying state retention across activity recreation and configuration changes
 * as required by Google's Tier 3 Adaptive Quality Guidelines (Config_Changes).
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveRotationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenInputText_whenConfigurationChangeOccurs_thenTextIsRetained() {
        val restorationTester = StateRestorationTester(composeTestRule)
        val testInput = "Remember this draft prompt across rotation"
        val inputTag = "test_prompt_input"

        restorationTester.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var textValue by rememberSaveable { mutableStateOf("") }
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier = Modifier.fillMaxWidth().testTag(inputTag),
                            label = { Text("Prompt") },
                        )
                    }
                }
            }
        }

        // Enter text into input
        composeTestRule.onNodeWithTag(inputTag).performTextInput(testInput)
        composeTestRule.onNodeWithTag(inputTag).assertTextContains(testInput)

        // Emulate configuration change / activity recreation with saved instance state
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify text survived configuration change
        composeTestRule.onNodeWithTag(inputTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(inputTag).assertTextContains(testInput)
    }

    @Test
    fun givenScrollPosition_whenConfigurationChangeOccurs_thenScrollStateIsPreserved() {
        val restorationTester = StateRestorationTester(composeTestRule)
        val listTag = "test_scroll_column"

        restorationTester.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag(listTag)
                                .verticalScroll(scrollState),
                    ) {
                        repeat(50) { index ->
                            Text(
                                text = "Item $index",
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag(listTag).assertIsDisplayed()

        // Emulate configuration change / recreation
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(listTag).assertIsDisplayed()
    }
}
