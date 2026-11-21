package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.fakeJwt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurveyViewModelTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository
    private lateinit var viewModel: SurveyViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        repository = SyncPassRepository(context)
        viewModel = SurveyViewModel(repository)
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        Dispatchers.resetMain()
    }

    @Test fun onRatingCoercesIntoOneToFiveRange() {
        viewModel.onRating(10)
        assertEquals(5, viewModel.state.value.rating)
        viewModel.onRating(-3)
        assertEquals(1, viewModel.state.value.rating)
    }

    @Test fun onCommentTruncatesAt2000Characters() {
        viewModel.onComment("x".repeat(3000))
        assertEquals(2000, viewModel.state.value.comment.length)
    }

    @Test fun guardarWithoutSessionRequiresLogin() {
        viewModel.guardar(1)
        assertTrue(viewModel.state.value.message.contains("Inicia sesión"))
    }

    @Test fun guardarWithoutTallerIdIsRejected() {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        viewModel.guardar(0)
        assertTrue(viewModel.state.value.message.contains("Selecciona un taller"))
    }

    @Test fun guardarQueuesSurveyOfflineAndClearsComment() = runBlocking {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        viewModel.onRating(4)
        viewModel.onComment("Muy buena actividad")
        withTimeout(5_000) {
            viewModel.guardar(5)
            viewModel.state.first { !it.busy && it.message.isNotBlank() }
        }
        assertTrue(viewModel.state.value.message.contains("Encuesta guardada"))
        assertEquals("", viewModel.state.value.comment)
        assertEquals(1, repository.pending("participante-1").size)
    }
}
