package dev.hossain.codematex.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun `formatStorageSize formats zero and negative bytes as 0 MB`() {
        assertThat(formatStorageSize(0L)).isEqualTo("0 MB")
        assertThat(formatStorageSize(-500L)).isEqualTo("0 MB")
    }

    @Test
    fun `formatStorageSize formats megabytes with thousands separators`() {
        assertThat(formatStorageSize(2_588_147_712L)).isEqualTo("2,588 MB")
        assertThat(formatStorageSize(3_659_530_240L)).isEqualTo("3,659 MB")
        assertThat(formatStorageSize(1_850_000_000L)).isEqualTo("1,850 MB")
        assertThat(formatStorageSize(500_000_000L)).isEqualTo("500 MB")
    }

    @Test
    fun `formattedStorageSize extension property matches formatStorageSize function`() {
        val bytes = 2_588_147_712L
        assertThat(bytes.formattedStorageSize).isEqualTo(formatStorageSize(bytes))
    }
}
