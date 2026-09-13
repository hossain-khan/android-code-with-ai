package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.subcircuit.SubCircuitContent
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.ui.animation.LessonCardSharedKey
import dev.hossain.codematex.ui.animation.LessonTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.component.LocalCodeBlockSettings
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.screens.lessons.quiz.LessonQuizSubScreen
import dev.hossain.codematex.ui.screens.lessons.snippet.InteractiveSnippetSubScreen
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.zacsweers.metro.AppScope

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
@CircuitInject(screen = LessonScreen::class, scope = AppScope::class)
@Composable
fun LessonScreenContent(
    state: LessonScreen.State,
    modifier: Modifier = Modifier,
) {
    if (SharedElementTransitionScope.isAvailable) {
        SharedElementTransitionScope {
            LessonScreenInnerContent(state = state, modifier = modifier, transitionScope = this)
        }
    } else {
        LessonScreenInnerContent(state = state, modifier = modifier, transitionScope = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun LessonScreenInnerContent(
    state: LessonScreen.State,
    modifier: Modifier = Modifier,
    transitionScope: SharedElementTransitionScope? = null,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val visualInfo =
        if (state is LessonScreen.State.Success) {
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
                modifier =
                    if (state is LessonScreen.State.Success) {
                        Modifier.sharedBoundsNav(transitionScope, LessonCardSharedKey(state.lesson.id))
                    } else {
                        Modifier
                    },
                title = {
                    Text(
                        when (state) {
                            is LessonScreen.State.Success -> state.lesson.title
                            else -> "Lesson"
                        },
                        fontWeight = FontWeight.Bold,
                        modifier =
                            if (state is LessonScreen.State.Success) {
                                Modifier.sharedBoundsNav(transitionScope, LessonTitleSharedKey(state.lesson.id))
                            } else {
                                Modifier
                            },
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
                            course = state.course,
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
    lesson: LearningLesson,
    course: LearningCourse,
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
                course.title,
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
            val visualInfo = state.course.topic.visualInfo
            itemsIndexed(state.lesson.blocks.toList()) { index, block ->
                LessonBlockContent(
                    lessonId = state.lesson.id,
                    blockIndex = index,
                    block = block,
                    topic = state.course.topic,
                    visualInfo = visualInfo,
                )
            }
        }
        item {
            val visualInfo = state.course.topic.visualInfo
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                onClick = { state.eventSink(LessonScreen.Event.AskAi) },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.12f))
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI Tutor",
                            tint = visualInfo.accentColor,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ask AI about this lesson",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Chat with the on-device tutor for deep explanations, code walkthroughs, or custom exercises.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = visualInfo.accentColor,
                    )
                }
            }
        }
        item {
            val visualInfo = state.course.topic.visualInfo
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isCompleted) {
                    OutlinedButton(
                        onClick = { state.eventSink(LessonScreen.Event.MarkCompleted) },
                        modifier = if (state.nextLessonId == null) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Mark complete")
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
                        modifier = if (state.nextLessonId == null) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = visualInfo.accentColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = visualInfo.accentColor,
                            )
                        }
                    }
                }
                state.nextLessonId?.let {
                    Button(
                        onClick = { state.eventSink(LessonScreen.Event.NextLesson) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Next lesson")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHighlightApi::class)
@Composable
private fun LessonBlockContent(
    lessonId: String,
    blockIndex: Int,
    block: LessonBlock,
    topic: CodingTopic,
    visualInfo: TopicVisualInfo,
) {
    val settings = LocalCodeBlockSettings.current
    val baseStyle =
        if (settings.preset == CodeBlockPreset.COMPACT) {
            CodeBlockStyle.Compact
        } else {
            CodeBlockStyle.Default
        }
    val effectiveStyle =
        remember(baseStyle, settings.fontSize) {
            baseStyle.copy(
                textStyle =
                    baseStyle.textStyle.copy(
                        fontSize = settings.fontSize.sizeSp.sp,
                        lineHeight = (settings.fontSize.sizeSp * 1.35f).sp,
                    ),
            )
        }

    when (block) {
        is LessonBlock.Markdown -> {
            MarkdownMessage(block.content)
        }

        is LessonBlock.Code -> {
            val resolvedLanguage = block.language.ifEmpty { "text" }
            if (block.isPlaygroundRunnable) {
                SubCircuitContent(
                    screen =
                        InteractiveSnippetSubScreen(
                            snippetId = "${lessonId}_snippet_$blockIndex",
                            code = block.code,
                            language = resolvedLanguage,
                            topic = topic,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    outerEventSink = {},
                )
            } else {
                SyntaxHighlightedCode(
                    code = block.code,
                    language = resolvedLanguage,
                    showLineNumbers = settings.showLineNumbers,
                    style = effectiveStyle,
                    languageLabel =
                        if (settings.showLanguageLabel && resolvedLanguage.isNotBlank()) {
                            { SyntaxHighlightedCodeDefaults.LanguageLabel(resolvedLanguage) }
                        } else {
                            null
                        },
                    copyButton =
                        if (settings.showCopyButton) {
                            { onClick -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = onClick) }
                        } else {
                            null
                        },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }

        is LessonBlock.Quiz -> {
            SubCircuitContent(
                screen =
                    LessonQuizSubScreen(
                        quizId = "${lessonId}_quiz_$blockIndex",
                        question = block.question,
                        options = block.options,
                        answerIndex = block.answerIndex,
                        explanation = block.explanation,
                        topic = topic,
                    ),
                modifier = Modifier.fillMaxWidth(),
                outerEventSink = {},
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@DevicePreviews
@Composable
private fun LessonPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            Surface {
                LessonScreenContent(
                    LessonScreen.State.Success(
                        lesson =
                            KotlinCourseContent.course.chapters
                                .first()
                                .lessons
                                .first(),
                        course = KotlinCourseContent.course,
                        isCompleted = false,
                        nextLessonId = "kotlin-variables",
                        eventSink = {},
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@DevicePreviews
@Composable
private fun LessonCompletedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            Surface {
                LessonScreenContent(
                    LessonScreen.State.Success(
                        lesson =
                            KotlinCourseContent.course.chapters
                                .first()
                                .lessons
                                .first(),
                        course = KotlinCourseContent.course,
                        isCompleted = true,
                        nextLessonId = null,
                        eventSink = {},
                    ),
                )
            }
        }
    }
}
