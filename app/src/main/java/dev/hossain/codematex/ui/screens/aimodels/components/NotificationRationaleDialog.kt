package dev.hossain.codematex.ui.screens.aimodels.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * In-context rationale dialog explaining why background download notifications are helpful
 * before requesting POST_NOTIFICATIONS permission on Android 13+.
 */
@Composable
fun NotificationRationaleDialog(
    modelSize: String,
    onEnableNotifications: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        title = {
            Text(
                text = "Stay Updated on Downloads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text =
                    "CodeMateX downloads multi-gigabyte on-device AI models " +
                        "($modelSize) to run locally on your phone.\n\n" +
                        "Enable notifications to track real-time download progress and get alerted when your offline AI tutor is ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onEnableNotifications,
            ) {
                Text("Enable Notifications")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Not Now")
            }
        },
    )
}

@ThemePreviews
@Composable
private fun NotificationRationaleDialogPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            NotificationRationaleDialog(
                modelSize = "2,588 MB",
                onEnableNotifications = {},
                onDismiss = {},
            )
        }
    }
}
