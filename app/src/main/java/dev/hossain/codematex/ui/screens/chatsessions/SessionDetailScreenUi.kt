package dev.hossain.codematex.ui.screens.chatsessions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.codematex.util.formatShortModelName
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = SessionDetailScreen::class, scope = AppScope::class)
@Composable
fun SessionDetailScreenContent(
    state: SessionDetailScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SessionDetailScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is SessionDetailScreen.State.Success -> {
            SessionDetailLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SessionDetailLayout(
    state: SessionDetailScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val visualInfo = state.session.topic.visualInfo

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f)),
        topBar = {
            TopAppBar(
                title = { Text("Session Details", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SessionDetailScreen.Event.Back) }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(SessionDetailScreen.Event.DeleteSession) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isExpanded) {
            // Adaptive 2-Pane Multi-Column Layout for Tablets & Foldables
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Left Pane: Session Information & Action Controls
                Surface(
                    modifier =
                        Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .padding(20.dp)
                                .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SessionInfoCard(state.session)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { state.eventSink(SessionDetailScreen.Event.ResumeSession) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume Session")
                            }

                            OutlinedButton(
                                onClick = { state.eventSink(SessionDetailScreen.Event.DeleteSession) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Session")
                            }
                        }
                    }
                }

                // Right Pane: Messages Stream
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    Text(
                        text = "Conversation History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    if (state.messages.isEmpty()) {
                        EmptyDetailMessagesView(visualInfo = visualInfo, modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.messages) { message ->
                                SessionMessageBubble(
                                    message = message,
                                    visualAccent = visualInfo.accentColor,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Compact Single Column Layout
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
            ) {
                SessionInfoCard(state.session, modifier = Modifier.padding(16.dp))

                if (state.messages.isNotEmpty()) {
                    Text(
                        text = "Conversation History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.messages) { message ->
                            SessionMessageBubble(
                                message = message,
                                visualAccent = visualInfo.accentColor,
                            )
                        }
                    }
                } else {
                    EmptyDetailMessagesView(
                        visualInfo = visualInfo,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { state.eventSink(SessionDetailScreen.Event.ResumeSession) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume Session")
                    }

                    OutlinedButton(
                        onClick = { state.eventSink(SessionDetailScreen.Event.DeleteSession) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailMessagesView(
    visualInfo: TopicVisualInfo,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = visualInfo.accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = visualInfo.accentColor,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = "No Messages Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "This session does not contain any transcript messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SessionInfoCard(
    session: ChatSession,
    modifier: Modifier = Modifier,
) {
    val visualInfo = session.topic.visualInfo
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = visualInfo.accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
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
                    session.topic.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = visualInfo.accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                session.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                session.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Model: ${formatShortModelName(session.modelUsed)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "${session.messageCount} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun SessionMessageBubble(
    message: ChatMessage,
    visualAccent: Color,
) {
    val context = LocalContext.current

    when (message) {
        is ChatMessage.User -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 48.dp),
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }

        is ChatMessage.Agent -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = visualAccent,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "CodeMateX",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = visualAccent,
                                )
                            }

                            IconButton(
                                modifier = Modifier.size(24.dp),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Chat message", message.content))
                                },
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }

                        SessionMessageMarkdown(
                            content = message.content.ifEmpty { "..." },
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        is ChatMessage.Error -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        is ChatMessage.System -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = message.info,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionMessageMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val currentTheme = MaterialTheme.colorScheme
    val chatMarkdownStyle =
        remember(currentTheme) {
            RichTextStyle(
                paragraphSpacing = 6.sp,
                headingStyle = { level: Int, defaultStyle: TextStyle ->
                    when (level) {
                        0 -> defaultStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        1 -> defaultStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        2 -> defaultStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        else -> defaultStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                codeBlockStyle =
                    CodeBlockStyle(
                        textStyle =
                            TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp,
                            ),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentTheme.surfaceContainerLowest)
                                .padding(8.dp),
                    ),
            )
        }

    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)) {
        RichText(
            style = chatMarkdownStyle,
            modifier = modifier,
        ) {
            Markdown(content = content)
        }
    }
}

// ==========================================
// Previews
// ==========================================

private val sampleSession =
    ChatSession(
        id = "101",
        title = "Room Database Migrations in Android",
        summary = "How to write safe AutoMigration and manual Migration specs with Room.",
        topic = CodingTopic.ANDROID,
        messageCount = 4,
        lastActiveAt = 0L,
        modelUsed = "gemma-4-E2B-it-litert-lm",
    )

private val sampleDetailMessages =
    listOf(
        ChatMessage.User(
            content = "How do I add a new column to a Room entity with migration?",
        ),
        ChatMessage.Agent(
            content =
                "You can write an `AutoMigration` if adding a nullable or default-valued column:\n\n" +
                    "```kotlin\n" +
                    "@Database(\n" +
                    "    version = 2,\n" +
                    "    entities = [User::class],\n" +
                    "    autoMigrations = [\n" +
                    "        AutoMigration(from = 1, to = 2)\n" +
                    "    ]\n" +
                    ")\n" +
                    "abstract class AppDatabase : RoomDatabase()\n" +
                    "```",
        ),
        ChatMessage.System(
            info = "Session restored from local database",
        ),
    )

@DevicePreviews
@Composable
private fun SessionDetailScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        SessionDetailLayout(
            state =
                SessionDetailScreen.State.Success(
                    session = sampleSession,
                    messages = sampleDetailMessages,
                    eventSink = {},
                ),
        )
    }
}

@ThemePreviews
@Composable
private fun SessionInfoCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        SessionInfoCard(
            session = sampleSession,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun SessionMessageBubblePreview() {
    val accent = sampleSession.topic.visualInfo.accentColor
    CodeWithAIAppTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SessionMessageBubble(
                message = sampleDetailMessages[0],
                visualAccent = accent,
            )
            SessionMessageBubble(
                message = sampleDetailMessages[1],
                visualAccent = accent,
            )
            SessionMessageBubble(
                message = sampleDetailMessages[2],
                visualAccent = accent,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyDetailMessagesViewPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            EmptyDetailMessagesView(
                visualInfo = sampleSession.topic.visualInfo,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
