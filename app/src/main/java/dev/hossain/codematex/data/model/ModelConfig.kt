package dev.hossain.codematex.data.model

import kotlinx.serialization.Serializable

/**
 * Immutable configuration parameters for Large Language Model text generation.
 *
 * @property temperature controls randomness in token selection (higher = more creative, lower = more deterministic).
 * @property topK limits sampling pool to the K most probable tokens.
 * @property topP nucleus sampling cumulative probability threshold.
 * @property maxTokens maximum number of tokens to decode in a single response.
 */
@Serializable
data class ModelConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 1.0f,
    val maxTokens: Int = 2048,
)
