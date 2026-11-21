package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
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
        var llamado = false
        viewModel.iniciarSesion { llamado = true }
        withTimeout(2_000) { viewModel.state.first { true } }
        assertFalse(llamado)
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun iniciarSesionWithoutNetworkReportsErrorAndDoesNotCallback() = runBlocking {
        viewModel.onUsername("ana")
        viewModel.onPassword("secreto123")
        var llamado = false
        withTimeout(5_000) {
            viewModel.iniciarSesion { llamado = true }
            viewModel.state.first { it.error != null }
        }
        assertFalse(llamado)
        assertFalse(viewModel.state.value.busy)
        assertTrue(viewModel.state.value.error!!.contains("Sin conexión"))
    }
}
