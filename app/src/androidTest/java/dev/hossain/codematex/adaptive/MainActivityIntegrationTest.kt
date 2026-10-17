package dev.hossain.codematex.adaptive

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end activity integration test verifying that [MainActivity] launches successfully
 * and survives configuration recreation (such as rotation or multi-window resizing) without crashes.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesAndSurvivesActivityRecreation() {
        // Assert initial activity is launched and running
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
            assertThat(activity.isDestroyed).isFalse()
        }

        // Recreate activity simulating orientation rotation or multi-window configuration change
        composeTestRule.activityRule.scenario.recreate()

        // Assert recreated activity is alive and healthy
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
            assertThat(activity.isDestroyed).isFalse()
        }
    }
}
