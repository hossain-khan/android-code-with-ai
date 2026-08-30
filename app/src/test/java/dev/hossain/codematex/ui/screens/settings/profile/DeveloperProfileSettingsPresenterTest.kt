package dev.hossain.codematex.ui.screens.settings.profile

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DeveloperExperienceLevel
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.DeveloperProfilePreset
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DeveloperProfileSettingsPresenterTest {
    private lateinit var fakeNavigator: FakeNavigator
    private lateinit var fakeUserPreferencesStore: FakeUserPreferencesStore

    @Before
    fun setUp() {
        fakeNavigator = FakeNavigator(DeveloperProfileSettingsScreen)
        fakeUserPreferencesStore = FakeUserPreferencesStore()
    }

    private fun createPresenter(
        userPreferencesStore: FakeUserPreferencesStore = fakeUserPreferencesStore,
    ): DeveloperProfileSettingsPresenter =
        DeveloperProfileSettingsPresenter(
            navigator = fakeNavigator,
            screen = DeveloperProfileSettingsScreen,
            userPreferencesStore = userPreferencesStore,
        )

    @Test
    fun `given initial presentation then emits default developer profile state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(state.profile.enabled).isFalse()
                assertThat(state.profile.experienceLevel).isEqualTo(DeveloperExperienceLevel.INTERMEDIATE)
                assertThat(state.profile.primaryStack).isEmpty()
                assertThat(state.profile.customDirectives).isEmpty()
            }
        }

    @Test
    fun `given customized profile in store then emits customized state and prompt preview`() =
        runTest {
            val customProfile =
                DeveloperProfile(
                    enabled = true,
                    experienceLevel = DeveloperExperienceLevel.SENIOR,
                    primaryStack = "Kotlin, Jetpack Compose",
                    customDirectives = "Code-first answers with coroutines.",
                )
            val store = FakeUserPreferencesStore(initialDeveloperProfile = customProfile)
            val presenter = createPresenter(userPreferencesStore = store)

            presenter.test {
                val state = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(state.profile.enabled).isTrue()
                assertThat(state.profile.experienceLevel).isEqualTo(DeveloperExperienceLevel.SENIOR)
                assertThat(state.profile.primaryStack).isEqualTo("Kotlin, Jetpack Compose")
                assertThat(state.profile.customDirectives).isEqualTo("Code-first answers with coroutines.")
                assertThat(state.generatedPromptSnippet).contains("=== USER DEVELOPER PROFILE ===")
                assertThat(state.generatedPromptSnippet).contains("Kotlin, Jetpack Compose")
            }
        }

    @Test
    fun `given EnabledToggled event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(initialState.profile.enabled).isFalse()

                initialState.eventSink(DeveloperProfileSettingsScreen.Event.EnabledToggled(true))

                val updatedState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(updatedState.profile.enabled).isTrue()
                assertThat(fakeUserPreferencesStore.getDeveloperProfile().enabled).isTrue()
            }
        }

    @Test
    fun `given ExperienceLevelSelected event then updates store and auto-enables profile`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                initialState.eventSink(
                    DeveloperProfileSettingsScreen.Event.ExperienceLevelSelected(DeveloperExperienceLevel.STAFF_LEAD),
                )

                val updatedState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(updatedState.profile.experienceLevel).isEqualTo(DeveloperExperienceLevel.STAFF_LEAD)
                assertThat(updatedState.profile.enabled).isTrue()
                assertThat(fakeUserPreferencesStore.getDeveloperProfile().experienceLevel).isEqualTo(DeveloperExperienceLevel.STAFF_LEAD)
            }
        }

    @Test
    fun `given PrimaryStackChanged event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                initialState.eventSink(
                    DeveloperProfileSettingsScreen.Event.PrimaryStackChanged("Rust, Go, WebAssembly"),
                )

                val updatedState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(updatedState.profile.primaryStack).isEqualTo("Rust, Go, WebAssembly")
                assertThat(fakeUserPreferencesStore.getDeveloperProfile().primaryStack).isEqualTo("Rust, Go, WebAssembly")
            }
        }

    @Test
    fun `given CustomDirectivesChanged event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                initialState.eventSink(
                    DeveloperProfileSettingsScreen.Event.CustomDirectivesChanged("Focus on memory safety."),
                )

                val updatedState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(updatedState.profile.customDirectives).isEqualTo("Focus on memory safety.")
                assertThat(fakeUserPreferencesStore.getDeveloperProfile().customDirectives).isEqualTo("Focus on memory safety.")
            }
        }

    @Test
    fun `given PresetApplied event then updates all profile fields in store`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                initialState.eventSink(
                    DeveloperProfileSettingsScreen.Event.PresetApplied(DeveloperProfilePreset.ANDROID_KOTLIN),
                )

                val updatedState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(updatedState.profile.enabled).isTrue()
                assertThat(updatedState.profile.experienceLevel).isEqualTo(DeveloperExperienceLevel.SENIOR)
                assertThat(updatedState.profile.primaryStack).contains("Kotlin")
                assertThat(updatedState.profile.customDirectives).contains("Jetpack Compose")

                val storedProfile = fakeUserPreferencesStore.getDeveloperProfile()
                assertThat(storedProfile.enabled).isTrue()
                assertThat(storedProfile.experienceLevel).isEqualTo(DeveloperExperienceLevel.SENIOR)
            }
        }

    @Test
    fun `given ResetClicked event then resets store to default`() =
        runTest {
            val customProfile =
                DeveloperProfile(
                    enabled = true,
                    experienceLevel = DeveloperExperienceLevel.STAFF_LEAD,
                    primaryStack = "Python",
                    customDirectives = "Short answers.",
                )
            val store = FakeUserPreferencesStore(initialDeveloperProfile = customProfile)
            val presenter = createPresenter(userPreferencesStore = store)

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(initialState.profile.enabled).isTrue()

                initialState.eventSink(DeveloperProfileSettingsScreen.Event.ResetClicked)

                val resetState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                assertThat(resetState.profile.enabled).isFalse()
                assertThat(resetState.profile.experienceLevel).isEqualTo(DeveloperExperienceLevel.INTERMEDIATE)
                assertThat(resetState.profile.primaryStack).isEmpty()
                assertThat(resetState.profile.customDirectives).isEmpty()
            }
        }

    @Test
    fun `given BackClicked event then pops navigator`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val initialState = expectMostRecentItem() as DeveloperProfileSettingsScreen.State.Content
                initialState.eventSink(DeveloperProfileSettingsScreen.Event.BackClicked)

                fakeNavigator.awaitPop()
            }
        }
}
