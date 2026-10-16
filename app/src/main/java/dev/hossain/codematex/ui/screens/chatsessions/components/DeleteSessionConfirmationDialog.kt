package dev.hossain.codematex.ui.screens.chatsessions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Confirmation dialog shown before permanently deleting a saved chat session.
 */
@Composable
fun DeleteSessionConfirmationDialog(
    sessionTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "Delete Chat Session?",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            val message =
                if (sessionTitle.isNotBlank()) {
                    "Are you sure you want to delete \"$sessionTitle\"? " +
                        "This action cannot be undone and will permanently remove all messages from your device."
                } else {
                    "Are you sure you want to delete this chat session? " +
                        "This action cannot be undone and will permanently remove all messages from your device."
                }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

@ThemePreviews
@Composable
private fun DeleteSessionConfirmationDialogPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DeleteSessionConfirmationDialog(
                sessionTitle = "Kotlin Coroutines & Flow Deep Dive",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
