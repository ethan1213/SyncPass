package cl.duoc.syncpass.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import cl.duoc.syncpass.data.*
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

private class FakeApi : SyncPassApi by cl.duoc.syncpass.data.FakeSyncPassApi() {
    var misInscripcionesResult: List<InscripcionDto> = emptyList()
    var marcarPendienteResult: InscripcionDto = InscripcionDto(1, 1, "pendiente")
    var errorEnMarcar: Throwable? = null

    override suspend fun login(request: LoginRequest) = throw NotImplementedError()
    override suspend fun register(request: RegisterRequest) = throw NotImplementedError()
    override suspend fun confirm(request: ConfirmRequest) { throw NotImplementedError() }
    override suspend fun resend(request: UsernameRequest) { throw NotImplementedError() }
    override suspend fun talleres(): List<TallerDto> = emptyList()
    override suspend fun joinGuest(tallerId: Long, request: GuestJoinRequest) = throw NotImplementedError()
    override suspend fun recordingStatus(tallerId: Long) = throw NotImplementedError()
    override suspend fun joinAccount(tallerId: Long, authorization: String, location: JoinLocation) = throw NotImplementedError()
    override suspend fun sync(authorization: String, batch: SyncBatch) = throw NotImplementedError()
    override suspend fun bitacora(tallerId: Long, authorization: String) = throw NotImplementedError()
    override suspend fun preguntarBitacora(tallerId: Long, authorization: String, pregunta: PreguntaBitacora) = throw NotImplementedError()
    override suspend fun marcarPendiente(tallerId: Long, authorization: String): InscripcionDto {
        errorEnMarcar?.let { throw it }
        return marcarPendienteResult
    }
    override suspend fun misInscripciones(authorization: String): List<InscripcionDto> = misInscripcionesResult
}

/** RF12: la vista "Mis talleres pendientes" lee el catalogo y marca preinscripciones. */
@RunWith(RobolectricTestRunner::class)
class MisInscripcionesViewModelTest {
    private lateinit var context: Context
    private lateinit var api: FakeApi
    private lateinit var repository: SyncPassRepository
    private lateinit var viewModel: MisInscripcionesViewModel

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        api = FakeApi()
        repository = SyncPassRepository(context, api, { true })
        viewModel = MisInscripcionesViewModel(repository)
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        Dispatchers.resetMain()
    }

    private fun seedTaller(id: Long) = runBlocking {
        val db = Room.databaseBuilder(context, SyncPassDb::class.java, "syncpass.db").build()
        db.talleres().save(listOf(TallerDto(id = id, title = "Huerto", descripcion = null, area = null,
            unidadVecinal = null, comuna = "La Florida", direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 10)))
        db.close()
    }

    @Test fun cargarSinTokenInformaErrorYNoLlamaApi() = runBlocking {
        viewModel.cargar(null)
        val estado = withTimeout(5_000) { viewModel.state.first { it.error.isNotBlank() } }
        assertTrue(estado.error.contains("sesión"))
    }

    @Test fun cargarListaTalleresYMarcaLosYaPendientes() = runBlocking {
        seedTaller(1)
        api.misInscripcionesResult = listOf(InscripcionDto(5, 1, "pendiente"))
        viewModel.cargar("token")
        val estado = withTimeout(5_000) { viewModel.state.first { it.talleres.isNotEmpty() } }
        assertEquals(1, estado.talleres.size)
        assertTrue(estado.pendientes.contains(1L))
    }

    @Test fun marcarAgregaElTallerALosPendientes() = runBlocking {
        seedTaller(2)
        api.marcarPendienteResult = InscripcionDto(9, 2, "pendiente")
        viewModel.cargar("token")
        withTimeout(5_000) { viewModel.state.first { it.talleres.isNotEmpty() } }

        viewModel.marcar(2, "token")
        val estado = withTimeout(5_000) { viewModel.state.first { it.pendientes.contains(2L) } }
        assertTrue(estado.pendientes.contains(2L))
        assertEquals(null, estado.marcandoId)
    }

    @Test fun marcarReportaErrorDelServidorSinRomper() = runBlocking {
        seedTaller(3)
        api.errorEnMarcar = IllegalStateException("Este taller es solo para: monitor")
        viewModel.cargar("token")
        withTimeout(5_000) { viewModel.state.first { it.talleres.isNotEmpty() } }

        viewModel.marcar(3, "token")
        val estado = withTimeout(5_000) { viewModel.state.first { it.error.isNotBlank() } }
        assertTrue(estado.error.contains("monitor"))
        assertTrue(!estado.pendientes.contains(3L))
    }
}
