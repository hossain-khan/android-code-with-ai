package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
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
    val visualInfo = CodingTopic.KOTLIN.visualInfo
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
                title = { Text("Kotlin Foundations", fontWeight = FontWeight.Bold) },
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
                                ChapterCard(chapter, state.progress) { lesson ->
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
                                ChapterCard(chapter, state.progress) { lesson ->
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(course.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${state.progress.completedLessons}/${state.progress.totalLessons} lessons complete",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearWavyProgressIndicator(
            progress = { state.progress.completionPercent / 100f },
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
            shape = androidx.compose.foundation.shape.CircleShape,
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
    progress: dev.hossain.codematex.data.model.CourseProgress,
    onLessonClick: (LearningLesson) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${chapter.order}. ${chapter.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(chapter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            chapter.lessons.forEach { lesson ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onLessonClick(lesson) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint =
                            if (lesson.id ==
                                progress.currentLessonId
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${chapter.order}.${lesson.order} ${lesson.title}", fontWeight = FontWeight.SemiBold)
                        Text("${lesson.estimatedMinutes} min • ${lesson.summary}", style = MaterialTheme.typography.bodySmall)
                    }
                }
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
                    dev.hossain.codematex.data.repository.KotlinCourseContent.course,
                    dev.hossain.codematex.data.model
                        .CourseProgress("kotlin-foundations", 1, 24, "kotlin-variables"),
                    {},
                ),
            )
        }
    }
}
