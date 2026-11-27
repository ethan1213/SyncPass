package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.FakeSyncPassApi
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {
    private lateinit var context: Context
    private lateinit var viewModel: LoginViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        viewModel = LoginViewModel(SyncPassRepository(context))
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        Dispatchers.resetMain()
    }

    @Test fun fieldSettersClearPreviousError() {
        viewModel.onUsername("ana")
        viewModel.onPassword("secreto")
        assertTrue(viewModel.state.value.puedeEnviar)
    }

    @Test fun iniciarSesionWithIncompleteFieldsIsNoOp() = runBlocking {
        viewModel.onUsername("ana")
        val evento = async(start = CoroutineStart.UNDISPATCHED) { viewModel.loginSuccess.first() }
        viewModel.iniciarSesion()
        assertFalse(evento.isCompleted)
        evento.cancel()
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun iniciarSesionWithoutNetworkReportsErrorAndDoesNotCallback() = runBlocking {
        viewModel.onUsername("ana")
        viewModel.onPassword("secreto123")
        val evento = async(start = CoroutineStart.UNDISPATCHED) { viewModel.loginSuccess.first() }
        withTimeout(5_000) {
            viewModel.iniciarSesion()
            viewModel.state.first { it.error != null }
        }
        assertFalse(evento.isCompleted)
        evento.cancel()
        assertFalse(viewModel.state.value.busy)
        assertTrue(viewModel.state.value.error!!.contains("Sin conexión"))
    }

    @Test fun iniciarSesionEmitsOneNavigationEventAndClearsPassword() = runBlocking {
        val api = FakeSyncPassApi()
        viewModel = LoginViewModel(SyncPassRepository(context, api, { true }))
        viewModel.onUsername(" ana ")
        viewModel.onPassword("clave-de-prueba")
        viewModel.iniciarSesion()

        withTimeout(5_000) { viewModel.loginSuccess.first() }
        assertEquals("ana", api.loginCalls.single().username)
        assertEquals("access", AuthSession.accessToken)
        assertEquals("", viewModel.state.value.password)
        assertFalse(viewModel.state.value.busy)
        val otroEvento = async(start = CoroutineStart.UNDISPATCHED) { viewModel.loginSuccess.first() }
        assertFalse(otroEvento.isCompleted)
        otroEvento.cancel()
    }
}
