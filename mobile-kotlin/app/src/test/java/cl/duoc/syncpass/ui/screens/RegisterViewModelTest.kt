package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.SyncPassRepository
import kotlinx.coroutines.Dispatchers
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
class RegisterViewModelTest {
    private lateinit var context: Context
    private lateinit var viewModel: RegisterViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        viewModel = RegisterViewModel(SyncPassRepository(context))
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        Dispatchers.resetMain()
    }

    @Test fun fieldSettersUpdateStateAndClearError() {
        viewModel.onUsername("ana")
        viewModel.onEmail("ana@duoc.cl")
        viewModel.onNickname("Ana")
        viewModel.onPassword("secreto123")
        viewModel.onCode("654321")
        val estado = viewModel.state.value
        assertEquals("ana", estado.username)
        assertEquals("ana@duoc.cl", estado.email)
        assertEquals("Ana", estado.nickname)
        assertEquals("secreto123", estado.password)
        assertEquals("654321", estado.code)
        assertTrue(estado.puedeRegistrar)
        assertTrue(estado.puedeConfirmar)
    }

    @Test fun registrarWithIncompleteFieldsIsNoOp() = runBlocking {
        viewModel.onUsername("ana")
        var llamado = false
        viewModel.registrar { llamado = true }
        withTimeout(2_000) { viewModel.state.first { true } }
        assertFalse(llamado)
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun registrarWithoutNetworkReportsError() = runBlocking {
        viewModel.onUsername("ana")
        viewModel.onEmail("ana@duoc.cl")
        viewModel.onNickname("Ana")
        viewModel.onPassword("secreto123")
        var llamado = false
        withTimeout(5_000) {
            viewModel.registrar { llamado = true }
            viewModel.state.first { it.error != null }
        }
        assertFalse(llamado)
        assertFalse(viewModel.state.value.busy)
        assertTrue(viewModel.state.value.error!!.contains("Sin conexión"))
    }

    @Test fun confirmarWithBlankCodeIsNoOp() = runBlocking {
        viewModel.onUsername("ana")
        viewModel.confirmar { }
        withTimeout(2_000) { viewModel.state.first { true } }
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun confirmarWithoutNetworkReportsError() = runBlocking {
        viewModel.onUsername("ana")
        viewModel.onCode("123456")
        withTimeout(5_000) {
            viewModel.confirmar { }
            viewModel.state.first { it.error != null }
        }
        assertTrue(viewModel.state.value.error!!.contains("Sin conexión"))
    }

    @Test fun reenviarCodigoWithoutNetworkReportsError() = runBlocking {
        viewModel.onUsername("ana")
        withTimeout(5_000) {
            viewModel.reenviarCodigo()
            viewModel.state.first { it.error != null }
        }
        assertTrue(viewModel.state.value.error!!.contains("Sin conexión"))
    }
}
