package dev.hossain.codematex.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation that displays both Light and Dark theme variations in Android Studio preview.
 */
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/**
 * Multi-preview annotation for adaptive window sizes (Phone and Tablet / Expanded layout).
 */
@Preview(name = "Phone - Light", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Tablet - Light", widthDp = 1024, heightDp = 768, showBackground = true)
@Preview(name = "Phone - Dark", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet - Dark", widthDp = 1024, heightDp = 768, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DevicePreviews
