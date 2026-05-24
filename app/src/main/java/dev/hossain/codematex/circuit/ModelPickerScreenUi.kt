package dev.hossain.codematex.circuit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.zacsweers.metro.AppScope
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(screen = ModelPickerScreen::class, scope = AppScope::class)
@Composable
fun ModelPickerScreenContent(
    state: ModelPickerScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ModelPickerScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ModelPickerScreen.State.Success -> {
            ModelPickerLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerLayout(
    state: ModelPickerScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val sizeFormatter = remember { DecimalFormat("#,### MB") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Models") })
        },
    ) { innerPadding ->
        if (state.models.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No models available yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(state.models) { model ->
                    ModelCard(model, sizeFormatter) {
                        when (model.downloadStatus) {
                            DownloadStatus.NOT_DOWNLOADED, DownloadStatus.FAILED -> {
                                state.eventSink(ModelPickerScreen.Event.Download(model))
                            }

                            DownloadStatus.DOWNLOADED -> {
                                state.eventSink(ModelPickerScreen.Event.Select(model))
                            }

                            DownloadStatus.DOWNLOADING -> {
                                state.eventSink(ModelPickerScreen.Event.CancelDownload(model))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: AiModel,
    sizeFormatter: DecimalFormat,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(model.displayName, style = MaterialTheme.typography.titleMedium)
            Text(sizeFormatter.format(model.sizeBytes / 1_000_000), style = MaterialTheme.typography.bodySmall)
            Text(model.downloadStatus.name, style = MaterialTheme.typography.labelSmall)
            Button(
                onClick = onClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    when (model.downloadStatus) {
                        DownloadStatus.NOT_DOWNLOADED -> "Download"
                        DownloadStatus.DOWNLOADING -> "Cancel"
                        DownloadStatus.DOWNLOADED -> "Select"
                        DownloadStatus.FAILED -> "Retry"
                    },
                )
            }
        }
    }
}
