package dev.hossain.codematex.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecondaryClickTest {
    @Test
    fun `onSecondaryClick when enabled appends SecondaryClickElement`() {
        val base = Modifier
        val modified = base.onSecondaryClick(enabled = true) { }
        assertThat(modified).isNotEqualTo(base)

        var foundElement: SecondaryClickElement? = null
        modified.foldIn(Unit) { _, element ->
            if (element is SecondaryClickElement) {
                foundElement = element
            }
        }
        assertThat(foundElement).isNotNull()
        assertThat(foundElement?.enabled).isTrue()
    }

    @Test
    fun `onSecondaryClick when disabled retains enabled false state`() {
        val base = Modifier
        val modified = base.onSecondaryClick(enabled = false) { }

        var foundElement: SecondaryClickElement? = null
        modified.foldIn(Unit) { _, element ->
            if (element is SecondaryClickElement) {
                foundElement = element
            }
        }
        assertThat(foundElement).isNotNull()
        assertThat(foundElement?.enabled).isFalse()
    }

    @Test
    fun `SecondaryClickNode updates callback in place without recreating node`() {
        var clickedOffset1: Offset? = null
        var clickedOffset2: Offset? = null

        val node = SecondaryClickNode(enabled = true) { clickedOffset1 = it }
        assertThat(node.enabled).isTrue()

        // Verify initial callback
        node.onSecondaryClick(Offset(10f, 20f))
        assertThat(clickedOffset1).isEqualTo(Offset(10f, 20f))

        // Update node with new callback
        node.update(enabled = false) { clickedOffset2 = it }
        assertThat(node.enabled).isFalse()

        // Verify updated callback is called
        node.onSecondaryClick(Offset(30f, 40f))
        assertThat(clickedOffset2).isEqualTo(Offset(30f, 40f))
    }
}
