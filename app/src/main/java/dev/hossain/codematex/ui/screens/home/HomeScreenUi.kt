package dev.hossain.codematex.ui.screens.home

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun HomeScreenContent(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is HomeScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is HomeScreen.State.IneligibleDevice -> {
            IneligibleDeviceLayout(state, modifier)
        }

        is HomeScreen.State.Success -> {
            HomeLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun HomeLayout(
    state: HomeScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    NotificationPermissionHandler()

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
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.GuidedLessons) }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Guided Lessons")
                    }
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.ManageModels) }) {
                        Icon(Icons.Default.Memory, contentDescription = "Manage Models")
                    }
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.ViewAllSessions) }) {
                        Icon(Icons.Default.History, contentDescription = "Session History")
                    }
                    IconButton(onClick = { state.eventSink(HomeScreen.Event.AppTour) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "App Overview & Tour")
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
                // Left Column: Hero Banner + Topics Grid
                Column(
                    modifier =
                        Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeroBanner(
                        hasDownloadedModel = state.hasDownloadedModel,
                        onManageModels = { state.eventSink(HomeScreen.Event.ManageModels) },
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Explore Topics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = "${state.topics.size} available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.topics) { topic ->
                            TopicCard(
                                topic = topic,
                                hasCourse = state.topicsWithCourses.contains(topic),
                                onClick = { state.eventSink(HomeScreen.Event.TopicSelected(topic)) },
                            )
                        }
                    }
                }

                // Right Column: Recent Sessions
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Recent Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.recentSessions.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { state.eventSink(HomeScreen.Event.ViewAllSessions) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("View all", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (state.recentSessions.isEmpty()) {
                        EmptySessionsCard(modifier = Modifier.fillMaxWidth())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.recentSessions) { session ->
                                SessionCard(session) {
                                    state.eventSink(HomeScreen.Event.SessionClicked(session.id))
                                }
                            }
                        }
                    }
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
                    HeroBanner(
                        hasDownloadedModel = state.hasDownloadedModel,
                        onManageModels = { state.eventSink(HomeScreen.Event.ManageModels) },
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Choose a Topic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = "${state.topics.size} available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(state.topics) { topic ->
                            TopicCompactCard(
                                topic = topic,
                                hasCourse = state.topicsWithCourses.contains(topic),
                                onClick = { state.eventSink(HomeScreen.Event.TopicSelected(topic)) },
                            )
                        }
                    }
                }

                if (state.recentSessions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Recent Sessions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FilledTonalButton(
                                onClick = { state.eventSink(HomeScreen.Event.ViewAllSessions) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("View all", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    items(state.recentSessions) { session ->
                        SessionCard(session) {
                            state.eventSink(HomeScreen.Event.SessionClicked(session.id))
                        }
                    }
                } else {
                    item {
                        EmptySessionsCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    hasDownloadedModel: Boolean,
    onManageModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "On-Device AI Tutor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Ask & Learn Locally",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color =
                        if (hasDownloadedModel) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (hasDownloadedModel) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint =
                                if (hasDownloadedModel) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Text(
                "Run optimized LLMs locally on your device with zero cloud latency and complete code privacy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledTonalButton(
                onClick = onManageModels,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (hasDownloadedModel) "Manage AI Models" else "Download AI Model",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: CodingTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasCourse: Boolean = false,
) {
    val visualInfo = topic.visualInfo
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = visualInfo.iconGlyph,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = visualInfo.accentColor,
                        )
                    }

                    if (hasCourse) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = visualInfo.accentColor.copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = visualInfo.accentColor,
                                )
                                Text(
                                    "Course",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = visualInfo.accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Icon(
                    Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = topic.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = visualInfo.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopicCompactCard(
    topic: CodingTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasCourse: Boolean = false,
) {
    val visualInfo = topic.visualInfo
    OutlinedCard(
        modifier =
            modifier
                .width(180.dp)
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = visualInfo.accentColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = visualInfo.iconGlyph,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = visualInfo.accentColor,
                    )
                }

                if (hasCourse) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = visualInfo.accentColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = visualInfo.accentColor,
                            )
                            Text(
                                "Course",
                                style = MaterialTheme.typography.labelSmall,
                                color = visualInfo.accentColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Text(
                text = topic.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = visualInfo.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: ChatSession,
    onClick: () -> Unit,
) {
    val visualInfo = session.topic.visualInfo
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.12f))
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Topic accent vertical strip
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(visualInfo.accentColor),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.topic.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualInfo.accentColor,
                        fontWeight = FontWeight.Medium,
                    )
                    val relativeTime =
                        dev.hossain.codematex.util
                            .formatRelativeTime(session.lastActiveAt)
                    val metadataText =
                        if (relativeTime.isNotEmpty()) {
                            "${session.messageCount} messages • $relativeTime"
                        } else {
                            "${session.messageCount} messages"
                        }
                    Text(
                        metadataText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptySessionsCard(modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Start Your First Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Select a topic above to ask questions and learn concepts with your private on-device AI tutor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE) }
    var hasPrompted by remember { mutableStateOf(prefs.getBoolean("has_prompted_notifications", false)) }
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    var showRationaleDialog by remember {
        mutableStateOf(!hasPrompted && !permissionState.status.isGranted)
    }

    if (showRationaleDialog && !permissionState.status.isGranted) {
        AlertDialog(
            onDismissRequest = {
                showRationaleDialog = false
                hasPrompted = true
                prefs.edit { putBoolean("has_prompted_notifications", true) }
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Stay Updated on Downloads",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text =
                        "CodeMateX downloads multi-gigabyte on-device AI models (2.6GB–3.7GB) to run locally on your phone.\n\n" +
                            "Enable notifications to track real-time download progress and get alerted when your offline AI tutor is ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        hasPrompted = true
                        prefs.edit { putBoolean("has_prompted_notifications", true) }
                        permissionState.launchPermissionRequest()
                    },
                ) {
                    Text("Enable Notifications")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        hasPrompted = true
                        prefs.edit { putBoolean("has_prompted_notifications", true) }
                    },
                ) {
                    Text("Not Now")
                }
            },
        )
    }
}

