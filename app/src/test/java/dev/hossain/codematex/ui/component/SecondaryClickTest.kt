package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecondaryClickTest {
    @Test
    fun `onSecondaryClick when enabled returns modifier with pointerInput`() {
        val base = Modifier
        val modified = base.onSecondaryClick(enabled = true) { }
        assertThat(modified).isNotEqualTo(base)
    }

    @Test
    fun `onSecondaryClick when disabled returns original modifier`() {
        val base = Modifier
        val modified = base.onSecondaryClick(enabled = false) { }
        assertThat(modified).isEqualTo(base)
    }
}
