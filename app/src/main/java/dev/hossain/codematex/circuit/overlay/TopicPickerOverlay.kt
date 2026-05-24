package dev.hossain.codematex.circuit.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuitx.overlays.BottomSheetOverlay
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.serialization.Serializable

@Serializable
data object TopicPickerOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("FunctionName")
fun TopicPickerOverlay(
    initialTopic: CodingTopic? = null,
    onTopicSelected: (CodingTopic) -> Unit = {},
): BottomSheetOverlay<Unit, Unit> =
    BottomSheetOverlay(
        model = Unit,
        onDismiss = {},
    ) { _, overlayNavigator ->
        TopicPickerContent(
            initialTopic = initialTopic,
            onTopicSelected = { topic ->
                onTopicSelected(topic)
                overlayNavigator.finish(Unit)
            },
        )
    }

@Composable
private fun TopicPickerContent(
    initialTopic: CodingTopic?,
    onTopicSelected: (CodingTopic) -> Unit,
) {
    var selectedTopic by remember { mutableStateOf(initialTopic) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Select a Topic", style = MaterialTheme.typography.titleLarge)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CodingTopic.entries.forEach { topic ->
                FilterChip(
                    selected = selectedTopic == topic,
                    onClick = {
                        selectedTopic = topic
                        onTopicSelected(topic)
                    },
                    label = { Text(topic.displayName) },
                )
            }
        }
    }
}
