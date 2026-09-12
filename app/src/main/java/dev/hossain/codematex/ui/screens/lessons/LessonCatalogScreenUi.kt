package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.course.GoCourseContent
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.data.repository.course.RustCourseContent
import dev.hossain.codematex.ui.animation.CourseBadgeSharedKey
import dev.hossain.codematex.ui.animation.CourseCardSharedKey
import dev.hossain.codematex.ui.animation.CourseProgressSharedKey
import dev.hossain.codematex.ui.animation.CourseTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.animation.sharedElementNav
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
@CircuitInject(screen = LessonCatalogScreen::class, scope = AppScope::class)
@Composable
fun LessonCatalogScreenContent(
    state: LessonCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    if (SharedElementTransitionScope.isAvailable) {
        SharedElementTransitionScope {
            LessonCatalogInnerContent(state = state, modifier = modifier, transitionScope = this)
        }
    } else {
        LessonCatalogInnerContent(state = state, modifier = modifier, transitionScope = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LessonCatalogInnerContent(
    state: LessonCatalogScreen.State,
    modifier: Modifier = Modifier,
    transitionScope: SharedElementTransitionScope? = null,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val activeAccentColor =
        when (state) {
            is LessonCatalogScreen.State.Success -> {
                state.selectedTopic?.visualInfo?.accentColor ?: CodingTopic.KOTLIN.visualInfo.accentColor
            }

            else -> {
                CodingTopic.KOTLIN.visualInfo.accentColor
            }
        }
    val animatedAccentColor by animateColorAsState(
        targetValue = activeAccentColor,
        animationSpec = tween(300),
        label = "LessonCatalogAmbientGlow",
    )
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(animatedAccentColor.copy(alpha = 0.15f)),
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                TopAppBar(
                    title = { Text("Guided Lessons", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { state.eventSinkOrNull(LessonCatalogScreen.Event.Back) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    scrollBehavior = scrollBehavior,
                )
                if (state is LessonCatalogScreen.State.Success && state.availableTopics.isNotEmpty()) {
                    LanguageFilterChipRow(
                        allCount = state.allCourses.size,
                        availableTopics = state.availableTopics,
                        courseCountsByTopic = state.courseCountsByTopic,
                        selectedTopic = state.selectedTopic,
                        onTopicSelected = { topic ->
                            state.eventSink(LessonCatalogScreen.Event.SelectTopic(topic))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        when (state) {
            LessonCatalogScreen.State.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator()
                }
            }

            is LessonCatalogScreen.State.Error -> {
                ErrorContent(state.message, padding) {
                    state.eventSink(LessonCatalogScreen.Event.Retry)
                }
            }

            is LessonCatalogScreen.State.Success -> {
                AnimatedContent(
                    targetState = state.selectedTopic,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 200))
                            .togetherWith(fadeOut(animationSpec = tween(durationMillis = 150)))
                    },
                    label = "LessonCatalogFilterAnimation",
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) { targetTopic ->
                    val displayCourses =
                        if (targetTopic == null) {
                            state.allCourses
                        } else {
                            state.allCourses.filter { it.topic == targetTopic }
                        }
                    if (displayCourses.isEmpty()) {
                        EmptyLessonsState(
                            modifier = Modifier.fillMaxSize(),
                            isFiltered = targetTopic != null,
                            onClearFilter = { state.eventSink(LessonCatalogScreen.Event.SelectTopic(null)) },
                            onRetry = { state.eventSink(LessonCatalogScreen.Event.Retry) },
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(
                                items = displayCourses,
                                key = { it.id },
                            ) { course ->
                                CourseCard(
                                    course = course,
                                    progress = state.progress[course.id],
                                    transitionScope = transitionScope,
                                    modifier =
                                        Modifier.animateItem(
                                            fadeInSpec = tween(durationMillis = 200),
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                            fadeOutSpec = tween(durationMillis = 150),
                                        ),
                                ) {
                                    state.eventSink(LessonCatalogScreen.Event.OpenCourse(course.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: LearningCourse,
    progress: CourseProgress?,
    modifier: Modifier = Modifier,
    transitionScope: SharedElementTransitionScope? = null,
    onClick: () -> Unit,
) {
    val percent = progress?.completionPercent ?: 0
    val visualInfo = course.topic.visualInfo
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .sharedBoundsNav(transitionScope, CourseCardSharedKey(course.id))
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = visualInfo.accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.sharedElementNav(transitionScope, CourseBadgeSharedKey(course.id)),
                ) {
                    Text(
                        text = visualInfo.iconGlyph,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visualInfo.accentColor,
                    )
                }
                Text(
                    text = course.language,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = visualInfo.accentColor,
                )
            }
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.sharedBoundsNav(transitionScope, CourseTitleSharedKey(course.id)),
            )
            Text(
                course.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${course.chapters.size} chapters • ${course.lessonCount} lessons • $percent% complete",
                style = MaterialTheme.typography.labelMedium,
                color = visualInfo.accentColor,
            )
            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                color = visualInfo.accentColor,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .sharedBoundsNav(transitionScope, CourseProgressSharedKey(course.id)),
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(if (percent > 0) "Continue course" else "Start course")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LanguageFilterChipRow(
    allCount: Int,
    availableTopics: List<CodingTopic>,
    courseCountsByTopic: Map<CodingTopic, Int>,
    selectedTopic: CodingTopic?,
    onTopicSelected: (CodingTopic?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isAllSelected = selectedTopic == null
        FilterChip(
            selected = isAllSelected,
            onClick = { onTopicSelected(null) },
            label = { Text("All ($allCount)") },
        )

        availableTopics.forEach { topic ->
            val isSelected = selectedTopic == topic
            val count = courseCountsByTopic[topic] ?: 0
            val accentColor = topic.visualInfo.accentColor
            FilterChip(
                selected = isSelected,
                onClick = { onTopicSelected(if (isSelected) null else topic) },
                label = { Text("${topic.displayName} ($count)") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = accentColor,
                    ),
                border =
                    if (isSelected) {
                        BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    } else {
                        FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                    },
            )
        }
    }
}

@Composable
private fun EmptyLessonsState(
    modifier: Modifier = Modifier,
    isFiltered: Boolean = false,
    onClearFilter: () -> Unit = {},
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(16.dp),
            )
        }
        Text(
            text = if (isFiltered) "No lessons for this topic" else "No guided lessons yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text =
                if (isFiltered) {
                    "Try selecting \"All\" or another topic to view available courses."
                } else {
                    "Lesson courses will appear here when they are available."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (isFiltered) {
            Button(onClick = onClearFilter, modifier = Modifier.padding(top = 16.dp)) {
                Text("Clear filter")
            }
        } else {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("Refresh")
            }
        }
    }
}

private fun LessonCatalogScreen.State.eventSinkOrNull(event: LessonCatalogScreen.Event) {
    when (this) {
        is LessonCatalogScreen.State.Success -> eventSink(event)
        is LessonCatalogScreen.State.Error -> eventSink(event)
        else -> Unit
    }
}

@Composable
private fun ErrorContent(
    message: String,
    padding: PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun LessonCatalogPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            LessonCatalogScreenContent(
                LessonCatalogScreen.State.Success(
                    allCourses = listOf(KotlinCourseContent.course),
                    courses = listOf(KotlinCourseContent.course),
                    availableTopics = listOf(CodingTopic.KOTLIN),
                    courseCountsByTopic = mapOf(CodingTopic.KOTLIN to 1),
                    selectedTopic = null,
                    progress = emptyMap(),
                    eventSink = {},
                ),
            )
        }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun LessonCatalogMoreItemsPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            val courses =
                listOf(
                    KotlinCourseContent.course,
                    RustCourseContent.course,
                    PythonCourseContent.course,
                    GoCourseContent.course,
                )
            LessonCatalogScreenContent(
                LessonCatalogScreen.State.Success(
                    allCourses = courses,
                    courses = courses,
                    availableTopics =
                        listOf(
                            CodingTopic.KOTLIN,
                            CodingTopic.RUST,
                            CodingTopic.PYTHON,
                            CodingTopic.GO,
                        ),
                    courseCountsByTopic =
                        mapOf(
                            CodingTopic.KOTLIN to 1,
                            CodingTopic.RUST to 1,
                            CodingTopic.PYTHON to 1,
                            CodingTopic.GO to 1,
                        ),
                    selectedTopic = null,
                    progress =
                        mapOf(
                            KotlinCourseContent.course.id to CourseProgress(KotlinCourseContent.course.id, 5, 15, null),
                            RustCourseContent.course.id to CourseProgress(RustCourseContent.course.id, 8, 24, null),
                        ),
                    eventSink = {},
                ),
            )
        }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun LessonCatalogFilteredPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            val allCourses =
                listOf(
                    KotlinCourseContent.course,
                    RustCourseContent.course,
                    PythonCourseContent.course,
                    GoCourseContent.course,
                )
            LessonCatalogScreenContent(
                LessonCatalogScreen.State.Success(
                    allCourses = allCourses,
                    courses = listOf(RustCourseContent.course),
                    availableTopics =
                        listOf(
                            CodingTopic.KOTLIN,
                            CodingTopic.RUST,
                            CodingTopic.PYTHON,
                            CodingTopic.GO,
                        ),
                    courseCountsByTopic =
                        mapOf(
                            CodingTopic.KOTLIN to 1,
                            CodingTopic.RUST to 1,
                            CodingTopic.PYTHON to 1,
                            CodingTopic.GO to 1,
                        ),
                    selectedTopic = CodingTopic.RUST,
                    progress =
                        mapOf(
                            RustCourseContent.course.id to CourseProgress(RustCourseContent.course.id, 8, 24, null),
                        ),
                    eventSink = {},
                ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LanguageFilterChipRowPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            LanguageFilterChipRow(
                allCount = 4,
                availableTopics =
                    listOf(
                        CodingTopic.KOTLIN,
                        CodingTopic.RUST,
                        CodingTopic.PYTHON,
                        CodingTopic.GO,
                    ),
                courseCountsByTopic =
                    mapOf(
                        CodingTopic.KOTLIN to 1,
                        CodingTopic.RUST to 1,
                        CodingTopic.PYTHON to 1,
                        CodingTopic.GO to 1,
                    ),
                selectedTopic = CodingTopic.KOTLIN,
                onTopicSelected = {},
            )
        }
    }
}