// ==========================================
// Previews
// ==========================================

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HomeLayout(
            state =
                HomeScreen.State.Success(
                    recentSessions =
                        listOf(
                            ChatSession(
                                id = "1",
                                title = "Kotlin Coroutines & Flow",
                                summary = "Explaining stateIn vs shareIn operators with practical examples.",
                                topic = CodingTopic.KOTLIN,
                                messageCount = 6,
                                lastActiveAt = 0L,
                                modelUsed = "Gemma 4-E2B IT",
                            ),
                            ChatSession(
                                id = "2",
                                title = "Jetpack Compose Performance",
                                summary = "Stability and smart recomposition optimization techniques.",
                                topic = CodingTopic.ANDROID,
                                messageCount = 4,
                                lastActiveAt = 0L,
                                modelUsed = "Gemma 4-E2B IT",
                            ),
                        ),
                    topics = CodingTopic.selectableEntries,
                    hasDownloadedModel = true,
                    eventSink = {},
                ),
        )
    }
}

@DevicePreviews
@Composable
private fun HomeScreenEmptySessionsPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HomeLayout(
            state =
                HomeScreen.State.Success(
                    recentSessions = emptyList(),
                    topics = CodingTopic.selectableEntries,
                    hasDownloadedModel = true,
                    eventSink = {},
                ),
        )
    }
}

@ThemePreviews
@Composable
private fun HeroBannerPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HeroBanner(
            hasDownloadedModel = true,
            onManageModels = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun TopicCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopicCard(
                topic = CodingTopic.KOTLIN,
                hasCourse = true,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            TopicCard(
                topic = CodingTopic.ANDROID,
                hasCourse = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TopicCompactCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopicCompactCard(
                topic = CodingTopic.RUST,
                hasCourse = true,
                onClick = {},
            )
            TopicCompactCard(
                topic = CodingTopic.SWIFT,
                hasCourse = false,
                onClick = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SessionCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        SessionCard(
            session =
                ChatSession(
                    id = "1",
                    title = "Kotlin Coroutines & Flow",
                    summary = "Explaining stateIn vs shareIn operators with practical examples.",
                    topic = CodingTopic.KOTLIN,
                    messageCount = 6,
                    lastActiveAt = 0L,
                    modelUsed = "Gemma 4-E2B IT",
                ),
            onClick = {},
        )
    }
}

@ThemePreviews
@Composable
private fun EmptySessionsCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        EmptySessionsCard(
            modifier = Modifier.padding(16.dp),
        )
    }
}
