package dev.hossain.codematex.ui.screens.lessons.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Composable rendering an interactive quiz block with multiple-choice options,
 * selection feedback, and instant explanation disclosure.
 */
@Composable
fun LessonQuizSubUi(
    state: LessonQuizSubState,
    modifier: Modifier = Modifier,
) {
    val visualInfo = state.topic.visualInfo

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
                text = state.question,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.options.forEachIndexed { index, option ->
                    val optionLetter = ('A' + index).toString()
                    val isChosen = state.selectedOptionIndex == index
                    val isCorrectAnswer = index == state.answerIndex
                    val isRevealed = state.selectedOptionIndex != null

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
                                .clickable { state.eventSink(LessonQuizUiEvent.SelectOption(index)) },
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

            state.selectedOptionIndex?.let { answer ->
                val isCorrect = answer == state.answerIndex
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
                                text = state.explanation,
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

/**
 * SubUi factory contributing [LessonQuizSubUi] into Metro DI [AppScope].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class LessonQuizSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is LessonQuizSubScreen -> {
                SubUi<LessonQuizSubState> { state, modifier ->
                    LessonQuizSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

// region Previews

@ThemePreviews
@Composable
private fun LessonQuizSubUiUnselectedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            LessonQuizSubUi(
                state =
                    LessonQuizSubState(
                        question = "What keyword is used to declare an immutable variable in Kotlin?",
                        options = listOf("var", "val", "let", "const"),
                        answerIndex = 1,
                        explanation = "In Kotlin, 'val' declares a read-only (immutable) variable.",
                        topic = CodingTopic.KOTLIN,
                        selectedOptionIndex = null,
                        eventSink = {},
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LessonQuizSubUiCorrectPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            LessonQuizSubUi(
                state =
                    LessonQuizSubState(
                        question = "What keyword is used to declare an immutable variable in Kotlin?",
                        options = listOf("var", "val", "let", "const"),
                        answerIndex = 1,
                        explanation = "In Kotlin, 'val' declares a read-only (immutable) variable.",
                        topic = CodingTopic.KOTLIN,
                        selectedOptionIndex = 1,
                        eventSink = {},
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LessonQuizSubUiIncorrectPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            LessonQuizSubUi(
                state =
                    LessonQuizSubState(
                        question = "What keyword is used to declare an immutable variable in Kotlin?",
                        options = listOf("var", "val", "let", "const"),
                        answerIndex = 1,
                        explanation = "In Kotlin, 'val' declares a read-only (immutable) variable.",
                        topic = CodingTopic.KOTLIN,
                        selectedOptionIndex = 0,
                        eventSink = {},
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

// endregion
