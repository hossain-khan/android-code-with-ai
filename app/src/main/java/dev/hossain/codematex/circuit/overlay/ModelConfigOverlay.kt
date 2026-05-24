package dev.hossain.codematex.circuit.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.serialization.Serializable

data class ModelConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
)

@Serializable
data object ModelConfigOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
fun ModelConfigOverlay(
    initialConfig: ModelConfig = ModelConfig(),
    onConfigSaved: (ModelConfig) -> Unit = {},
): BottomSheetOverlay<Unit, Unit> =
    BottomSheetOverlay(
        model = Unit,
        onDismiss = {},
    ) { _, overlayNavigator ->
        ModelConfigContent(
            initialConfig = initialConfig,
            onDismiss = {
                overlayNavigator.finish(Unit)
            },
            onSave = { config ->
                onConfigSaved(config)
                overlayNavigator.finish(Unit)
            },
        )
    }

@Composable
private fun ModelConfigContent(
    initialConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit,
) {
    var temperature by remember { mutableFloatStateOf(initialConfig.temperature) }
    var topK by remember { mutableFloatStateOf(initialConfig.topK.toFloat()) }
    var topP by remember { mutableFloatStateOf(initialConfig.topP) }
    var maxTokens by remember { mutableFloatStateOf(initialConfig.maxTokens.toFloat()) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Model Configuration", style = MaterialTheme.typography.titleLarge)

        ConfigSlider("Temperature", temperature, 0f, 2f) { temperature = it }
        ConfigSlider("Top-K", topK, 1f, 100f) { topK = it }
        ConfigSlider("Top-P", topP, 0f, 1f) { topP = it }
        ConfigSlider("Max Tokens", maxTokens, 128f, 8192f) { maxTokens = it }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSave(
                        ModelConfig(
                            temperature = temperature,
                            topK = topK.toInt(),
                            topP = topP,
                            maxTokens = maxTokens.toInt(),
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label)
            Text(if (value == value.toInt().toFloat()) value.toInt().toString() else "%.2f".format(value))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
        )
    }
}
