package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
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
                    blockIndex = index,
                    block = block,
                    visualInfo = visualInfo,
                    snippetState = state.snippetExecutionStates[index],
                    onRunSnippet = { blockIndex, code, language ->
                        state.eventSink(LessonScreen.Event.RunSnippet(blockIndex, code, language))
                    },
                    onDismissSnippet = { blockIndex ->
                        state.eventSink(LessonScreen.Event.DismissSnippetOutput(blockIndex))
                    },
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
    blockIndex: Int,
    block: LessonBlock,
    visualInfo: TopicVisualInfo,
    snippetState: SnippetExecutionState? = null,
    onRunSnippet: ((blockIndex: Int, code: String, language: String) -> Unit)? = null,
    onDismissSnippet: ((blockIndex: Int) -> Unit)? = null,
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
            val isPlaygroundSupported = block.isPlaygroundRunnable

            if (isPlaygroundSupported) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.fillMaxWidth(),
                        )

                        PlaygroundSnippetControls(
                            code = block.code,
                            language = resolvedLanguage,
                            executionState = snippetState ?: SnippetExecutionState.Idle,
                            visualInfo = visualInfo,
                            onRun = { onRunSnippet?.invoke(blockIndex, block.code, resolvedLanguage) },
                            onDismiss = { onDismissSnippet?.invoke(blockIndex) },
                        )
                    }
                }
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
            QuizContent(block, visualInfo)
        }
    }
}

@Composable
private fun QuizContent(
    block: LessonBlock.Quiz,
    visualInfo: TopicVisualInfo,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.08f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = visualInfo.accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "Quiz",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visualInfo.accentColor,
                    )
                }
                Text(
                    "Quick Check",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = block.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.options.forEachIndexed { index, option ->
                    val optionLetter = ('A' + index).toString()
                    val isChosen = selected == index
                    val isCorrectAnswer = index == block.answerIndex
                    val isRevealed = selected != null

                    val containerColor =
                        when {
                            !isRevealed -> MaterialTheme.colorScheme.surfaceContainerHigh
                            isCorrectAnswer -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            isChosen -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        }

                    val borderColor =
                        when {
                            !isRevealed -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            isCorrectAnswer -> MaterialTheme.colorScheme.primary
                            isChosen -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        }

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { selected = index },
                        shape = MaterialTheme.shapes.medium,
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color =
                                    when {
                                        !isRevealed -> visualInfo.accentColor.copy(alpha = 0.15f)
                                        isCorrectAnswer -> MaterialTheme.colorScheme.primary
                                        isChosen -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isRevealed && (isCorrectAnswer || isChosen)) {
                                        Icon(
                                            imageVector = if (isCorrectAnswer) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint =
                                                if (isCorrectAnswer) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onError
                                                },
                                            modifier = Modifier.size(14.dp),
                                        )
                                    } else {
                                        Text(
                                            text = optionLetter,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isRevealed) visualInfo.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            selected?.let { answer ->
                val isCorrect = answer == block.answerIndex
                val resultContainerColor =
                    if (isCorrect) {
                        visualInfo.accentColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    }
                val resultBorderColor =
                    if (isCorrect) {
                        visualInfo.accentColor.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = resultContainerColor,
                    border = BorderStroke(1.dp, resultBorderColor),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isCorrect) visualInfo.accentColor else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (isCorrect) "Correct!" else "Explanation",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) visualInfo.accentColor else MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = block.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaygroundSnippetControls(
    code: String,
    language: String,
    executionState: SnippetExecutionState,
    visualInfo: TopicVisualInfo,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playgroundTitle = getPlaygroundTitle(language)

    val isOutputVisible =
        executionState is SnippetExecutionState.Success ||
            executionState is SnippetExecutionState.CompilationError ||
            executionState is SnippetExecutionState.Error

    var lastOutputState by remember { mutableStateOf<SnippetExecutionState?>(null) }
    if (isOutputVisible) {
        lastOutputState = executionState
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = visualInfo.accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = visualInfo.accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = playgroundTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualInfo.accentColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (executionState is SnippetExecutionState.Compiling) {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(text = "Running...", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(
                    onClick = onRun,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = visualInfo.accentColor.copy(alpha = 0.15f),
                            contentColor = visualInfo.accentColor,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run code on playground",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (executionState !is SnippetExecutionState.Idle) "Re-run" else "Run Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = executionState is SnippetExecutionState.Compiling,
            enter = fadeIn(animationSpec = tween(150)) + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = visualInfo.accentColor,
                )
                Text(
                    text = "Compiling & executing on $playgroundTitle...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = isOutputVisible,
            enter =
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                ) +
                    expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    ) +
                    fadeIn(animationSpec = tween(durationMillis = 250)),
            exit =
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing),
                ) +
                    shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing),
                    ) +
                    fadeOut(animationSpec = tween(durationMillis = 200)),
        ) {
            val stateToRender = if (isOutputVisible) executionState else lastOutputState
            when (stateToRender) {
                is SnippetExecutionState.Success -> {
                    TerminalOutputCard(
                        title = "OUTPUT",
                        isError = false,
                        text = stateToRender.output,
                        visualInfo = visualInfo,
                        onDismiss = onDismiss,
                    )
                }

                is SnippetExecutionState.CompilationError -> {
                    TerminalOutputCard(
                        title = "COMPILER DIAGNOSTIC",
                        isError = true,
                        text = stateToRender.diagnostic,
                        visualInfo = visualInfo,
                        onDismiss = onDismiss,
                    )
                }

                is SnippetExecutionState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stateToRender.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                else -> {
                }
            }
        }
    }
}

