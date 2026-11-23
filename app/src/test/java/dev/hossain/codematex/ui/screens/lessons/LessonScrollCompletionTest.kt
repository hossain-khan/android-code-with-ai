package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LessonScrollCompletionTest {
    @Test
    fun `returns true when user manually scrolls to the end of an incomplete non-empty lesson`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = false,
                hasLessonBlocks = true,
                hasUserScrolled = true,
                canScrollBackward = true,
                canScrollForward = false,
            )

        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when lesson is already completed`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = true,
                hasLessonBlocks = true,
                hasUserScrolled = true,
                canScrollBackward = true,
                canScrollForward = false,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when user has not scrolled`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = false,
                hasLessonBlocks = true,
                hasUserScrolled = false,
                canScrollBackward = false,
                canScrollForward = false,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when more content remains below viewport`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = false,
                hasLessonBlocks = true,
                hasUserScrolled = true,
                canScrollBackward = true,
                canScrollForward = true,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when lesson has no content blocks`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = false,
                hasLessonBlocks = false,
                hasUserScrolled = true,
                canScrollBackward = true,
                canScrollForward = false,
            )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when list never scrolled past top`() {
        val result =
            shouldAutoCompleteLessonOnScrollEnd(
                isCompleted = false,
                hasLessonBlocks = true,
                hasUserScrolled = true,
                canScrollBackward = false,
                canScrollForward = false,
            )

        assertThat(result).isFalse()
    }
}
