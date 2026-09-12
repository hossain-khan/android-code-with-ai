package dev.hossain.codematex.ui.screens.home.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.ui.animation.CourseBadgeSharedKey
import dev.hossain.codematex.ui.animation.CourseCardSharedKey
import dev.hossain.codematex.ui.animation.CourseTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.animation.sharedElementNav
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Composable rendering the interactive guided courses section on the home dashboard.
 */
@Composable
fun GuidedCoursesSubUi(
    state: GuidedCoursesSubState,
    modifier: Modifier = Modifier,
) {
    if (state.availableCourses.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Guided Courses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = "${state.availableCourses.size} available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            FilledTonalButton(
                onClick = { state.eventSink(GuidedCoursesUiEvent.ViewAllCourses) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("View all", style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(state.availableCourses) { course ->
                CourseHomeCard(
                    course = course,
                    onClick = { state.eventSink(GuidedCoursesUiEvent.CourseClicked(course.id)) },
                )
            }
        }
    }
}

/**
 * Compact horizontal course card showing language badge, title, summary, and chapter metadata.
 */
@Composable
internal fun CourseHomeCard(
    course: LearningCourse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualInfo = course.topic.visualInfo
    Card(
        modifier =
            modifier
                .width(260.dp)
                .sharedBoundsNav(CourseCardSharedKey(course.id))
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = visualInfo.accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.sharedElementNav(CourseBadgeSharedKey(course.id)),
                    ) {
                        Text(
                            text = visualInfo.iconGlyph,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
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

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = visualInfo.accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsNav(CourseTitleSharedKey(course.id)),
            )

            Text(
                text = course.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${course.chapters.size} chapters • ${course.lessonCount} lessons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Factory creating [SubUi] instances for [GuidedCoursesSubScreen].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class GuidedCoursesSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is GuidedCoursesSubScreen -> {
                SubUi<GuidedCoursesSubState> { state, modifier ->
                    GuidedCoursesSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun GuidedCoursesSubUiPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            GuidedCoursesSubUi(
                state =
                    GuidedCoursesSubState(
                        availableCourses =
                            listOf(
                                LearningCourse(
                                    id = "kotlin-fundamentals",
                                    title = "Kotlin Fundamentals",
                                    language = "Kotlin",
                                    description = "Master Kotlin syntax, coroutines, and idioms.",
                                    version = 1,
                                    chapters = emptyList(),
                                ),
                                LearningCourse(
                                    id = "rust-primer",
                                    title = "Rust Primer",
                                    language = "Rust",
                                    description = "Learn memory safety, borrowing, and traits.",
                                    version = 1,
                                    chapters = emptyList(),
                                ),
                            ),
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
