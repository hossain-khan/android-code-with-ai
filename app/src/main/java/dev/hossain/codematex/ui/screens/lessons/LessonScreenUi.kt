package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@CircuitInject(screen = LessonScreen::class, scope = AppScope::class)
@Composable
fun LessonScreenContent(
    state: LessonScreen.State,
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
                title = {
                    Text(
                        when (state) {
                            is LessonScreen.State.Success -> state.lesson.title
                            else -> "Lesson"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (state) {
                            is LessonScreen.State.Success -> state.eventSink(LessonScreen.Event.Back)
                            is LessonScreen.State.NotFound -> state.eventSink(LessonScreen.Event.Back)
                            LessonScreen.State.Loading -> Unit
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
            LessonScreen.State.Loading -> {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularWavyProgressIndicator() }
            }

            is LessonScreen.State.NotFound -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message)
                    Button(onClick = { state.eventSink(LessonScreen.Event.Back) }) { Text("Go back") }
                }
            }

            is LessonScreen.State.Success -> {
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LessonOutline(
                            lesson = state.lesson,
                            modifier = Modifier.widthIn(max = 320.dp).padding(vertical = 16.dp),
                        )
                        LessonBody(
                            state = state,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    LessonBody(state = state, modifier = Modifier.fillMaxSize().padding(padding))
                }
            }
        }
    }
}

@Composable
private fun LessonOutline(
    lesson: dev.hossain.codematex.data.model.LearningLesson,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Lesson flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Kotlin Foundations",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Current lesson",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(lesson.title, fontWeight = FontWeight.SemiBold)
            Text(
                "Read the explanation, study the example, complete the check, and continue when ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LessonBody(
    state: LessonScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.widthIn(max = 900.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(state.lesson.summary, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${state.lesson.estimatedMinutes} minutes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (state.lesson.blocks.isEmpty()) {
            item {
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
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Text("Lesson content is coming soon", fontWeight = FontWeight.Bold)
                    Text(
                        "This lesson has not been authored yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.lesson.blocks.toList()) { block ->
                LessonBlockContent(block)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.isCompleted) {
                    Button(onClick = { state.eventSink(LessonScreen.Event.MarkCompleted) }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Mark complete")
                    }
                } else {
                    OutlinedButton(onClick = {}) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Completed")
                    }
                }
                state.nextLessonId?.let {
                    Button(onClick = { state.eventSink(LessonScreen.Event.NextLesson) }) {
                        Text("Next")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHighlightApi::class)
@Composable
private fun LessonBlockContent(block: LessonBlock) {
    when (block) {
        is LessonBlock.Markdown -> {
            MarkdownMessage(block.content)
        }

        is LessonBlock.Code -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                StreamingSyntaxHighlightedCode(
                    code = block.code,
                    language = block.language,
                    showLineNumbers = true,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }

        is LessonBlock.Quiz -> {
            QuizBlock(block)
        }
    }
}

@Composable
private fun QuizBlock(block: LessonBlock.Quiz) {
    var selected by remember { mutableStateOf<Int?>(null) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Check your understanding", fontWeight = FontWeight.Bold)
            Text(block.question)
            block.options.forEachIndexed { index, option ->
                OutlinedButton(
                    onClick = { selected = index },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(option, modifier = Modifier.weight(1f))
                }
            }
            selected?.let { answer ->
                Text(
                    if (answer == block.answerIndex) "Correct! ${block.explanation}" else "Not quite. ${block.explanation}",
                    color = if (answer == block.answerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun LessonPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        androidx.compose.material3.Surface {
            LessonScreenContent(
                LessonScreen.State.Success(
                    lesson =
                        dev.hossain.codematex.data.repository.KotlinCourseContent.course.chapters
                            .first()
                            .lessons
                            .first(),
                    isCompleted = false,
                    nextLessonId = "kotlin-variables",
                    eventSink = {},
                ),
            )
        }
    }
}
