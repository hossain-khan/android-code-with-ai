package dev.hossain.codematex.data.model

import dev.hossain.codematex.runtime.LlmEngine

object DevModels {
    val STUB_MODEL =
        AiModel(
            id = "dev/stub-model",
            name = "stub-model",
            displayName = "Dev Stub Model",
            downloadUrl = "",
            sizeBytes = 0,
            localPath = "/dev/null",
            downloadStatus = DownloadStatus.DOWNLOADED,
            preferredBackend = LlmEngine.Backend.CPU,
        )
}
