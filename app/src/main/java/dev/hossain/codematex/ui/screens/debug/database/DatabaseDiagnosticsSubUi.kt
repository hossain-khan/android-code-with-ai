package dev.hossain.codematex.ui.screens.debug.database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.ui.screens.debug.DebugScreen.DebugDatabaseStats
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun DatabaseDiagnosticsSubUi(
    state: DatabaseDiagnosticsSubState,
    modifier: Modifier = Modifier,
) {
    DatabaseDiagnosticsCard(
        stats = state.stats,
        onResetProgress = { state.eventSink(DatabaseDiagnosticsUiEvent.ResetProgress) },
        onSeedProgress = { state.eventSink(DatabaseDiagnosticsUiEvent.SeedProgress) },
        onClearSessions = { state.eventSink(DatabaseDiagnosticsUiEvent.ClearSessions) },
        modifier = modifier,
    )
}

@Composable
internal fun DatabaseDiagnosticsCard(
    stats: DebugDatabaseStats,
    onResetProgress: () -> Unit,
    onSeedProgress: () -> Unit,
    onClearSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetProgressDialog by rememberSaveable { mutableStateOf(false) }
    var showClearSessionsDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        text = "Course Progress & Database Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Curriculum progress and Room database metrics with QA test controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Sub-section: Curricula & Learning Progress
            Text(
                text = "Learning Progress Scorecard",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val lessonPercent =
                if (stats.totalBundledLessons > 0) {
                    (stats.completedLessons * 100) / stats.totalBundledLessons
                } else {
                    0
                }

            val courseMetrics =
                listOf(
                    "Completed Lessons" to "${stats.completedLessons} / ${stats.totalBundledLessons} ($lessonPercent%)",
                    "In-Progress Lessons" to "${stats.inProgressLessons}",
                    "Course Completion" to "${stats.completedCourses} / ${stats.totalCourses} courses",
                    "Bundled Quizzes" to "${stats.totalQuizzes} quizzes across curricula",
                )

            courseMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Sub-section: Room Conversation Storage
            Text(
                text = "Room Conversation Storage",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val sessionMetrics =
                listOf(
                    "Saved Chat Sessions" to "${stats.totalSessions} sessions",
                    "Persisted Message Rows" to "${stats.totalMessages} messages",
                )

            sessionMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Sub-section: QA Testing Actions
            Text(
                text = "QA Testing Actions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSeedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seed Sample Progress (First 3/course)")
                }

                OutlinedButton(
                    onClick = { showResetProgressDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Lesson Progress (0%)")
                }

                OutlinedButton(
                    onClick = { showClearSessionsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Chat Sessions & Messages")
                }
            }
        }
    }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Reset All Course Progress?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text =
                        "Are you sure you want to reset all lesson progress? " +
                            "This will set completion back to 0% across all curricula. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetProgressDialog = false
                        onResetProgress()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClearSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Clear All Chat Sessions?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text =
                        "Are you sure you want to delete all saved conversations? " +
                            "This will wipe all chat sessions and message rows from the Room database. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearSessionsDialog = false
                        onClearSessions()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Clear Sessions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class DatabaseDiagnosticsSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is DatabaseDiagnosticsSubScreen -> {
                SubUi<DatabaseDiagnosticsSubState> { state, modifier ->
                    DatabaseDiagnosticsSubUi(state, modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun DatabaseDiagnosticsCardPreview() {
    CodeWithAIAppTheme {
        Surface {
            DatabaseDiagnosticsSubUi(
                state =
                    DatabaseDiagnosticsSubState(
                        stats =
                            DebugDatabaseStats(
                                completedLessons = 14,
                                inProgressLessons = 2,
                                totalBundledLessons = 36,
                                totalCourses = 3,
                                completedCourses = 1,
                                totalQuizzes = 8,
                                totalSessions = 5,
                                totalMessages = 42,
                            ),
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
