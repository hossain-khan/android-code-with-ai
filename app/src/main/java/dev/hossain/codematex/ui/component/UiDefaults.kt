package dev.hossain.codematex.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Shared dialog window properties applying a subtle blur-behind effect on Android 12+ (API 31+).
 * Gracefully no-ops on older platform versions.
 */
internal val AppDialogProperties = DialogProperties(blurBehindRadius = 12.dp)

/**
 * Shared cache window for Markdown and code-heavy lazy lists to preload upcoming items
 * smoothly without holding excessive viewports in memory on lower-RAM devices.
 */
@OptIn(ExperimentalFoundationApi::class)
internal val ContentListCacheWindow =
    LazyLayoutCacheWindow(
        aheadFraction = 0.5f,
        behindFraction = 0.25f,
    )
