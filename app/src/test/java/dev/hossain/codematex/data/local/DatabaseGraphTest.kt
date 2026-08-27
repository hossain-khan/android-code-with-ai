package dev.hossain.codematex.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DatabaseGraphTest {
    @Test
    fun `provideSessionDao returns dao from database`() {
        val fakeDao = FakeSessionDao()
        val fakeDatabase = FakeSessionDatabase(fakeDao)
        val graph =
            object : DatabaseGraph {
                // No overrides needed for this test; default method bodies are used.
            }

        val dao = graph.provideSessionDao(fakeDatabase)

        assertThat(dao).isSameInstanceAs(fakeDao)
    }
}
