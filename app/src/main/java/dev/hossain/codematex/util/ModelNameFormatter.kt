package dev.hossain.codematex.util

/**
 * Formats full model IDs or technical artifact names into concise, user-friendly display names.
 *
 * Examples:
 * - `"gemma-4-E2B-it-litert-lm"` -> `"Gemma 4-E2B IT"`
 * - `"gemma-4-E4B-it-litert-lm"` -> `"Gemma 4-E4B IT"`
 * - `"gemma-2b-it-litert-lm"` -> `"Gemma 2B IT"`
 * - `"Dev Stub Model"` -> `"Dev Stub Model"`
 */
fun formatShortModelName(modelName: String): String {
    if (modelName.isBlank()) return ""
    val cleanName =
        modelName
            .substringAfterLast("/")
            .removeSuffix("-litert-lm")

    return when {
        cleanName.startsWith("gemma-", ignoreCase = true) -> {
            val parts = cleanName.removePrefix("gemma-").removePrefix("Gemma-")
            if (parts.endsWith("-it", ignoreCase = true)) {
                val base = parts.substring(0, parts.length - 3).uppercase()
                "Gemma $base IT"
            } else {
                "Gemma $parts"
            }
        }

        else -> {
            cleanName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
