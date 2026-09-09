package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.CodingTopic
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

    @Test
    fun `given uninitialized store - dismissedCourseBannerTopicsFlow emits empty set`() =
        runTest(testDispatcher) {
            assertThat(store.dismissedCourseBannerTopicsFlow.first()).isEmpty()
        }

    @Test
    fun `given dismissCourseBanner - persists topic and updates flow`() =
        runTest(testDispatcher) {
            store.dismissCourseBanner(CodingTopic.KOTLIN)

            assertThat(store.dismissedCourseBannerTopicsFlow.first()).containsExactly("KOTLIN")

            store.dismissCourseBanner(CodingTopic.PYTHON)

            assertThat(store.dismissedCourseBannerTopicsFlow.first()).containsExactly("KOTLIN", "PYTHON")
        }

    @Test
    fun `given uninitialized store - isOnboardingCompleted returns default false`() =
        runTest(testDispatcher) {
            assertThat(store.isOnboardingCompleted()).isFalse()
            assertThat(store.isOnboardingCompletedFlow.first()).isFalse()
        }

    @Test
    fun `given setOnboardingCompleted write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setOnboardingCompleted(true)
            assertThat(store.isOnboardingCompleted()).isTrue()
            assertThat(store.isOnboardingCompletedFlow.first()).isTrue()
        }

    @Test
    fun `given uninitialized store - isWifiOnlyDownloadEnabled returns default true`() =
        runTest(testDispatcher) {
            assertThat(store.isWifiOnlyDownloadEnabled()).isTrue()
            assertThat(store.isWifiOnlyDownloadEnabledFlow.first()).isTrue()
        }

    @Test
    fun `given setWifiOnlyDownloadEnabled write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setWifiOnlyDownloadEnabled(false)
            assertThat(store.isWifiOnlyDownloadEnabled()).isFalse()
            assertThat(store.isWifiOnlyDownloadEnabledFlow.first()).isFalse()
        }

    @Test
    fun `given uninitialized store - isShowLineNumbersEnabled returns default false`() =
        runTest(testDispatcher) {
            assertThat(store.isShowLineNumbersEnabled()).isFalse()
            assertThat(store.showLineNumbersFlow.first()).isFalse()
        }

    @Test
    fun `given setShowLineNumbers write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setShowLineNumbers(true)
            assertThat(store.isShowLineNumbersEnabled()).isTrue()
            assertThat(store.showLineNumbersFlow.first()).isTrue()
        }

    @Test
    fun `given uninitialized store - isHapticFeedbackEnabled returns default true`() =
        runTest(testDispatcher) {
            assertThat(store.isHapticFeedbackEnabled()).isTrue()
            assertThat(store.hapticFeedbackEnabledFlow.first()).isTrue()
        }

    @Test
    fun `given setHapticFeedbackEnabled write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setHapticFeedbackEnabled(false)
            assertThat(store.isHapticFeedbackEnabled()).isFalse()
            assertThat(store.hapticFeedbackEnabledFlow.first()).isFalse()
        }

    @Test
    fun `given uninitialized store - getRamEvictionMinutes returns default 3`() =
        runTest(testDispatcher) {
            assertThat(store.getRamEvictionMinutes()).isEqualTo(3)
            assertThat(store.ramEvictionMinutesFlow.first()).isEqualTo(3)
        }

    @Test
    fun `given setRamEvictionMinutes write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setRamEvictionMinutes(10)
            assertThat(store.getRamEvictionMinutes()).isEqualTo(10)
            assertThat(store.ramEvictionMinutesFlow.first()).isEqualTo(10)
        }

    @Test
    fun `given uninitialized store - getCodeTheme returns default TOMORROW`() =
        runTest(testDispatcher) {
            assertThat(store.getCodeTheme()).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.TOMORROW)
            assertThat(store.codeThemeFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.TOMORROW)
        }

    @Test
    fun `given setCodeTheme write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setCodeTheme(dev.hossain.codematex.data.model.CodeTheme.DRACULA)
            assertThat(store.getCodeTheme()).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.DRACULA)
            assertThat(store.codeThemeFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.DRACULA)
        }

    @Test
    fun `given unknown or corrupt stored code theme - defaults gracefully to TOMORROW`() =
        runTest(testDispatcher) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("code_block_theme")] = "INVALID_THEME"
            }
            assertThat(store.getCodeTheme()).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.TOMORROW)
        }

    @Test
    fun `given uninitialized store - isShowLanguageLabelEnabled returns default true`() =
        runTest(testDispatcher) {
            assertThat(store.isShowLanguageLabelEnabled()).isTrue()
            assertThat(store.showLanguageLabelFlow.first()).isTrue()
        }

    @Test
    fun `given setShowLanguageLabel write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setShowLanguageLabel(false)
            assertThat(store.isShowLanguageLabelEnabled()).isFalse()
            assertThat(store.showLanguageLabelFlow.first()).isFalse()
        }

    @Test
    fun `given uninitialized store - isShowCopyButtonEnabled returns default true`() =
        runTest(testDispatcher) {
            assertThat(store.isShowCopyButtonEnabled()).isTrue()
            assertThat(store.showCopyButtonFlow.first()).isTrue()
        }

    @Test
    fun `given setShowCopyButton write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setShowCopyButton(false)
            assertThat(store.isShowCopyButtonEnabled()).isFalse()
            assertThat(store.showCopyButtonFlow.first()).isFalse()
        }

    @Test
    fun `given uninitialized store - getCodeBlockPreset returns default COMPACT`() =
        runTest(testDispatcher) {
            assertThat(store.getCodeBlockPreset()).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMPACT)
            assertThat(store.codeBlockPresetFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMPACT)
        }

    @Test
    fun `given setCodeBlockPreset write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setCodeBlockPreset(dev.hossain.codematex.data.model.CodeBlockPreset.COMFORTABLE)
            assertThat(store.getCodeBlockPreset()).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMFORTABLE)
            assertThat(store.codeBlockPresetFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMFORTABLE)
        }

    @Test
    fun `given unknown stored code block preset - defaults gracefully to COMPACT`() =
        runTest(testDispatcher) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("code_block_preset")] = "INVALID_PRESET"
            }
            assertThat(store.getCodeBlockPreset()).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMPACT)
        }

    @Test
    fun `given uninitialized store - getCodeFontSize returns default MEDIUM`() =
        runTest(testDispatcher) {
            assertThat(store.getCodeFontSize()).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.MEDIUM)
            assertThat(store.codeFontSizeFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.MEDIUM)
        }

    @Test
    fun `given setCodeFontSize write - state is immediately observable`() =
        runTest(testDispatcher) {
            store.setCodeFontSize(dev.hossain.codematex.data.model.CodeFontSize.LARGE)
            assertThat(store.getCodeFontSize()).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.LARGE)
            assertThat(store.codeFontSizeFlow.first()).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.LARGE)
        }

    @Test
    fun `given unknown stored code font size - defaults gracefully to MEDIUM`() =
        runTest(testDispatcher) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("code_font_size")] = "INVALID_SIZE"
            }
            assertThat(store.getCodeFontSize()).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.MEDIUM)
        }

    @Test
    fun `given settings updates - codeBlockSettingsFlow reflects composite snapshot`() =
        runTest(testDispatcher) {
            store.setCodeTheme(dev.hossain.codematex.data.model.CodeTheme.DRACULA)
            store.setShowLineNumbers(true)
            store.setShowLanguageLabel(false)
            store.setShowCopyButton(false)
            store.setCodeBlockPreset(dev.hossain.codematex.data.model.CodeBlockPreset.COMFORTABLE)
            store.setCodeFontSize(dev.hossain.codematex.data.model.CodeFontSize.LARGE)

            val snapshot = store.codeBlockSettingsFlow.first()
            assertThat(snapshot.theme).isEqualTo(dev.hossain.codematex.data.model.CodeTheme.DRACULA)
            assertThat(snapshot.showLineNumbers).isTrue()
            assertThat(snapshot.showLanguageLabel).isFalse()
            assertThat(snapshot.showCopyButton).isFalse()
            assertThat(snapshot.preset).isEqualTo(dev.hossain.codematex.data.model.CodeBlockPreset.COMFORTABLE)
            assertThat(snapshot.fontSize).isEqualTo(dev.hossain.codematex.data.model.CodeFontSize.LARGE)
        }

    @Test
    fun `given uninitialized store - getDeveloperProfile returns default intermediate`() =
        runTest(testDispatcher) {
            val profile = store.getDeveloperProfile()
            assertThat(profile.enabled).isFalse()
            assertThat(profile.experienceLevel).isEqualTo(dev.hossain.codematex.data.model.DeveloperExperienceLevel.INTERMEDIATE)
            assertThat(profile.primaryStack).isEmpty()
            assertThat(profile.customDirectives).isEmpty()
        }

    @Test
    fun `given setDeveloperProfile write - state is immediately observable`() =
        runTest(testDispatcher) {
            val customProfile =
                dev.hossain.codematex.data.model.DeveloperProfile(
                    enabled = true,
                    experienceLevel = dev.hossain.codematex.data.model.DeveloperExperienceLevel.STAFF_LEAD,
                    primaryStack = "Kotlin, Android",
                    customDirectives = "Always use coroutines",
                )
            store.setDeveloperProfile(customProfile)

            val updated = store.getDeveloperProfile()
            assertThat(updated.enabled).isTrue()
            assertThat(updated.experienceLevel).isEqualTo(dev.hossain.codematex.data.model.DeveloperExperienceLevel.STAFF_LEAD)
            assertThat(updated.primaryStack).isEqualTo("Kotlin, Android")
            assertThat(updated.customDirectives).isEqualTo("Always use coroutines")
        }

    @Test
    fun `given unknown stored experience level - defaults gracefully to INTERMEDIATE`() =
        runTest(testDispatcher) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("dev_profile_experience")] = "INVALID_LEVEL"
            }
            val profile = store.getDeveloperProfile()
            assertThat(profile.experienceLevel).isEqualTo(dev.hossain.codematex.data.model.DeveloperExperienceLevel.INTERMEDIATE)
        }
}
