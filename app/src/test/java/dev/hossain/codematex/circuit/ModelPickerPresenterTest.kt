package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ModelPickerPresenter].
 */
class ModelPickerPresenterTest {
    private val downloadedModel =
        testModel(id = "google/gemma-2-2b-it", downloadStatus = DownloadStatus.DOWNLOADED)
    private val remoteModel =
        testModel(id = "google/gemma-4-e2b", downloadStatus = DownloadStatus.NOT_DOWNLOADED, localPath = null)

    @Test
    fun `given models load - emits success with model list`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel, remoteModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertEquals(listOf(downloadedModel, remoteModel), state.models)
            }
        }

    @Test
    fun `given model load fails - emits success with empty list`() =
        runTest {
            val fakeRepo = FakeModelRepository(getException = IllegalStateException("Storage error"))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertTrue(state.models.isEmpty())
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Back)
                navigator.awaitPop()
            }
        }

    @Test
    fun `given download event - starts model download`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(remoteModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Download(remoteModel))
                assertEquals(listOf(remoteModel), fakeRepo.downloadCalls)
            }
        }

    @Test
    fun `given cancel download event - cancels model download`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(remoteModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.CancelDownload(remoteModel))
                assertEquals(listOf(remoteModel), fakeRepo.cancelDownloadCalls)
            }
        }

    @Test
    fun `given delete event - deletes model`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Delete(downloadedModel))
                assertEquals(listOf(downloadedModel), fakeRepo.deleteCalls)
            }
        }

    @Test
    fun `given select event - selects model and pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Select(downloadedModel))
                navigator.awaitPop()
                assertEquals(downloadedModel, fakeRepo.getSelectedModel())
            }
        }
}
