package dev.hossain.codematex.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScrollbarTest {
    // --- ScrollState & Scroll Value Tests ---

    @Test
    fun `calculateScrollbarMetrics with ScrollState returns null when unlaid-out`() {
        val state = ScrollState(initial = 0)
        // maxValue defaults to Int.MAX_VALUE before initial layout measurement
        assertThat(state.maxValue).isEqualTo(Int.MAX_VALUE)
        val metrics = calculateScrollbarMetrics(state, viewportLength = 1000f, minThumbLength = 24f)

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics returns null when content fits in viewport`() {
        // maxScrollValue <= 0 means entire content fits in viewport
        val metrics =
            calculateScrollbarMetrics(
                scrollValue = 0,
                maxScrollValue = 0,
                viewportLength = 1000f,
                minThumbLength = 24f,
            )

        assertThat(metrics).isNull()
    }

    @Test
    fun `calculateScrollbarMetrics returns valid metrics when scrollable`() {
        // scrollValue = 250 with maxScrollValue = 500 represents 50% scroll progress
        val metrics =
            calculateScrollbarMetrics(
                scrollValue = 250,
                maxScrollValue = 500,
                viewportLength = 500f,
                minThumbLength = 24f,
            )

        assertThat(metrics).isNotNull()
        val availableTrack = 500f - metrics!!.length
        assertThat(metrics.offset).isWithin(1f).of(0.5f * availableTrack)
    }

    @Test
    fun `calculateScrollbarMetrics safely clamps when minThumbLength exceeds viewport`() {
        // Viewport is 20f, but minThumbLength is 50f - ensures no coerceIn crash
        val metrics =
            calculateScrollbarMetrics(
                scrollValue = 100,
                maxScrollValue = 500,
                viewportLength = 20f,
                minThumbLength = 50f,
            )

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isEqualTo(20f)
        assertThat(metrics.offset).isEqualTo(0f)
    }

    @Test
    fun `calculateScrollbarMetrics returns null when viewport is zero or negative`() {
        val metrics =
            calculateScrollbarMetrics(
                scrollValue = 0,
                maxScrollValue = 100,
                viewportLength = 0f,
                minThumbLength = 24f,
            )

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
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 0,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 24f)

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
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 200,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 24f)

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
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.offset).isEqualTo(0f)
        assertThat(metrics.length).isGreaterThan(24f)
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
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        val expectedOffset = 500f - metrics!!.length
        assertThat(metrics.offset).isWithin(1f).of(expectedOffset)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo in reverseLayout positions thumb at bottom for index 0`() {
        // In reverse layout (chat feed), index 0 at start offset 0 is at bottom
        val info =
            ListScrollInfo(
                totalItemsCount = 20,
                visibleItemsCount = 5,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
                reverseLayout = true,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        // For reverse layout at index 0, effective progress is 1f (bottom)
        val expectedOffset = 500f - metrics!!.length
        assertThat(metrics.offset).isWithin(1f).of(expectedOffset)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo in reverseLayout moves thumb upward on positive scroll offset`() {
        // User scrolls 50px into item 0 (item size 100px) towards older messages
        val info =
            ListScrollInfo(
                totalItemsCount = 10,
                visibleItemsCount = 3,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 50,
                firstItemSize = 100,
                lastItemOffset = 250,
                lastItemSize = 100,
                reverseLayout = true,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 300f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        val availableTrack = 300f - metrics!!.length
        // scrollFraction = 50 / 100 = 0.5f. maxDecimalIndex = 10 - 3 = 7. progress = 0.5 / 7.
        // effectiveProgress = 1 - (0.5 / 7)
        val expectedOffset = (1f - (0.5f / 7f)) * availableTrack
        assertThat(metrics.offset).isWithin(1f).of(expectedOffset)
        // Offset should be less than bottom (availableTrack), proving thumb moved upward
        assertThat(metrics.offset).isLessThan(availableTrack)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo respects minThumbLength for very large lists`() {
        val info =
            ListScrollInfo(
                totalItemsCount = 1000,
                visibleItemsCount = 2,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 100,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 500f, minThumbLength = 40f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isAtLeast(40f)
    }

    @Test
    fun `calculateScrollbarMetrics with ListScrollInfo safely clamps when minThumbLength exceeds viewport`() {
        val info =
            ListScrollInfo(
                totalItemsCount = 20,
                visibleItemsCount = 5,
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 0,
                firstItemSize = 100,
                lastItemOffset = 400,
                lastItemSize = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 20f, minThumbLength = 50f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isEqualTo(20f)
        assertThat(metrics.offset).isEqualTo(0f)
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
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 0,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 24f)

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
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 100,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 1000f, minThumbLength = 24f)

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
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 200,
                lastItemHeight = 100,
            )
        val startMetrics = calculateScrollbarMetrics(startInfo, viewportLength = 300f, minThumbLength = 24f)

        assertThat(startMetrics).isNotNull()
        assertThat(startMetrics!!.offset).isEqualTo(0f)

        // At end: rows 7..9 visible
        val endInfo =
            GridScrollInfo(
                totalItemsCount = 20,
                visibleRowCount = 3,
                totalRows = 10,
                firstRow = 7,
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 200,
                lastItemHeight = 100,
            )
        val endMetrics = calculateScrollbarMetrics(endInfo, viewportLength = 300f, minThumbLength = 24f)

        assertThat(endMetrics).isNotNull()
        val expectedOffset = 300f - endMetrics!!.length
        assertThat(endMetrics.offset).isWithin(1f).of(expectedOffset)
    }

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo safely clamps when minThumbLength exceeds viewport`() {
        val info =
            GridScrollInfo(
                totalItemsCount = 20,
                visibleRowCount = 3,
                totalRows = 10,
                firstRow = 2,
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 200,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 20f, minThumbLength = 50f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isEqualTo(20f)
        assertThat(metrics.offset).isEqualTo(0f)
    }

    // --- ScrollbarDefaults Tests ---

    @Test
    fun `ScrollbarDefaults exposes expected standard dimensions and auto-hide timing`() {
        assertThat(ScrollbarDefaults.Width).isEqualTo(4.dp)
        assertThat(ScrollbarDefaults.Padding).isEqualTo(4.dp)
        assertThat(ScrollbarDefaults.MinThumbLength).isEqualTo(24.dp)
        assertThat(ScrollbarDefaults.AutoHideDelayMs).isEqualTo(1500L)
    }

    // --- estimateGridTotalRows Tests ---

    @Test
    fun `estimateGridTotalRows with single visible row and partial columns calculates correct total rows`() {
        val totalRows =
            estimateGridTotalRows(
                totalItems = 20,
                firstIndex = 0,
                lastIndex = 1,
                firstRow = 0,
                lastRow = 0,
                maxVisibleColumnIndex = 1,
            )
        // 2 columns observed, 20 items -> 10 rows
        assertThat(totalRows).isEqualTo(10)
    }

    @Test
    fun `estimateGridTotalRows with multiple visible rows and higher column index uses max observed columns`() {
        val totalRows =
            estimateGridTotalRows(
                totalItems = 20,
                firstIndex = 0,
                lastIndex = 7,
                firstRow = 0,
                lastRow = 1,
                maxVisibleColumnIndex = 3,
            )
        // 4 columns observed (0..3), 20 items -> 5 rows
        assertThat(totalRows).isEqualTo(5)
    }

    @Test
    fun `estimateGridTotalRows with last item visible returns exact lastRow plus one`() {
        val totalRows =
            estimateGridTotalRows(
                totalItems = 20,
                firstIndex = 16,
                lastIndex = 19,
                firstRow = 4,
                lastRow = 4,
                maxVisibleColumnIndex = 3,
            )
        assertThat(totalRows).isEqualTo(5)
    }

    @Test
    fun `estimateGridTotalRows with uneven row spans uses average items per row when higher than column index`() {
        val totalRows =
            estimateGridTotalRows(
                totalItems = 30,
                firstIndex = 0,
                lastIndex = 9,
                firstRow = 0,
                lastRow = 1,
                maxVisibleColumnIndex = 1,
            )
        // 10 items across 2 rows = 5 items/row, higher than observed column index (2) -> 30 / 5 = 6 rows
        assertThat(totalRows).isEqualTo(6)
    }

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo with single visible row calculates correct ratio and bounds`() {
        val info =
            GridScrollInfo(
                totalItemsCount = 20,
                visibleRowCount = 1,
                totalRows = 10,
                firstRow = 0,
                firstItemScrollOffset = 0,
                firstItemHeight = 100,
                lastItemOffsetY = 0,
                lastItemHeight = 100,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 300f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        // 1 / 10 rows visible = 10% thumb ratio -> 30px thumb length
        assertThat(metrics!!.length).isEqualTo(30f)
        assertThat(metrics.offset).isEqualTo(0f)
    }

    @Test
    fun `calculateScrollbarMetrics with GridScrollInfo with uneven rows calculates correct bounds`() {
        // First item is 150px tall, last item is 50px tall across 2 visible rows in 300px viewport
        val info =
            GridScrollInfo(
                totalItemsCount = 15,
                visibleRowCount = 2,
                totalRows = 8,
                firstRow = 3,
                firstItemScrollOffset = 50,
                firstItemHeight = 150,
                lastItemOffsetY = 150,
                lastItemHeight = 50,
            )
        val metrics = calculateScrollbarMetrics(info, viewportLength = 300f, minThumbLength = 24f)

        assertThat(metrics).isNotNull()
        assertThat(metrics!!.length).isAtLeast(24f)
        assertThat(metrics.offset).isAtLeast(0f)
        assertThat(metrics.offset + metrics.length).isAtMost(300f)
    }
}
