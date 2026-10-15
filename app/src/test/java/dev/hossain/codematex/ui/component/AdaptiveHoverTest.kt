package dev.hossain.codematex.ui.component

import android.view.PointerIcon
import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdaptiveHoverTest {
    @Test
    fun `handHoverIcon when enabled appends PointerHoverIcon with hand icon`() {
        val base = Modifier
        val modified = base.handHoverIcon(enabled = true)

        assertThat(modified).isNotEqualTo(base)
        assertThat(modified.toString()).contains("PointerHoverIconModifierElement")
        // PointerIcon.TYPE_HAND in Android framework is 1002
        assertThat(modified.toString()).contains("type=${PointerIcon.TYPE_HAND}")
    }

    @Test
    fun `handHoverIcon when disabled returns unchanged modifier`() {
        val base = Modifier
        val modified = base.handHoverIcon(enabled = false)

        assertThat(modified).isEqualTo(base)
        assertThat(modified.toString()).doesNotContain("PointerHoverIconModifierElement")
    }
}
