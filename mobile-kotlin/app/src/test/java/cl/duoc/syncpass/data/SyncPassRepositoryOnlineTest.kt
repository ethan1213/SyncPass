package cl.duoc.syncpass.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response

private fun httpError(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "error".toResponseBody("text/plain".toMediaTypeOrNull())))

/**
 * Fake de [SyncPassApi]: cada metodo devuelve lo que el test deja preparado
 * o lanza [errors] si corresponde, sin tocar la red real.
 */
internal class FakeSyncPassApi : SyncPassApi {
    var loginResult: LoginResult = LoginResult("access", "id", "refresh", 3600)
    var registerResult: RegisterResult = RegisterResult(userConfirmed = false, destination = "e@x.cl")
    var talleresResult: List<TallerDto> = emptyList()
    var joinGuestResult: JoinResult = JoinResult("presente", "r1", "guest:abc", true)
    var joinAccountResult: JoinResult = JoinResult("presente", "r2", "sub-1", false)
    var recordingStatusResult: RecordingStatus = RecordingStatus(grabado = true, resumenDisponible = true, mensaje = "ok")
    var syncResult: SyncResult = SyncResult(emptyList())
    var bitacoraResult: BitacoraDto = BitacoraDto("contenido", "resumen", "2026-11-19", emptyList())
    var respuestaResult: RespuestaBitacora = RespuestaBitacora("respuesta", 0, "cita")
    var borradorResult = BorradorBitacora("Resumen de prueba.", "extractivo")
    val solicitudesResumen = mutableListOf<Pair<Long, PeticionResumen>>()
    val publicacionesBitacora = mutableListOf<Pair<Long, PublicacionBitacora>>()
    var marcarPendienteResult: InscripcionDto = InscripcionDto(1, 1, "pendiente")
    var misInscripcionesResult: List<InscripcionDto> = emptyList()

    var errorOnJoinGuest: Throwable? = null
    var errorOnJoinAccount: Throwable? = null
    var errorOnTalleres: Throwable? = null
    var errorOnBitacora: Throwable? = null
    var rejectedSync: Map<String, Int> = emptyMap()

    val loginCalls = mutableListOf<LoginRequest>()
    val registerCalls = mutableListOf<RegisterRequest>()
    val confirmCalls = mutableListOf<ConfirmRequest>()
    val resendCalls = mutableListOf<UsernameRequest>()
    val syncCalls = mutableListOf<SyncBatch>()

    override suspend fun login(request: LoginRequest): LoginResult {
        loginCalls += request
        return loginResult
    }
    override suspend fun register(request: RegisterRequest): RegisterResult {
        registerCalls += request
        return registerResult
    }
    override suspend fun confirm(request: ConfirmRequest) { confirmCalls += request }
    override suspend fun resend(request: UsernameRequest) { resendCalls += request }
    override suspend fun talleres(): List<TallerDto> {
        errorOnTalleres?.let { throw it }
        return talleresResult
    }
    override suspend fun joinGuest(tallerId: Long, request: GuestJoinRequest): JoinResult {
        errorOnJoinGuest?.let { throw it }
        return joinGuestResult
    }
    override suspend fun recordingStatus(tallerId: Long): RecordingStatus = recordingStatusResult
    override suspend fun joinAccount(tallerId: Long, authorization: String, location: JoinLocation): JoinResult {
        errorOnJoinAccount?.let { throw it }
        return joinAccountResult
    }
    override suspend fun sync(authorization: String, batch: SyncBatch): SyncResult {
        syncCalls += batch
        batch.registros.firstNotNullOfOrNull { rejectedSync[it.id] }?.let { throw httpError(it) }
        return syncResult
    }
    override suspend fun bitacora(tallerId: Long, authorization: String): BitacoraDto {
        errorOnBitacora?.let { throw it }
        return bitacoraResult
    }
    override suspend fun preguntarBitacora(tallerId: Long, authorization: String, pregunta: PreguntaBitacora): RespuestaBitacora =
        respuestaResult
    override suspend fun prepararResumen(tallerId: Long, authorization: String, peticion: PeticionResumen): BorradorBitacora {
        solicitudesResumen += tallerId to peticion
        return borradorResult
    }
    override suspend fun publicarBitacora(tallerId: Long, authorization: String, publicacion: PublicacionBitacora): BitacoraDto {
        publicacionesBitacora += tallerId to publicacion
        return BitacoraDto(publicacion.contenido, publicacion.resumenRevisado, "2026-11-27",
            listOf(publicacion.contenido))
    }
    override suspend fun marcarPendiente(tallerId: Long, authorization: String): InscripcionDto = marcarPendienteResult
    override suspend fun misInscripciones(authorization: String): List<InscripcionDto> = misInscripcionesResult
}

