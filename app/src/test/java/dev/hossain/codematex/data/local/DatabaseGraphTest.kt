package dev.hossain.codematex.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DatabaseGraphTest {
    @Test
    fun `provideSessionDao returns dao from database`() {
        val fakeDao = FakeSessionDao()
        val fakeDatabase = FakeSessionDatabase(fakeDao)

        val dao = DatabaseGraph.provideSessionDao(fakeDatabase)

        assertThat(dao).isSameInstanceAs(fakeDao)
    }
}
