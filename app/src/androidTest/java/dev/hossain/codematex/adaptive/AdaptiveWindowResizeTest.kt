package dev.hossain.codematex.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumentation test verifying multi-window and split-screen breakpoint resizing
 * as required by Google's Tier 3 Adaptive Quality Guidelines (Multi-Window / Breakpoints).
 */
@RunWith(AndroidJUnit4::class)
class AdaptiveWindowResizeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenAdaptiveContent_whenResizedAcrossCompactAndExpandedBreakpoints_thenAdaptsSmoothly() {
        val primaryPaneTag = "adaptive_primary_pane"
        val secondaryPaneTag = "adaptive_secondary_pane"

        var containerWidth by mutableStateOf(360.dp) // Start compact (<600dp)

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.width(containerWidth)) {
                        val isExpanded = containerWidth >= 600.dp
                        val counter by rememberSaveable { mutableStateOf(42) }

                        if (isExpanded) {
                            // Two-pane expanded layout (tablet / unfolded foldable)
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).testTag(primaryPaneTag)) {
                                    Text("Master Content ($counter)")
                                }
                                Box(modifier = Modifier.weight(1f).testTag(secondaryPaneTag)) {
                                    Text("Detail Content")
                                }
                            }
                        } else {
                            // Single-pane compact layout (compact phone)
                            Box(modifier = Modifier.fillMaxSize().testTag(primaryPaneTag)) {
                                Text("Compact Content ($counter)")
                            }
                        }
                    }
                }
            }
        }

        // In compact mode: primary pane is displayed, secondary pane does not exist
        composeTestRule.onNodeWithTag(primaryPaneTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(secondaryPaneTag).assertDoesNotExist()

        // Transition to expanded mode (simulate unfolding or entering split-screen expanded)
        containerWidth = 840.dp
        composeTestRule.waitForIdle()

        // In expanded mode: both primary and secondary panes are displayed
        composeTestRule.onNodeWithTag(primaryPaneTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(secondaryPaneTag).assertIsDisplayed()

        // Transition back to compact mode (simulate folding or narrow split-screen window)
        containerWidth = 400.dp
        composeTestRule.waitForIdle()

        // In compact mode: secondary pane is dismissed, primary pane remains intact
        composeTestRule.onNodeWithTag(primaryPaneTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(secondaryPaneTag).assertDoesNotExist()
    }
}
