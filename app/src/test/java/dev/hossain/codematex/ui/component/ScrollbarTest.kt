package dev.hossain.codematex.ui.component

import androidx.compose.foundation.ScrollState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScrollbarTest {
    // --- ScrollState Tests ---

    @Test
    fun `calculateScrollbarMetrics with ScrollState returns null when content fits in viewport`() {
        val state = ScrollState(initial = 0)
        // maxValue defaults to 0 when not laid out or content fits
        val metrics = calculateScrollbarMetrics(state, viewportLength = 1000f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics with ScrollState returns null when viewport is zero or negative`() {
        val state = ScrollState(initial = 0)
        val metrics = calculateScrollbarMetrics(state, viewportLength = 0f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    // --- ListScrollInfo Tests ---

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo returns null when items are empty`() {
        val info =
            ListScrollInfo(
                totalItemsCount = 0,
                visibleItemsCount = 0,
                firstVisibleItemIndex = 0,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 0,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo returns null when all items fit in viewport`() {
        // 3 items of 100px each in 1000px viewport
        val info =
            ListScrollInfo(
                totalItemsCount = 3,
                visibleItemsCount = 3,
                firstVisibleItemIndex = 0,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 200,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo returns top offset when scrolled to start`() {
        // 5 visible items out of 20 total items
        val info =
            ListScrollInfo(
                totalItemsCount = 20,
                visibleItemsCount = 5,
                firstVisibleItemIndex = 0,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 32f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.offset).isEqualTo(0f)
        assertThat(metrics.length).isGreaterThan(32f)
        assertThat(metrics.length).isLessThan(500f)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo returns bottom offset when scrolled to end`() {
        // Last 5 items (indices 15..19) visible
        val info =
            ListScrollInfo(
                totalItemsCount = 20,
                visibleItemsCount = 5,
                firstVisibleItemIndex = 15,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 32f)

        assertThat(metrics).isNotNull()
        val expectedOffset = 500f - metrics!!.length
        assertThat(metrics.offset).isWithin(1f).of(expectedOffset)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo in reverseLayout positions thumb at bottom for index 0`() {
        // In reverse layout (chat feed), index 0 is at bottom
        val info =
            ListScrollInfo(
                totalItemsCount = 20,
                visibleItemsCount = 5,
                firstVisibleItemIndex = 0,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
                reverseLayout = true,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 32f)

        assertThat(metrics).isNotNull()
        // For reverse layout at index 0, effective progress is 1f (bottom)
        val expectedOffset = 500f - metrics!!.length
        assertThat(metrics.offset).isWithin(1f).of(expectedOffset)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo respects minThumbLength for very large lists`() {
        val info =
            ListScrollInfo(
                totalItemsCount = 1000,
                visibleItemsCount = 2,
                firstVisibleItemIndex = 0,
                firstVisibleItemOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 100,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 40f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isAtLeast(40f)
    }

    // --- GridScrollInfo Tests ---

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo returns null when empty`() {
        val info =
            GridScrollInfo(
                totalItemsCount = 0,
                visibleRowCount = 0,
                totalRows = 0,
                firstRow = 0,
                firstItemOffsetY = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 0,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo returns null when content fits in viewport`() {
        // 4 items (2 rows) of 100px height each in 1000px viewport
        val info =
            GridScrollInfo(
                totalItemsCount = 4,
                visibleRowCount = 2,
                totalRows = 2,
                firstRow = 0,
                firstItemOffsetY = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 100,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 32f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo returns correct metrics at start and end`() {
        // 20 items total, 2 per row = 10 rows. Viewport shows 3 rows
        val startInfo =
            GridScrollInfo(
                totalItemsCount = 20,
                visibleRowCount = 3,
                totalRows = 10,
                firstRow = 0,
                firstItemOffsetY = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 200,
                lastItemHeight = 100,
            )
        val startMetrics = calculateScrollbarMetrics(startInfo, viewportLength = 300f, minThumbLength = 32f)

        assertThat(startMetrics).isNotNull()
        assertThat(startMetrics!!.offset).isEqualTo(0f)

        // At end: rows 7..9 visible
        val endInfo =
            GridScrollInfo(
                totalItemsCount = 20,
                visibleRowCount = 3,
                totalRows = 10,
                firstRow = 7,
                firstItemOffsetY = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 200,
                lastItemHeight = 100,
            )
        val endMetrics = calculateScrollbarMetrics(endInfo, viewportLength = 300f, minThumbLength = 32f)

        assertThat(endMetrics).isNotNull()
        val expectedOffset = 300f - endMetrics!!.length
        assertThat(endMetrics.offset).isWithin(1f).of(expectedOffset)
    }
}
