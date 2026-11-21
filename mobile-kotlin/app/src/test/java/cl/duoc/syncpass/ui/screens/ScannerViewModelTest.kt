package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.duoc.syncpass.data.AuthSession
import cl.duoc.syncpass.data.SyncPassDb
import cl.duoc.syncpass.data.SyncPassRepository
import cl.duoc.syncpass.data.TallerDto
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val CODIGO_VALIDO = "SP1:123e4567-e89b-12d3-a456-426614174000"

@RunWith(RobolectricTestRunner::class)
class ScannerViewModelTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository
    private lateinit var viewModel: ScannerViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        AuthSession.signOut()
        repository = SyncPassRepository(context)
        viewModel = ScannerViewModel(repository)
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        AuthSession.signOut()
        Dispatchers.resetMain()
    }

    private fun seedTaller(taller: TallerDto) = runBlocking {
        val seedDb = Room.databaseBuilder(context, SyncPassDb::class.java, "syncpass.db").build()
        seedDb.talleres().save(listOf(taller))
        seedDb.close()
    }

    @Test fun cargarDetectsGeocercaFromCachedTaller() = runBlocking {
        seedTaller(TallerDto(id = 1, title = "T", descripcion = null, area = null, unidadVecinal = null,
            comuna = null, direccion = null, fechaHora = null, docenteLider = null, cuposMaximos = 10,
            latitud = -33.0, longitud = -70.0))
        withTimeout(5_000) {
            viewModel.cargar(1)
            viewModel.state.first { it.exigeGeocerca }
        }
        assertTrue(viewModel.state.value.exigeGeocerca)
    }

    @Test fun cargarWithoutGeocercaStaysFalse() = runBlocking {
        viewModel.cargar(404)
        withTimeout(5_000) { viewModel.state.first { true } }
        assertFalse(viewModel.state.value.exigeGeocerca)
    }

    @Test fun onCodeTrimsWhitespace() {
        viewModel.onCode("  $CODIGO_VALIDO  ")
        assertEquals(CODIGO_VALIDO, viewModel.state.value.code)
    }

    @Test fun onQrLeidoSetsConfirmationMessage() {
        viewModel.onQrLeido(CODIGO_VALIDO)
        assertEquals(CODIGO_VALIDO, viewModel.state.value.code)
        assertTrue(viewModel.state.value.message.contains("QR leído"))
    }

    @Test fun onCamaraNoDisponibleSetsMessage() {
        viewModel.onCamaraNoDisponible()
        assertTrue(viewModel.state.value.message.contains("Cámara no disponible"))
    }

    @Test fun onUbicacionDenegadaSetsMessage() {
        viewModel.onUbicacionDenegada()
        assertTrue(viewModel.state.value.message.contains("Permite la ubicación"))
    }

    @Test fun guardarWithoutSessionRequiresLogin() {
        viewModel.onCode(CODIGO_VALIDO)
        viewModel.guardar(1, permisoUbicacion = true)
        assertTrue(viewModel.state.value.message.contains("Inicia sesión"))
    }

    @Test fun guardarWithoutAccreditingRoleIsRejected() {
        AuthSession.signedIn(fakeJwt("participante-1", listOf("asistente")), null)
        viewModel.onCode(CODIGO_VALIDO)
        viewModel.guardar(1, permisoUbicacion = true)
        assertTrue(viewModel.state.value.message.contains("no puede acreditar"))
    }

    @Test fun guardarWithInvalidCodeIsRejected() {
        AuthSession.signedIn(fakeJwt("coordinador-1", listOf("coordinador")), null)
        viewModel.onCode("codigo-invalido")
        viewModel.guardar(1, permisoUbicacion = true)
        assertTrue(viewModel.state.value.message.contains("código válido") ||
            viewModel.state.value.message.contains("QR válido"))
    }

    @Test fun guardarRequiresLocationPermissionWhenTallerNeedsGeocerca() = runBlocking {
        seedTaller(TallerDto(id = 2, title = "T", descripcion = null, area = null, unidadVecinal = null,
            comuna = null, direccion = null, fechaHora = null, docenteLider = null, cuposMaximos = 10,
            latitud = -33.0, longitud = -70.0))
        AuthSession.signedIn(fakeJwt("coordinador-1", listOf("monitor")), null)
        withTimeout(5_000) { viewModel.cargar(2); viewModel.state.first { it.exigeGeocerca } }
        viewModel.onCode(CODIGO_VALIDO)
        viewModel.guardar(2, permisoUbicacion = false)
        assertTrue(viewModel.state.value.message.contains("ubicación GPS"))
    }

    @Test fun guardarSucceedsOfflineAndQueuesAttendance() = runBlocking {
        AuthSession.signedIn(fakeJwt("coordinador-1", listOf("admin")), null)
        viewModel.onCode(CODIGO_VALIDO)
        withTimeout(5_000) {
            viewModel.guardar(3, permisoUbicacion = true)
            viewModel.state.first { !it.busy && it.message.isNotBlank() }
        }
        assertTrue(viewModel.state.value.message.contains("guardada en el dispositivo"))
        assertEquals("", viewModel.state.value.code)
        assertEquals(1, repository.pending("coordinador-1").size)
    }
}
