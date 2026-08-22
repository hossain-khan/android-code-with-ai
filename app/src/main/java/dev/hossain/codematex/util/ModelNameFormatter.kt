package dev.hossain.codematex.util

/**
 * Formats full model IDs or technical artifact names into concise, user-friendly display names.
 *
 * Examples:
 * - `"gemma-4-E2B-it-litert-lm"` -> `"Gemma-4-E2B"`
 * - `"gemma-4-E4B-it-litert-lm"` -> `"Gemma-4-E4B"`
 * - `"Dev Stub Model"` -> `"Dev Stub Model"`
 */
fun formatShortModelName(modelName: String): String {
    if (modelName.isBlank()) return ""
    return modelName
        .removeSuffix("-litert-lm")
        .removeSuffix("-it")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
