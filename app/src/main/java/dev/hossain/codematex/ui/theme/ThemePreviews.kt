package dev.hossain.codematex.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation that displays both Light and Dark theme variations in Android Studio preview.
 */
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/**
 * Multi-preview annotation for adaptive window sizes across key device form factors:
 * Phone (Compact), Foldable (Medium), Tablet (Expanded), and Desktop.
 */
@Preview(name = "Phone - Light", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Foldable - Light", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet - Light", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop - Light", device = Devices.DESKTOP, showBackground = true)
@Preview(name = "Phone - Dark", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet - Dark", device = Devices.TABLET, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DevicePreviews
