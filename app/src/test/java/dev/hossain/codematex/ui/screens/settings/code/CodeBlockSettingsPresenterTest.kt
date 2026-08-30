package dev.hossain.codematex.ui.screens.settings.code

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodeFontSize
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CodeBlockSettingsPresenterTest {
    private lateinit var fakeNavigator: FakeNavigator
    private lateinit var fakeUserPreferencesStore: FakeUserPreferencesStore

    @Before
    fun setUp() {
        fakeNavigator = FakeNavigator(CodeBlockSettingsScreen)
        fakeUserPreferencesStore = FakeUserPreferencesStore()
    }

    private fun createPresenter(userPreferencesStore: FakeUserPreferencesStore = fakeUserPreferencesStore): CodeBlockSettingsPresenter =
        CodeBlockSettingsPresenter(
            navigator = fakeNavigator,
            screen = CodeBlockSettingsScreen,
            userPreferencesStore = userPreferencesStore,
        )

    @Test
    fun `given initial presentation then emits default code block settings state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(state.settings.theme).isEqualTo(CodeTheme.TOMORROW)
                assertThat(state.settings.showLineNumbers).isTrue()
                assertThat(state.settings.showLanguageLabel).isTrue()
                assertThat(state.settings.showCopyButton).isTrue()
                assertThat(state.settings.preset).isEqualTo(CodeBlockPreset.COMFORTABLE)
                assertThat(state.settings.fontSize).isEqualTo(CodeFontSize.MEDIUM)
                assertThat(state.previewCode).isNotEmpty()
            }
        }

    @Test
    fun `given ThemeSelected event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.ThemeSelected(CodeTheme.DRACULA))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.theme).isEqualTo(CodeTheme.DRACULA)
                assertThat(fakeUserPreferencesStore.getCodeTheme()).isEqualTo(CodeTheme.DRACULA)
            }
        }

    @Test
    fun `given LineNumbersToggled event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.LineNumbersToggled(false))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.showLineNumbers).isFalse()
                assertThat(fakeUserPreferencesStore.isShowLineNumbersEnabled()).isFalse()
            }
        }

    @Test
    fun `given LanguageLabelToggled event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.LanguageLabelToggled(false))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.showLanguageLabel).isFalse()
                assertThat(fakeUserPreferencesStore.isShowLanguageLabelEnabled()).isFalse()
            }
        }

    @Test
    fun `given CopyButtonToggled event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.CopyButtonToggled(false))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.showCopyButton).isFalse()
                assertThat(fakeUserPreferencesStore.isShowCopyButtonEnabled()).isFalse()
            }
        }

    @Test
    fun `given PresetSelected event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.PresetSelected(CodeBlockPreset.COMPACT))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.preset).isEqualTo(CodeBlockPreset.COMPACT)
                assertThat(fakeUserPreferencesStore.getCodeBlockPreset()).isEqualTo(CodeBlockPreset.COMPACT)
            }
        }

    @Test
    fun `given FontSizeSelected event then updates store and state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.FontSizeSelected(CodeFontSize.LARGE))

                val updatedState = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                assertThat(updatedState.settings.fontSize).isEqualTo(CodeFontSize.LARGE)
                assertThat(fakeUserPreferencesStore.getCodeFontSize()).isEqualTo(CodeFontSize.LARGE)
            }
        }

    @Test
    fun `given BackClicked event then pops navigator`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as CodeBlockSettingsScreen.State.Content
                state.eventSink(CodeBlockSettingsScreen.Event.BackClicked)

                fakeNavigator.awaitPop()
            }
        }
}
