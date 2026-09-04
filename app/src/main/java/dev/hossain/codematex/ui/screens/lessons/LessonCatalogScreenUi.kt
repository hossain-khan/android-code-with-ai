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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.course.GoCourseContent
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.data.repository.course.RustCourseContent
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = LessonCatalogScreen::class, scope = AppScope::class)
@Composable
fun LessonCatalogScreenContent(
    state: LessonCatalogScreen.State,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val visualInfo = CodingTopic.KOTLIN.visualInfo
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f)),
        topBar = {
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
                if (state.courses.isEmpty()) {
                    EmptyLessonsState(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        onRetry = { state.eventSink(LessonCatalogScreen.Event.Retry) },
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.courses) { course ->
                            CourseCard(course, state.progress[course.id]) {
                                state.eventSink(LessonCatalogScreen.Event.OpenCourse(course.id))
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
    onClick: () -> Unit,
) {
    val percent = progress?.completionPercent ?: 0
    val visualInfo = course.topic.visualInfo
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(if (percent > 0) "Continue course" else "Start course")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EmptyLessonsState(
    modifier: Modifier = Modifier,
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
            text = "No guided lessons yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Lesson courses will appear here when they are available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Refresh")
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
                    courses = listOf(KotlinCourseContent.course),
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
            LessonCatalogScreenContent(
                LessonCatalogScreen.State.Success(
                    courses =
                        listOf(
                            KotlinCourseContent.course,
                            RustCourseContent.course,
                            PythonCourseContent.course,
                            GoCourseContent.course,
                        ),
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
