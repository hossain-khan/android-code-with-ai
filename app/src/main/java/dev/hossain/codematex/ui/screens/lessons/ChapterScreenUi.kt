package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@CircuitInject(screen = ChapterScreen::class, scope = AppScope::class)
@Composable
fun ChapterScreenContent(
    state: ChapterScreen.State,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val visualInfo =
        if (state is ChapterScreen.State.Success) {
            state.course.topic.visualInfo
        } else {
            CodingTopic.KOTLIN.visualInfo
        }
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state is ChapterScreen.State.Success) state.course.title else "Guided Lessons",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (state) {
                            is ChapterScreen.State.Success -> state.eventSink(ChapterScreen.Event.Back)
                            is ChapterScreen.State.NotFound -> state.eventSink(ChapterScreen.Event.Back)
                            ChapterScreen.State.Loading -> Unit
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when (state) {
            ChapterScreen.State.Loading -> {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularWavyProgressIndicator() }
            }

            is ChapterScreen.State.NotFound -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message)
                    Button(onClick = { state.eventSink(ChapterScreen.Event.Back) }) { Text("Go back") }
                }
            }

            is ChapterScreen.State.Success -> {
                val course = state.course
                if (isExpanded) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item(span = {
                            androidx.compose.foundation.lazy.grid
                                .GridItemSpan(maxLineSpan)
                        }) {
                            CourseProgressHeader(course, state)
                        }
                        if (course.chapters.isEmpty()) {
                            item(span = {
                                androidx.compose.foundation.lazy.grid
                                    .GridItemSpan(maxLineSpan)
                            }) {
                                EmptyChapterState()
                            }
                        } else {
                            gridItems(course.chapters) { chapter ->
                                ChapterCard(chapter, state.progress, visualAccent = visualInfo.accentColor) { lesson ->
                                    state.eventSink(ChapterScreen.Event.OpenLesson(lesson.id))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            CourseProgressHeader(course, state)
                        }
                        if (course.chapters.isEmpty()) {
                            item {
                                EmptyChapterState()
                            }
                        } else {
                            lazyItems(course.chapters) { chapter ->
                                ChapterCard(chapter, state.progress, visualAccent = visualInfo.accentColor) { lesson ->
                                    state.eventSink(ChapterScreen.Event.OpenLesson(lesson.id))
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
private fun CourseProgressHeader(
    course: LearningCourse,
    state: ChapterScreen.State.Success,
) {
    val visualInfo = course.topic.visualInfo
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(course.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${state.progress.completedLessons}/${state.progress.totalLessons} lessons complete",
            style = MaterialTheme.typography.labelLarge,
            color = visualInfo.accentColor,
        )
        LinearProgressIndicator(
            progress = { (state.progress.completionPercent / 100f).coerceIn(0f, 1f) },
            color = visualInfo.accentColor,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { state.eventSink(ChapterScreen.Event.ResetProgress) }) {
            Icon(Icons.Default.RestartAlt, contentDescription = null)
            Text("Reset progress")
        }
    }
}

@Composable
private fun EmptyChapterState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(16.dp),
            )
        }
        Text("No chapters yet", fontWeight = FontWeight.Bold)
        Text(
            "This course is being prepared.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChapterCard(
    chapter: LearningChapter,
    progress: CourseProgress,
    visualAccent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onLessonClick: (LearningLesson) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, visualAccent.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualAccent.copy(alpha = 0.08f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = visualAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, visualAccent.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "Chapter ${chapter.order}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visualAccent,
                    )
                }
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = chapter.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            chapter.lessons.forEach { lesson ->
                val isCompleted = progress.completedLessonIds.contains(lesson.id)
                val isCurrent = lesson.id == progress.currentLessonId && !isCompleted
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onLessonClick(lesson) },
                    shape = MaterialTheme.shapes.medium,
                    color =
                        when {
                            isCurrent -> visualAccent.copy(alpha = 0.12f)
                            isCompleted -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                            else -> Color.Transparent
                        },
                    border =
                        when {
                            isCurrent -> BorderStroke(1.dp, visualAccent.copy(alpha = 0.4f))
                            isCompleted -> BorderStroke(1.dp, visualAccent.copy(alpha = 0.25f))
                            else -> null
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when {
                            isCompleted -> {
                                Surface(
                                    shape = CircleShape,
                                    color = visualAccent,
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            isCurrent -> {
                                Surface(
                                    shape = CircleShape,
                                    color = visualAccent.copy(alpha = 0.2f),
                                    border = BorderStroke(1.5.dp, visualAccent),
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Current",
                                            tint = visualAccent,
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }

                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.size(20.dp),
                                ) {}
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${chapter.order}.${lesson.order} ${lesson.title}",
                                    modifier = Modifier.weight(1f, fill = false),
                                    fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                                    color =
                                        when {
                                            isCurrent -> visualAccent
                                            isCompleted -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = visualAccent.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            text = "Current",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = visualAccent,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                } else if (isCompleted) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = visualAccent.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = "Done",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = visualAccent,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                            Text(
                                "${lesson.estimatedMinutes} min • ${lesson.summary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ChapterCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                ChapterCard(
                    chapter = KotlinCourseContent.course.chapters.first(),
                    progress =
                        CourseProgress(
                            courseId = "kotlin-foundations",
                            completedLessons = 1,
                            totalLessons = 24,
                            currentLessonId = "kotlin-variables",
                            completedLessonIds = setOf("kotlin-intro"),
                        ),
                    visualAccent = CodingTopic.KOTLIN.visualInfo.accentColor,
                    onLessonClick = {},
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ChapterCardPythonPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                ChapterCard(
                    chapter = PythonCourseContent.course.chapters.first(),
                    progress =
                        CourseProgress(
                            courseId = "python-foundations",
                            completedLessons = 3,
                            totalLessons = 24,
                            currentLessonId = "python-control-flow",
                            completedLessonIds = setOf("python-intro", "python-variables", "python-data-types"),
                        ),
                    visualAccent = CodingTopic.PYTHON.visualInfo.accentColor,
                    onLessonClick = {},
                )
            }
        }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun ChapterPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ChapterScreenContent(
                ChapterScreen.State.Success(
                    KotlinCourseContent.course,
                    CourseProgress(
                        courseId = "kotlin-foundations",
                        completedLessons = 2,
                        totalLessons = 24,
                        currentLessonId = "kotlin-functions",
                        completedLessonIds = setOf("kotlin-intro", "kotlin-variables"),
                    ),
                    {},
                ),
            )
        }
    }
}
