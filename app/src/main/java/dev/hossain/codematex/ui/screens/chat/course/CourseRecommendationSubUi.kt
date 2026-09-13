package dev.hossain.codematex.ui.screens.chat.course

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Composable rendering a contextual course suggestion card above topic starters in empty chat state.
 */
@Composable
fun CourseRecommendationSubUi(
    state: CourseRecommendationSubState,
    modifier: Modifier = Modifier,
) {
    val course = state.course ?: return
    val visualInfo = state.topic.visualInfo

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    state.eventSink(CourseRecommendationUiEvent.StartCourse(course.id))
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

/**
 * SubUi factory contributing [CourseRecommendationSubUi] into Metro DI [AppScope].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class CourseRecommendationSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is CourseRecommendationSubScreen -> {
                SubUi<CourseRecommendationSubState> { state, modifier ->
                    CourseRecommendationSubUi(state = state, modifier = modifier)
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
private fun CourseRecommendationSubUiPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            CourseRecommendationSubUi(
                state =
                    CourseRecommendationSubState(
                        topic = CodingTopic.KOTLIN,
                        course = KotlinCourseContent.course,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

// endregion
