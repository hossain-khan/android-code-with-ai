package dev.hossain.codematex.ui.screens.home

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.subcircuit.SubCircuitContent
import dev.hossain.codematex.ui.animation.LocalSharedElementTransitionScope
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.screens.home.courses.GuidedCoursesOuterEvent
import dev.hossain.codematex.ui.screens.home.courses.GuidedCoursesSubScreen
import dev.hossain.codematex.ui.screens.home.model.ActiveModelBannerOuterEvent
import dev.hossain.codematex.ui.screens.home.model.ActiveModelBannerSubScreen
import dev.hossain.codematex.ui.screens.home.sessions.RecentSessionsOuterEvent
import dev.hossain.codematex.ui.screens.home.sessions.RecentSessionsSubScreen
import dev.hossain.codematex.ui.screens.home.topics.TopicsGridOuterEvent
import dev.hossain.codematex.ui.screens.home.topics.TopicsGridSubScreen
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeScreenContent(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    if (SharedElementTransitionScope.isAvailable) {
        SharedElementTransitionScope {
            CompositionLocalProvider(LocalSharedElementTransitionScope provides this) {
                HomeScreenInnerContent(state = state, modifier = modifier)
            }
        }
    } else {
        HomeScreenInnerContent(state = state, modifier = modifier)
    }
}

@Composable
private fun HomeScreenInnerContent(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is HomeScreen.State.IneligibleDevice -> {
            IneligibleDeviceLayout(state, modifier)
        }

        is HomeScreen.State.Success -> {
            HomeLayout(state, modifier)
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun HomeLayout(
    state: HomeScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { state.eventSink(HomeScreen.Event.AppTour) },
                                ),
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text("CodeMateX", fontWeight = FontWeight.Bold)
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.ViewAllSessions) }) {
                        Icon(Icons.Default.History, contentDescription = "Session History")
                    }
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.OpenSettings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isExpanded) {
            // Adaptive 2-Column Multi-Pane Layout for Tablets / Foldables / Landscape
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Left Column: Active Model Banner + Guided Courses + Topics Grid
                Column(
                    modifier =
                        Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SubCircuitContent(
                        screen = ActiveModelBannerSubScreen,
                        outerEventSink = { event ->
                            when (event) {
                                ActiveModelBannerOuterEvent.NavigateToModelPicker -> {
                                    state.eventSink(HomeScreen.Event.ManageModels)
                                }
                            }
                        },
                    )

                    SubCircuitContent(
                        screen = GuidedCoursesSubScreen,
                        outerEventSink = { event ->
                            when (event) {
                                is GuidedCoursesOuterEvent.NavigateToCourse -> {
                                    state.eventSink(HomeScreen.Event.CourseClicked(event.courseId))
                                }

                                GuidedCoursesOuterEvent.NavigateToAllCourses -> {
                                    state.eventSink(HomeScreen.Event.GuidedLessons)
                                }
                            }
                        },
                    )

                    SubCircuitContent(
                        screen = TopicsGridSubScreen(isCompact = false),
                        modifier = Modifier.weight(1f),
                        outerEventSink = { event ->
                            when (event) {
                                is TopicsGridOuterEvent.NavigateToTopic -> {
                                    state.eventSink(HomeScreen.Event.TopicSelected(event.topic))
                                }
                            }
                        },
                    )
                }

                // Right Column: Recent Sessions
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    SubCircuitContent(
                        screen = RecentSessionsSubScreen(isExpanded = true),
                        outerEventSink = { event ->
                            when (event) {
                                is RecentSessionsOuterEvent.NavigateToSession -> {
                                    state.eventSink(HomeScreen.Event.SessionSelected(event.topic, event.sessionId))
                                }

                                RecentSessionsOuterEvent.NavigateToAllSessions -> {
                                    state.eventSink(HomeScreen.Event.ViewAllSessions)
                                }
                            }
                        },
                    )
                }
            }
        } else {
            // Compact Single Column Layout
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    SubCircuitContent(
                        screen = ActiveModelBannerSubScreen,
                        outerEventSink = { event ->
                            when (event) {
                                ActiveModelBannerOuterEvent.NavigateToModelPicker -> {
                                    state.eventSink(HomeScreen.Event.ManageModels)
                                }
                            }
                        },
                    )
                }

                item {
                    SubCircuitContent(
                        screen = GuidedCoursesSubScreen,
                        outerEventSink = { event ->
                            when (event) {
                                is GuidedCoursesOuterEvent.NavigateToCourse -> {
                                    state.eventSink(HomeScreen.Event.CourseClicked(event.courseId))
                                }

                                GuidedCoursesOuterEvent.NavigateToAllCourses -> {
                                    state.eventSink(HomeScreen.Event.GuidedLessons)
                                }
                            }
                        },
                    )
                }

                item {
                    SubCircuitContent(
                        screen = TopicsGridSubScreen(isCompact = true),
                        outerEventSink = { event ->
                            when (event) {
                                is TopicsGridOuterEvent.NavigateToTopic -> {
                                    state.eventSink(HomeScreen.Event.TopicSelected(event.topic))
                                }
                            }
                        },
                    )
                }

                item {
                    SubCircuitContent(
                        screen = RecentSessionsSubScreen(isExpanded = false),
                        outerEventSink = { event ->
                            when (event) {
                                is RecentSessionsOuterEvent.NavigateToSession -> {
                                    state.eventSink(HomeScreen.Event.SessionSelected(event.topic, event.sessionId))
                                }

                                RecentSessionsOuterEvent.NavigateToAllSessions -> {
                                    state.eventSink(HomeScreen.Event.ViewAllSessions)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IneligibleDeviceLayout(
    state: HomeScreen.State.IneligibleDevice,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
        topBar = {
            TopAppBar(
                title = { Text("Hardware Compatibility", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier =
                    Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    Text(
                        text = "High-Performance Device Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    // Hardware Specs Card
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Required RAM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "8.0 GB+",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Detected RAM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f GB", state.detectedRamGb),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (state.detectedRamGb >= 7.2) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Architecture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text =
                                        if (state.is64BitSupported) {
                                            "64-bit (ARM64/x86_64)"
                                        } else {
                                            "32-bit (Unsupported)"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (state.is64BitSupported) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                )
                            }
                        }
                    }

                    Text(
                        text =
                            "Running local on-device LLMs requires substantial RAM and GPU compute shaders. " +
                                "Lower-spec hardware can cause out-of-memory crashes or system freezes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )

                    Button(
                        onClick = { state.eventSink(HomeScreen.Event.DismissIneligibilityWarning) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Proceed Anyway (May Be Unstable)")
                    }
                }
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

@DevicePreviews
@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HomeLayout(
            state = HomeScreen.State.Success(eventSink = {}),
        )
    }
}

@ThemePreviews
@Composable
private fun IneligibleDevicePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        IneligibleDeviceLayout(
            state =
                HomeScreen.State.IneligibleDevice(
                    reason = "Device has 4.0 GB of RAM. Minimum required is 8.0 GB.",
                    detectedRamGb = 4.0,
                    minRequiredRamGb = 8.0,
                    is64BitSupported = true,
                    eventSink = {},
                ),
        )
    }
}
