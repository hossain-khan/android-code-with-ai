package dev.hossain.codematex.ui.screens.home.topics

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.animation.TopicCardSharedKey
import dev.hossain.codematex.ui.animation.TopicGlyphSharedKey
import dev.hossain.codematex.ui.animation.TopicTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.animation.sharedElementNav
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun TopicsGridSubUi(
    state: TopicsGridSubState,
    modifier: Modifier = Modifier,
) {
    if (state.isCompact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopicsHeader(topicCount = state.topics.size)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(state.topics) { topic ->
                    TopicCompactCard(
                        topic = topic,
                        hasCourse = state.topicsWithCourses.contains(topic),
                        onClick = { state.eventSink(TopicsGridUiEvent.TopicSelected(topic)) },
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TopicsHeader(topicCount = state.topics.size)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.topics) { topic ->
                    TopicCard(
                        topic = topic,
                        hasCourse = state.topicsWithCourses.contains(topic),
                        onClick = { state.eventSink(TopicsGridUiEvent.TopicSelected(topic)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicsHeader(
    topicCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Chat with AI Tutor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Text(
                text = "$topicCount topics",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun TopicCard(
    topic: CodingTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasCourse: Boolean = false,
) {
    val visualInfo = topic.visualInfo
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .sharedBoundsNav(TopicCardSharedKey(topic.stableId))
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.3f)),
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
                        shape = MaterialTheme.shapes.small,
                        color = visualInfo.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.sharedElementNav(TopicGlyphSharedKey(topic.stableId)),
                    ) {
                        Text(
                            text = visualInfo.iconGlyph,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = visualInfo.accentColor,
                        )
                    }

                    if (hasCourse) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = visualInfo.accentColor.copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = visualInfo.accentColor,
                                )
                                Text(
                                    "Course",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = visualInfo.accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = topic.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsNav(TopicTitleSharedKey(topic.stableId)),
            )

            Text(
                text = visualInfo.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TopicCompactCard(
    topic: CodingTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasCourse: Boolean = false,
) {
    val visualInfo = topic.visualInfo
    OutlinedCard(
        modifier =
            modifier
                .width(180.dp)
                .sharedBoundsNav(TopicCardSharedKey(topic.stableId))
                .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f))
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = visualInfo.accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.sharedElementNav(TopicGlyphSharedKey(topic.stableId)),
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

                if (hasCourse) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = visualInfo.accentColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = visualInfo.accentColor,
                            )
                            Text(
                                "Course",
                                style = MaterialTheme.typography.labelSmall,
                                color = visualInfo.accentColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Text(
                text = topic.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sharedBoundsNav(TopicTitleSharedKey(topic.stableId)),
            )

            Text(
                text = visualInfo.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class TopicsGridSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is TopicsGridSubScreen -> {
                SubUi<TopicsGridSubState> { state, modifier ->
                    TopicsGridSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun TopicsGridSubUiCompactPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            TopicsGridSubUi(
                state =
                    TopicsGridSubState(
                        topics = listOf(CodingTopic.KOTLIN, CodingTopic.PYTHON, CodingTopic.RUST),
                        topicsWithCourses = setOf(CodingTopic.KOTLIN),
                        isCompact = true,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TopicsGridSubUiExpandedPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            TopicsGridSubUi(
                state =
                    TopicsGridSubState(
                        topics = listOf(CodingTopic.KOTLIN, CodingTopic.PYTHON, CodingTopic.RUST, CodingTopic.GO),
                        topicsWithCourses = setOf(CodingTopic.KOTLIN, CodingTopic.RUST),
                        isCompact = false,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
