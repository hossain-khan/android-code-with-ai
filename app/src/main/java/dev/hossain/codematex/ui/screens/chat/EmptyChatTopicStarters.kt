package dev.hossain.codematex.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo

@Composable
internal fun EmptyChatTopicStarters(
    visualInfo: TopicVisualInfo,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    course: LearningCourse? = null,
    onStartCourse: ((String) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = visualInfo.accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = visualInfo.iconGlyph,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = visualInfo.accentColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Learn ${visualInfo.topic.displayName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = visualInfo.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            textAlign = TextAlign.Center,
        )

        if (course != null) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clickable(enabled = onStartCourse != null) {
                            onStartCourse?.invoke(course.id)
                        },
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = visualInfo.accentColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Guided Course",
                                style = MaterialTheme.typography.labelSmall,
                                color = visualInfo.accentColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "• ${course.chapters.size} ch, ${course.lessonCount} lessons",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Start Course",
                        tint = visualInfo.accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = visualInfo.accentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Suggested Questions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visualInfo.starterPrompts.forEach { prompt ->
                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onPromptSelected(prompt) },
                    shape = MaterialTheme.shapes.medium,
                    border =
                        BorderStroke(
                            1.dp,
                            if (enabled) {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            },
                        ),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                                if (enabled) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                                },
                        ),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                    )
                }
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

@ThemePreviews
@Composable
private fun EmptyChatTopicStartersPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            EmptyChatTopicStarters(
                visualInfo = CodingTopic.KOTLIN.visualInfo,
                onPromptSelected = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyChatTopicStartersWithCoursePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            EmptyChatTopicStarters(
                visualInfo = CodingTopic.RUST.visualInfo,
                course =
                    LearningCourse(
                        id = "rust-foundations",
                        language = "Rust",
                        title = "Rust Foundations",
                        description = "Master memory safety, ownership, borrowing, and concurrency.",
                        version = 1,
                        chapters =
                            listOf(
                                dev.hossain.codematex.data.model.LearningChapter(
                                    id = "rust-ch1",
                                    courseId = "rust-foundations",
                                    order = 1,
                                    title = "Getting Started",
                                    description = "Intro to Rust",
                                    lessons =
                                        listOf(
                                            dev.hossain.codematex.data.model.LearningLesson(
                                                id = "rust-l1",
                                                chapterId = "rust-ch1",
                                                order = 1,
                                                title = "Hello Rust",
                                                summary = "First steps",
                                                estimatedMinutes = 5,
                                                blocks = emptyList(),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                onStartCourse = {},
                onPromptSelected = {},
            )
        }
    }
}
