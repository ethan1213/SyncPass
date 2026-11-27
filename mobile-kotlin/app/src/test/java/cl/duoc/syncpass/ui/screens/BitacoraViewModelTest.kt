package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.BitacoraDto
import cl.duoc.syncpass.data.FakeSyncPassApi
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.fakeJwt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitacoraViewModelTest {
    private lateinit var context: Context
    private lateinit var viewModel: BitacoraViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        viewModel = BitacoraViewModel(SyncPassRepository(context))
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        Dispatchers.resetMain()
    }

    @Test fun onQuestionTruncatesAt300Characters() {
        viewModel.onQuestion("x".repeat(500))
        assertEquals(300, viewModel.state.value.question.length)
    }

    @Test fun cargarWithoutSessionRequiresLogin() {
        viewModel.cargar(1)
        assertTrue(viewModel.state.value.status.contains("Inicia sesión"))
    }

    @Test fun cargarWithoutNetworkReportsError() = runBlocking {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        withTimeout(5_000) {
            viewModel.cargar(1)
            viewModel.state.first { !it.busy && it.status.isNotBlank() }
        }
        assertTrue(viewModel.state.value.status.isNotBlank())
    }

    @Test fun unpublishedBitacoraCanBeRefreshedAfterPublication() = runBlocking {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        val api = FakeSyncPassApi()
        api.errorOnBitacora = HttpException(Response.error<Any>(404, "".toResponseBody()))
        viewModel = BitacoraViewModel(SyncPassRepository(context, api, { true }))
        viewModel.cargar(2)
        withTimeout(5000) { viewModel.state.first { !it.busy } }
        assertTrue(viewModel.state.value.status.contains("cuando el equipo publique"))
        assertNull(viewModel.state.value.bitacora)
        api.errorOnBitacora = null
        viewModel.cargar(2)
        val published = withTimeout(5000) { viewModel.state.first { it.bitacora != null } }
        assertEquals("resumen", published.bitacora?.resumen)
    }

    @Test fun cargarWithPublishedButEmptyBitacoraShowsFriendlyMessage() = runBlocking {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        val api = FakeSyncPassApi()
        api.bitacoraResult = BitacoraDto("", "", "2026-11-26", emptyList())
        viewModel = BitacoraViewModel(SyncPassRepository(context, api, { true }))
        viewModel.cargar(3)
        withTimeout(5000) { viewModel.state.first { !it.busy } }
        assertTrue(viewModel.state.value.status.contains("cuando el equipo publique"))
        assertNull(viewModel.state.value.bitacora)
    }

    @Test fun preguntarWithoutTokenIsNoOp() {
        viewModel.onQuestion("algo")
        viewModel.preguntar(1)
        assertNull(viewModel.state.value.answer)
    }

    @Test fun preguntarWithBlankQuestionIsNoOp() {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        viewModel.preguntar(1)
        assertEquals(false, viewModel.state.value.busy)
    }

    @Test fun preguntarWithoutNetworkReportsError() = runBlocking {
        AuthSession.signedIn(fakeJwt("participante-1"), null)
        viewModel.onQuestion("cuando es la proxima sesion?")
        withTimeout(5_000) {
            viewModel.preguntar(1)
            viewModel.state.first { !it.busy }
        }
        assertNull(viewModel.state.value.answer)
    }
}
