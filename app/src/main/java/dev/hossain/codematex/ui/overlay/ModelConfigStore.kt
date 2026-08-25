package dev.hossain.codematex.ui.overlay

import android.content.Context

class ModelConfigStore(
    private val context: Context,
) {
    @Volatile
    private var _config: ModelConfig = ModelConfig()

    val config: ModelConfig
        get() = _config

    fun updateConfig(newConfig: ModelConfig) {
        _config = newConfig
    }
}
