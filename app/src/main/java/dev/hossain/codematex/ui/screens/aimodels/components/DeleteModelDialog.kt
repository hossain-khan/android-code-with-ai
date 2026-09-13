package dev.hossain.codematex.ui.screens.aimodels.components

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
import dev.hossain.codematex.util.formatShortModelName

/**
 * Confirmation dialog shown before permanently deleting a downloaded AI model.
 */
@Composable
fun DeleteModelDialog(
    modelName: String,
    modelSize: String,
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
                text = "Delete ${formatShortModelName(modelName)}?",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text =
                    "Are you sure you want to delete this model? " +
                        "This will permanently remove the model file from your device and free up $modelSize of storage space.",
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
private fun DeleteModelDialogPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DeleteModelDialog(
                modelName = "Gemma 4-E2B IT",
                modelSize = "2.5 GB",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
