package dev.hossain.codematex.circuit.overlay

/**
 * Runtime sampler configuration for the on-device LLM.
 */
data class ModelConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
)
