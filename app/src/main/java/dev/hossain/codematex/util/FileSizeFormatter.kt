package dev.hossain.codematex.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a byte count into a human-readable storage size string in MB (e.g. `"2,588 MB"`).
 *
 * @param bytes The raw size in bytes.
 * @return Formatted string with thousands separators and unit (e.g. `"2,588 MB"`).
 */
fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val sizeMb = bytes / 1_000_000L
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(sizeMb)
    return "$formatted MB"
}

/**
 * Extension property to format a [Long] byte count as a storage size string in MB.
 */
val Long.formattedStorageSize: String
    get() = formatStorageSize(this)
