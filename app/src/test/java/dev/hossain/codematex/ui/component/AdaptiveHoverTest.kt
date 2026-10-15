package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveHoverTest {
    @Test
    fun `handHoverIcon when enabled modifies modifier chain`() {
        val base = Modifier
        val modified = base.handHoverIcon(enabled = true)

        assertThat(modified).isNotEqualTo(base)
    }

    @Test
    fun `handHoverIcon when disabled returns same modifier`() {
        val base = Modifier
        val modified = base.handHoverIcon(enabled = false)

        assertThat(modified).isEqualTo(base)
    }
}