@Composable
private fun TerminalOutputCard(
    title: String,
    isError: Boolean,
    text: String,
    visualInfo: TopicVisualInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isError) MaterialTheme.colorScheme.error else visualInfo.accentColor,
                                    shape = CircleShape,
                                ),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.error else visualInfo.accentColor,
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss output",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            SelectionContainer {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

private fun getPlaygroundTitle(language: String): String =
    when (language.trim().lowercase()) {
        "rust", "rs" -> "Rust Playground"
        "kotlin", "kt" -> "Kotlin Playground"
        "go", "golang" -> "Go Playground"
        "python", "py", "python3", "cpython" -> "Python Playground"
        else -> "${language.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Playground"
    }

@ThemePreviews
@Composable
private fun PlaygroundSnippetControlsIdlePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                PlaygroundSnippetControls(
                    code = "fun main() {\n    println(\"Hello, Kotlin!\")\n}",
                    language = "kotlin",
                    executionState = SnippetExecutionState.Idle,
                    visualInfo = CodingTopic.KOTLIN.visualInfo,
                    onRun = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundSnippetControlsSuccessPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                PlaygroundSnippetControls(
                    code = "fn main() { println!(\"Hello, world!\"); }",
                    language = "rust",
                    executionState = SnippetExecutionState.Success("Hello, world!\n"),
                    visualInfo = CodingTopic.RUST.visualInfo,
                    onRun = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundSnippetControlsErrorPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                PlaygroundSnippetControls(
                    code = "package main\n\nfunc main() {}",
                    language = "go",
                    executionState =
                        SnippetExecutionState.Error(
                            "Internet connection required to run code on the Go Playground.",
                        ),
                    visualInfo = CodingTopic.GO.visualInfo,
                    onRun = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun QuizContentPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                QuizContent(
                    block =
                        LessonBlock.Quiz(
                            question = "Which keyword creates an immutable read-only variable in Kotlin?",
                            options = listOf("val", "var", "const", "let"),
                            answerIndex = 0,
                            explanation = "'val' declares a read-only variable whose value cannot be reassigned once initialized.",
                        ),
                    visualInfo = CodingTopic.KOTLIN.visualInfo,
                )
            }
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
