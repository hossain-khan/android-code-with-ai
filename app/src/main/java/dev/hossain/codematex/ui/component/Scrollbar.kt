package dev.hossain.codematex.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Calculated metrics for drawing a scrollbar thumb.
 *
 * @property offset Top position in pixels along the scroll track.
 * @property length Length in pixels of the scrollbar thumb.
 */
internal data class ScrollbarMetrics(
    val offset: Float,
    val length: Float,
)

/**
 * Decoupled layout metrics for calculating list scrollbar positions.
 */
internal data class ListScrollInfo(
    val totalItemsCount: Int,
    val visibleItemsCount: Int,
    val firstVisibleItemIndex: Int,
    val firstVisibleItemOffset: Int,
    val firstItemSize: Int,
    val lastItemOffset: Int,
    val lastItemSize: Int,
    val reverseLayout: Boolean = false,
)

/**
 * Decoupled layout metrics for calculating grid scrollbar positions.
 */
internal data class GridScrollInfo(
    val totalItemsCount: Int,
    val visibleRowCount: Int,
    val totalRows: Int,
    val firstRow: Int,
    val firstItemOffsetY: Int,
    val firstItemHeight: Int,
    val lastItemOffsetY: Int,
    val lastItemHeight: Int,
    val reverseLayout: Boolean = false,
)

/**
 * Calculates thumb offset and length for a standard [ScrollState].
 */
