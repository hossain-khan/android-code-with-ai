package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesStoreImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: UserPreferencesStore

    @Before
    fun setUp() {
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tempFolder.newFile("test_user_prefs.preferences_pb") },
            )
        store = UserPreferencesStoreImpl(dataStore)
    }

    @Test
    fun `given uninitialized store - getSelectedPersona returns default senior engineer`() =
        runTest(testDispatcher) {
            assertThat(store.getSelectedPersona()).isEqualTo(TutorPersona.SENIOR_ENGINEER)
            assertThat(store.selectedPersonaFlow.first()).isEqualTo(TutorPersona.SENIOR_ENGINEER)
        }

    @Test
    fun `given setSelectedPersona write - write is awaited and immediately observable`() =
        runTest(testDispatcher) {
            store.setSelectedPersona(TutorPersona.BEGINNER_FRIENDLY)

            assertThat(store.getSelectedPersona()).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
            assertThat(store.selectedPersonaFlow.first()).isEqualTo(TutorPersona.BEGINNER_FRIENDLY)
        }

    @Test
    fun `given unknown or corrupt stored persona name - defaults gracefully to senior engineer`() =
        runTest(testDispatcher) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("selected_tutor_persona")] = "INVALID_UNKNOWN_PERSONA"
            }

            assertThat(store.getSelectedPersona()).isEqualTo(TutorPersona.SENIOR_ENGINEER)
            assertThat(store.selectedPersonaFlow.first()).isEqualTo(TutorPersona.SENIOR_ENGINEER)
        }

    @Test
    fun `given multiple sequential persona updates - flow emits updates in order`() =
        runTest(testDispatcher) {
            val collected = mutableListOf<TutorPersona>()
            val job =
                launch {
                    store.selectedPersonaFlow.take(3).toList(collected)
                }

            store.setSelectedPersona(TutorPersona.BEGINNER_FRIENDLY)
            store.setSelectedPersona(TutorPersona.INTERVIEW_COACH)

            job.join()

            assertThat(collected)
                .containsExactly(
                    TutorPersona.SENIOR_ENGINEER,
                    TutorPersona.BEGINNER_FRIENDLY,
                    TutorPersona.INTERVIEW_COACH,
                ).inOrder()
        }

    @Test(expected = java.io.IOException::class)
    fun `given failing datastore write - setSelectedPersona propagates exception to caller`() =
        runTest(testDispatcher) {
            val failingDataStore =
                object : DataStore<Preferences> {
                    override val data: kotlinx.coroutines.flow.Flow<Preferences> =
                        kotlinx.coroutines.flow.flowOf(
                            androidx.datastore.preferences.core
                                .emptyPreferences(),
                        )

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        throw java.io.IOException("Disk write failure")
                }
            val failingStore = UserPreferencesStoreImpl(failingDataStore)
            failingStore.setSelectedPersona(TutorPersona.BEGINNER_FRIENDLY)
        }
}