/**
 * Cobertura de las ramas EN LINEA de SyncPassRepository (login/registro,
 * catalogo en vivo, sincronizacion exitosa y descartes HTTP 4xx) usando los
 * dos puntos de inyeccion agregados a SyncPassRepository (injectedApi,
 * injectedOnline). Complementa SyncPassRepositoryOfflineTest, que solo cubre
 * las ramas sin conexion.
 */
@RunWith(RobolectricTestRunner::class)
class SyncPassRepositoryOnlineTest {
    private lateinit var context: Context
    private lateinit var api: FakeSyncPassApi
    private var online = true
    private lateinit var repository: SyncPassRepository

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        api = FakeSyncPassApi()
        online = true
        repository = SyncPassRepository(context, api, { online })
    }

    @After fun cleanup() {
        context.deleteDatabase("syncpass.db")
        context.deleteDatabase("syncpass-local-test.db")
    }

    @Test fun localAndCloudDatabasesKeepSeparateQueues() = runBlocking {
        val local = SyncPassRepository(context, api, { true }, databaseName = "syncpass-local-test.db")
        local.queue(PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 1, rating = 5))
        assertEquals(1, local.pending("coord-1").size)
        assertTrue(repository.pending("coord-1").isEmpty())
    }

    @Test fun loginSuccessReturnsTokensAndForwardsCredentials() = runBlocking {
        val result = repository.login("usuario", "clave")
        assertEquals("access", result.accessToken)
        assertEquals("usuario", api.loginCalls.single().username)
    }

    @Test fun registerSuccessForwardsRequest() = runBlocking {
        val result = repository.register("usuario", "u@x.cl", "Nick", "clave")
        assertEquals("e@x.cl", result.destination)
        assertEquals("u@x.cl", api.registerCalls.single().email)
    }

    @Test fun confirmAndResendSuccessDoNotThrow() = runBlocking {
        repository.confirm("usuario", "123456")
        repository.resend("usuario")
        assertEquals("123456", api.confirmCalls.single().code)
        assertEquals("usuario", api.resendCalls.single().username)
    }

    @Test fun talleresSuccessPersistsRowsInRoom() = runBlocking {
        api.talleresResult = listOf(TallerDto(id = 1, title = "Huerto", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 10))
        val rows = repository.talleres()
        assertEquals(1, rows.size)
        assertEquals("Huerto", repository.cachedTalleres().single().title)
    }

    @Test fun catalogOnlineReturnsLiveRowsNotFromCache() = runBlocking {
        api.talleresResult = listOf(TallerDto(id = 2, title = "Reciclaje", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 5))
        val snapshot = repository.catalog()
        assertFalse(snapshot.fromCache)
        assertEquals("Reciclaje", snapshot.rows.single().title)
    }

    @Test fun catalogFallsBackToCacheWhenApiFailsEvenOnline() = runBlocking {
        api.errorOnTalleres = RuntimeException("servidor caido")
        val snapshot = repository.catalog()
        assertTrue(snapshot.fromCache)
        assertTrue(snapshot.rows.isEmpty())
    }

    @Test fun joinGuestOnlineCallsApiInsteadOfQueueing() = runBlocking {
        val result = repository.joinGuest(7, "Ana")
        assertEquals("presente", result.estado)
        assertEquals(0, repository.pendingEntries())
    }

    @Test fun joinAccountOnlineCallsApiInsteadOfQueueing() = runBlocking {
        val result = repository.joinAccount(8, "token-abc")
        assertEquals("sub-1", result.participante)
        assertEquals(0, repository.pendingEntries())
    }

    @Test fun marcarPendienteYMisInscripcionesForwardToApiWhenOnline() = runBlocking {
        api.marcarPendienteResult = InscripcionDto(7, 3, "pendiente")
        val creada = repository.marcarPendiente(3, "token")
        assertEquals("pendiente", creada.estado)

        api.misInscripcionesResult = listOf(InscripcionDto(7, 3, "pendiente"))
        val mias = repository.misInscripciones("token")
        assertEquals(1, mias.size)
        assertEquals(3L, mias.first().tallerId)
    }

    @Test fun recordingStatusAndBitacoraForwardToApiWhenOnline() = runBlocking {
        assertTrue(repository.recordingStatus(1).grabado)
        assertEquals("contenido", repository.bitacora(1, "token").contenido)
        assertEquals("respuesta", repository.preguntarBitacora(1, "token", "hola?").respuesta)
    }

    @Test fun syncSendsPendingBatchAndRemovesOnlyAcceptedRows() = runBlocking {
        repository.queue(PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 1, rating = 5))
        val aceptado = PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 2, rating = 4)
        repository.queue(aceptado)
        api.syncResult = SyncResult(acceptedIds = listOf(aceptado.id))

        val total = repository.sync("coord-1", "token-xyz")

        assertEquals(1, total)
        val restante = repository.pending("coord-1")
        assertEquals(1, restante.size)
        assertEquals(1L, restante.first().tallerId)
    }

    @Test fun syncKeepsRecordsFromAnotherCatalogForLater() = runBlocking {
        api.talleresResult = listOf(TallerDto(id = 1, title = "Taller local", descripcion = null,
            area = null, unidadVecinal = null, comuna = null, direccion = null,
            fechaHora = null, docenteLider = null, cuposMaximos = 10))
        repository.talleres()
        val local = PendingRecord(owner = "coord-1", tipo = "asistencia", tallerId = 1,
            participante = "SP1:11111111-2222-4333-8444-555555555555")
        val otroCatalogo = PendingRecord(owner = "coord-1", tipo = "asistencia", tallerId = 4,
            participante = "SP1:11111111-2222-4333-8444-555555555555")
        repository.queue(local)
        repository.queue(otroCatalogo)
        api.syncResult = SyncResult(acceptedIds = listOf(local.id))

        assertEquals(1, repository.sync("coord-1", "token-xyz"))
        assertEquals(listOf(local.id), api.syncCalls.single().registros.map { it.id })
        assertEquals(listOf(otroCatalogo.id), repository.pending("coord-1").map { it.id })
    }

    @Test fun syncRetains404And409WithoutBlockingValidRecords() = runBlocking {
        val ausente = PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 1, rating = 5)
        val conflicto = PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 2, rating = 5)
        val valido = PendingRecord(owner = "coord-1", tipo = "encuesta", tallerId = 3, rating = 5)
        listOf(ausente, conflicto, valido).forEach { repository.queue(it) }
        api.rejectedSync = mapOf(ausente.id to 404, conflicto.id to 409)
        api.syncResult = SyncResult(listOf(valido.id))

        assertEquals(1, repository.sync("coord-1", "token"))
        assertEquals(setOf(ausente.id, conflicto.id), repository.pending("coord-1").map { it.id }.toSet())
    }

    @Test fun refreshedCatalogReplacesOldRowsAndRemainsVisibleOffline() = runBlocking {
        val original = TallerDto(id = 1, title = "Primera edición", descripcion = null,
            area = null, unidadVecinal = null, comuna = null, direccion = null,
            fechaHora = null, docenteLider = null, cuposMaximos = 10)
        api.talleresResult = listOf(original, original.copy(id = 2))
        assertEquals(2, repository.catalog().rows.size)
        api.talleresResult = listOf(original.copy(title = "Edición desde web"))
        assertFalse(repository.catalog().fromCache)
        online = false
        val cache = repository.catalog()
        assertTrue(cache.fromCache)
        assertEquals(listOf("Edición desde web"), cache.rows.map { it.title })
    }

    @Test fun flushEntriesOnlineSendsGuestEntryAndRemovesIt() = runBlocking {
        online = false
        repository.joinGuest(3, "Vecino")
        online = true
        assertEquals(1, repository.pendingEntries())

        val result = repository.flushEntries(null)

        assertEquals(1, result.enviados)
        assertEquals(0, result.descartados)
        assertEquals(0, repository.pendingEntries())
    }

    @Test fun flushEntriesOnlineSendsAccountEntryOnlyWhenTokenPresent() = runBlocking {
        online = false
        repository.joinAccount(4, "ignorado-offline")
        online = true

        val sinToken = repository.flushEntries(null)
        assertEquals(0, sinToken.enviados)
        assertEquals(1, repository.pendingEntries())

        val conToken = repository.flushEntries("token-real")
        assertEquals(1, conToken.enviados)
        assertEquals(0, repository.pendingEntries())
    }

    @Test fun flushEntriesDiscardsRowOnDefinitiveHttpRejection() = runBlocking {
        online = false
        repository.joinGuest(5, "Rechazado")
        online = true
        api.errorOnJoinGuest = httpError(409)

        val result = repository.flushEntries(null)

        assertEquals(0, result.enviados)
        assertEquals(1, result.descartados)
        assertTrue(result.motivo!!.contains("Taller #5"))
        assertEquals(0, repository.pendingEntries())
    }

    @Test fun flushEntriesKeepsRowWhenServerRejectionIsRetryable() = runBlocking {
        online = false
        repository.joinGuest(6, "Reintentable")
        online = true
        api.errorOnJoinGuest = httpError(401)

        val result = repository.flushEntries(null)

        assertEquals(0, result.enviados)
        assertEquals(0, result.descartados)
        assertNull(result.motivo)
        assertEquals(1, repository.pendingEntries())
    }
}