internal fun calculateScrollbarMetrics(
    state: ScrollState,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? {
    if (state.maxValue <= 0 || state.maxValue == Int.MAX_VALUE || viewportLength <= 0f) return null
    val totalLength = viewportLength + state.maxValue.toFloat()
    val ratio = (viewportLength / totalLength).coerceIn(0.05f, 1f)
    val thumbLength = (viewportLength * ratio).coerceIn(minThumbLength, viewportLength)
    val availableTrack = viewportLength - thumbLength
    val progress = (state.value.toFloat() / state.maxValue.toFloat()).coerceIn(0f, 1f)
    val thumbOffset = progress * availableTrack
    return ScrollbarMetrics(offset = thumbOffset, length = thumbLength)
}

/**
 * Calculates thumb offset and length for a [ListScrollInfo].
 */
internal fun calculateScrollbarMetrics(
    info: ListScrollInfo,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? {
    if (info.visibleItemsCount <= 0 || info.totalItemsCount <= 0 || viewportLength <= 0f) return null

    // Check if entire content fits into viewport
    if (info.visibleItemsCount == info.totalItemsCount && info.firstVisibleItemOffset >= 0) {
        val totalMeasuredHeight = (info.lastItemOffset + info.lastItemSize) - info.firstVisibleItemOffset
        if (totalMeasuredHeight <= viewportLength) return null
    }

    val estimatedTotalHeight = (info.firstItemSize.toFloat() * info.totalItemsCount).coerceAtLeast(viewportLength)
    val ratio = (viewportLength / estimatedTotalHeight).coerceIn(0.05f, 1f)
    val thumbLength = (viewportLength * ratio).coerceIn(minThumbLength, viewportLength)

    val firstItemSize = info.firstItemSize.toFloat().coerceAtLeast(1f)
    val decimalIndex = info.firstVisibleItemIndex.toFloat() + (-info.firstVisibleItemOffset.toFloat() / firstItemSize).coerceAtLeast(0f)
    val maxDecimalIndex = (info.totalItemsCount - info.visibleItemsCount).coerceAtLeast(1).toFloat()
    val progress = (decimalIndex / maxDecimalIndex).coerceIn(0f, 1f)
    val effectiveProgress = if (info.reverseLayout) 1f - progress else progress

    val availableTrack = viewportLength - thumbLength
    val thumbOffset = effectiveProgress * availableTrack
    return ScrollbarMetrics(offset = thumbOffset, length = thumbLength)
}

/**
 * Calculates thumb offset and length for a [GridScrollInfo].
 */
internal fun calculateScrollbarMetrics(
    info: GridScrollInfo,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? {
    if (info.visibleRowCount <= 0 || info.totalItemsCount <= 0 || viewportLength <= 0f) return null

    if (info.visibleRowCount >= info.totalRows && info.firstItemOffsetY >= 0) {
        val totalMeasuredHeight = (info.lastItemOffsetY + info.lastItemHeight) - info.firstItemOffsetY
        if (totalMeasuredHeight <= viewportLength) return null
    }

    val ratio = (info.visibleRowCount.toFloat() / info.totalRows.toFloat()).coerceIn(0.05f, 1f)
    val thumbLength = (viewportLength * ratio).coerceIn(minThumbLength, viewportLength)

    val firstItemHeight = info.firstItemHeight.toFloat().coerceAtLeast(1f)
    val decimalRow = info.firstRow.toFloat() + (-info.firstItemOffsetY.toFloat() / firstItemHeight).coerceAtLeast(0f)
    val maxDecimalRow = (info.totalRows - info.visibleRowCount).coerceAtLeast(1).toFloat()
    val progress = (decimalRow / maxDecimalRow).coerceIn(0f, 1f)
    val effectiveProgress = if (info.reverseLayout) 1f - progress else progress

    val availableTrack = viewportLength - thumbLength
    val thumbOffset = effectiveProgress * availableTrack
    return ScrollbarMetrics(offset = thumbOffset, length = thumbLength)
}

/**
 * Calculates thumb offset and length for a [LazyListState].
 */
internal fun calculateScrollbarMetrics(
    state: LazyListState,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? = calculateScrollbarMetrics(state.layoutInfo, viewportLength, minThumbLength)

/**
 * Calculates thumb offset and length for a [LazyListLayoutInfo].
 */
internal fun calculateScrollbarMetrics(
    layoutInfo: LazyListLayoutInfo,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount
    if (visibleItems.isEmpty() || totalItems <= 0 || viewportLength <= 0f) return null

    val firstItem = visibleItems.first()
    val lastItem = visibleItems.last()
    val avgSize = (visibleItems.sumOf { it.size } / visibleItems.size).coerceAtLeast(1)

    return calculateScrollbarMetrics(
        info =
            ListScrollInfo(
                totalItemsCount = totalItems,
                visibleItemsCount = visibleItems.size,
                firstVisibleItemIndex = firstItem.index,
                firstVisibleItemOffset = firstItem.offset,
                firstItemSize = avgSize,
                lastItemOffset = lastItem.offset,
                lastItemSize = lastItem.size,
                reverseLayout = layoutInfo.reverseLayout,
            ),
        viewportLength = viewportLength,
        minThumbLength = minThumbLength,
    )
}

/**
 * Calculates thumb offset and length for a [LazyGridState].
 */
internal fun calculateScrollbarMetrics(
    state: LazyGridState,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? = calculateScrollbarMetrics(state.layoutInfo, viewportLength, minThumbLength)

/**
 * Calculates thumb offset and length for a [LazyGridLayoutInfo].
 */
internal fun calculateScrollbarMetrics(
    layoutInfo: LazyGridLayoutInfo,
    viewportLength: Float,
    minThumbLength: Float,
): ScrollbarMetrics? {
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount
    if (visibleItems.isEmpty() || totalItems <= 0 || viewportLength <= 0f) return null

    val firstItem = visibleItems.first()
    val lastItem = visibleItems.last()
    val firstRow = firstItem.row
    val lastRow = lastItem.row
    val visibleRowCount = (lastRow - firstRow + 1).coerceAtLeast(1)

    val itemsPerRow =
        visibleItems
            .groupBy { it.row }
            .values
            .maxOfOrNull { it.size }
            ?.coerceAtLeast(1) ?: 1
    val totalRows = ((totalItems + itemsPerRow - 1) / itemsPerRow).coerceAtLeast(1)

    return calculateScrollbarMetrics(
        info =
            GridScrollInfo(
                totalItemsCount = totalItems,
                visibleRowCount = visibleRowCount,
                totalRows = totalRows,
                firstRow = firstRow,
                firstItemOffsetY = firstItem.offset.y,
                firstItemHeight = firstItem.size.height.coerceAtLeast(1),
                lastItemOffsetY = lastItem.offset.y,
                lastItemHeight = lastItem.size.height,
                reverseLayout = layoutInfo.reverseLayout,
            ),
        viewportLength = viewportLength,
        minThumbLength = minThumbLength,
    )
}

/**
 * Remembers an animated alpha value that fades in quickly when scrolling starts and
 * fades out smoothly after scrolling ends.
 */
@Composable
private fun rememberScrollbarAlpha(
    isScrollInProgress: Boolean,
    alwaysShow: Boolean,
): State<Float> =
    animateFloatAsState(
        targetValue = if (isScrollInProgress || alwaysShow) 1f else 0f,
        animationSpec =
            if (isScrollInProgress || alwaysShow) {
                tween(durationMillis = 150)
            } else {
                tween(durationMillis = 500, delayMillis = 500)
            },
        label = "ScrollbarAlpha",
    )

/**
 * Draws an adaptive vertical scrollbar on a [LazyListState] container (e.g. `LazyColumn`).
 *
 * Automatically fades in when scrolling with a mouse, trackpad, or touch gesture,
 * and fades out after scrolling finishes. Does not consume layout space or cause content shifting.
 *
 * @param state The [LazyListState] to observe.
 * @param color The thumb color. Defaults to `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.
 * @param width Thickness of the scrollbar thumb.
 * @param padding Inset from the outer edge.
 * @param minThumbLength Minimum thumb height to ensure visibility.
 * @param alwaysShow Whether to keep the scrollbar permanently visible.
 */
fun Modifier.adaptiveScrollbar(
    state: LazyListState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: Dp = 4.dp,
    minThumbLength: Dp = 32.dp,
    alwaysShow: Boolean = false,
): Modifier =
    composed {
        val alpha by rememberScrollbarAlpha(
            isScrollInProgress = state.isScrollInProgress,
            alwaysShow = alwaysShow,
        )

        val resolvedColor =
            if (color != Color.Unspecified) {
                color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }

        drawWithContent {
            drawContent()

            if (alpha > 0f) {
                val metrics =
                    calculateScrollbarMetrics(
                        state = state,
                        viewportLength = size.height,
                        minThumbLength = minThumbLength.toPx(),
                    ) ?: return@drawWithContent

                val widthPx = width.toPx()
                val paddingPx = padding.toPx()
                val xPos =
                    if (layoutDirection == LayoutDirection.Ltr) {
                        size.width - widthPx - paddingPx
                    } else {
                        paddingPx
                    }

                drawRoundRect(
                    color = resolvedColor,
                    topLeft = Offset(x = xPos, y = metrics.offset),
                    size = Size(width = widthPx, height = metrics.length),
                    cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f),
                    alpha = alpha,
                )
            }
        }
    }

/**
 * Draws an adaptive vertical scrollbar on a [LazyGridState] container (e.g. `LazyVerticalGrid`).
 *
 * Automatically fades in when scrolling with a mouse, trackpad, or touch gesture,
 * and fades out after scrolling finishes. Does not consume layout space or cause content shifting.
 *
 * @param state The [LazyGridState] to observe.
 * @param color The thumb color. Defaults to `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.
 * @param width Thickness of the scrollbar thumb.
 * @param padding Inset from the outer edge.
 * @param minThumbLength Minimum thumb height to ensure visibility.
 * @param alwaysShow Whether to keep the scrollbar permanently visible.
 */
fun Modifier.adaptiveScrollbar(
    state: LazyGridState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: Dp = 4.dp,
    minThumbLength: Dp = 32.dp,
    alwaysShow: Boolean = false,
): Modifier =
    composed {
        val alpha by rememberScrollbarAlpha(
            isScrollInProgress = state.isScrollInProgress,
            alwaysShow = alwaysShow,
        )

        val resolvedColor =
            if (color != Color.Unspecified) {
                color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }

        drawWithContent {
            drawContent()

            if (alpha > 0f) {
                val metrics =
                    calculateScrollbarMetrics(
                        state = state,
                        viewportLength = size.height,
                        minThumbLength = minThumbLength.toPx(),
                    ) ?: return@drawWithContent

                val widthPx = width.toPx()
                val paddingPx = padding.toPx()
                val xPos =
                    if (layoutDirection == LayoutDirection.Ltr) {
                        size.width - widthPx - paddingPx
                    } else {
                        paddingPx
                    }

                drawRoundRect(
                    color = resolvedColor,
                    topLeft = Offset(x = xPos, y = metrics.offset),
                    size = Size(width = widthPx, height = metrics.length),
                    cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f),
                    alpha = alpha,
                )
            }
        }
    }

/**
 * Draws an adaptive vertical scrollbar on a [ScrollState] container (e.g. `Column(Modifier.verticalScroll(state))`).
 *
 * Automatically fades in when scrolling with a mouse, trackpad, or touch gesture,
 * and fades out after scrolling finishes. Does not consume layout space or cause content shifting.
 *
 * @param state The [ScrollState] to observe.
 * @param color The thumb color. Defaults to `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)`.
 * @param width Thickness of the scrollbar thumb.
 * @param padding Inset from the outer edge.
 * @param minThumbLength Minimum thumb height to ensure visibility.
 * @param alwaysShow Whether to keep the scrollbar permanently visible.
 */
fun Modifier.adaptiveScrollbar(
    state: ScrollState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: Dp = 4.dp,
    minThumbLength: Dp = 32.dp,
    alwaysShow: Boolean = false,
): Modifier =
    composed {
        val alpha by rememberScrollbarAlpha(
            isScrollInProgress = state.isScrollInProgress,
            alwaysShow = alwaysShow,
        )

        val resolvedColor =
            if (color != Color.Unspecified) {
                color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }

        drawWithContent {
            drawContent()

            if (alpha > 0f) {
                val metrics =
                    calculateScrollbarMetrics(
                        state = state,
                        viewportLength = size.height,
                        minThumbLength = minThumbLength.toPx(),
                    ) ?: return@drawWithContent

                val widthPx = width.toPx()
                val paddingPx = padding.toPx()
                val xPos =
                    if (layoutDirection == LayoutDirection.Ltr) {
                        size.width - widthPx - paddingPx
                    } else {
                        paddingPx
                    }

                drawRoundRect(
                    color = resolvedColor,
                    topLeft = Offset(x = xPos, y = metrics.offset),
                    size = Size(width = widthPx, height = metrics.length),
                    cornerRadius = CornerRadius(widthPx / 2f, widthPx / 2f),
                    alpha = alpha,
                )
            }
        }
    }

@ThemePreviews
@Composable
private fun AdaptiveScrollbarPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
            ) {
                val listState = rememberLazyListState()
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .adaptiveScrollbar(state = listState, alwaysShow = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(10) { index ->
                        Text(
                            text = "Scrollable preview item #$index",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.small,
                                    ).padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
