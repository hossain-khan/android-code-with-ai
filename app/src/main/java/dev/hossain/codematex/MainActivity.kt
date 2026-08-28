package dev.hossain.codematex

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.navstack.rememberSaveableNavStack
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.hossain.codematex.di.ActivityKey
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.home.HomeScreen
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

/**
 * Main activity for the application, demonstrating Metro constructor injection for Activities.
 *
 * This Activity is injected via constructor using Metro DI, enabled by [ComposeAppComponentFactory].
 *
 * Key Metro features demonstrated:
 * - [ActivityKey]: Map key annotation for multibinding
 * - [ContributesIntoMap]: Contributes this Activity to the multibinding map
 * - [Inject]: Marks this class for constructor injection
 * - [binding]: Type-safe binding helper for specifying the bound type
 *
 * The Activity receives its dependencies ([Circuit]) through constructor injection,
 * which is more testable and type-safe than field injection.
 *
 * See https://zacsweers.github.io/metro/latest/injection-types/#constructor-injection for constructor injection.
 * See https://zacsweers.github.io/metro/latest/bindings/#multibindings for multibindings.
 * See https://zacsweers.github.io/metro/latest/aggregation/ for contribution.
 *
 * Note: [@Inject][Inject] is now implicit when using [@ContributesIntoMap][ContributesIntoMap]
 * as of Metro 0.10.0 with `contributesAsInject` enabled by default.
 */
@ActivityKey(MainActivity::class)
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class MainActivity
    constructor(
        private val circuit: Circuit,
    ) : ComponentActivity() {
        private var pendingDeepLinkScreen by mutableStateOf<Screen?>(null)

        @OptIn(ExperimentalSharedTransitionApi::class)
        override fun onCreate(savedInstanceState: Bundle?) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            handleIntent(intent)

            setContent {
                CodeWithAIAppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        // Cold-start deep link initialization: if launched directly via deep link,
                        // seed the backstack with HomeScreen -> TargetScreen so the back button works naturally.
                        val initialStack =
                            remember {
                                if (pendingDeepLinkScreen != null) {
                                    val target = pendingDeepLinkScreen!!
                                    pendingDeepLinkScreen = null
                                    listOf(HomeScreen, target)
                                } else {
                                    listOf(HomeScreen)
                                }
                            }
                        val navStack = rememberSaveableNavStack(initialStack)
                        val navigator = rememberCircuitNavigator(navStack)

                        // Warm-start deep link handling: navigate to the deep-linked screen if already running
                        LaunchedEffect(pendingDeepLinkScreen) {
                            val target = pendingDeepLinkScreen
                            if (target != null) {
                                if (navStack.topRecord?.screen != target) {
                                    navigator.goTo(target)
                                }
                                pendingDeepLinkScreen = null
                            }
                        }

                        // See https://slackhq.github.io/circuit/circuit-content/
                        HighlightThemeProvider(
                            lightHighlightTheme = rememberTomorrowLightTheme(),
                            darkHighlightTheme = rememberTomorrowNightTheme(),
                        ) {
                            CircuitCompositionLocals(circuit) {
                                // See https://slackhq.github.io/circuit/shared-elements/
                                SharedElementTransitionLayout {
                                    // See https://slackhq.github.io/circuit/overlays/
                                    ContentWithOverlays {
                                        NavigableCircuitContent(
                                            navigator = navigator,
                                            navStack = navStack,
                                            decoratorFactory =
                                                remember {
                                                    GestureNavigationDecorationFactory()
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            handleIntent(intent)
        }

        private fun handleIntent(intent: Intent?) {
            val uri = intent?.data
            if ((uri?.scheme == "codematex" && uri.host == "models") ||
                intent?.getStringExtra(EXTRA_TARGET_SCREEN) == SCREEN_MODELS
            ) {
                pendingDeepLinkScreen = ModelPickerScreen
            }
        }

        companion object {
            const val EXTRA_TARGET_SCREEN = "extra_target_screen"
            const val SCREEN_MODELS = "models"
        }
    }
