package cl.duoc.syncpass.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Cobertura adicional de SyncPassRepository para las ramas sin conexion: son
 * las unicas que se pueden ejercitar de forma determinista en un test unitario,
 * porque el repositorio construye su propio Retrofit/OkHttp internamente (no
 * hay semilla de inyeccion de dependencias para sustituir la API por un fake
 * sin tocar produccion). Las ramas que exigen una respuesta real del servidor
 * (sync()/flushEntries() exitosos, descarte por HTTP 4xx) quedan fuera de
 * alcance por la misma razon.
 */
@RunWith(RobolectricTestRunner::class)
class SyncPassRepositoryOfflineTest {
    private lateinit var context: Context
    private lateinit var repository: SyncPassRepository

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("syncpass.db")
        context.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE).edit().clear().commit()
        repository = SyncPassRepository(context)
    }

    @After fun cleanup() { context.deleteDatabase("syncpass.db") }

    /** Inserta filas directamente en Room para simular un catalogo ya sincronizado. */
    private fun seedTalleres(vararg talleres: TallerDto) = runBlocking {
        val seedDb = Room.databaseBuilder(context, SyncPassDb::class.java, "syncpass.db").build()
        seedDb.talleres().save(talleres.toList())
        seedDb.close()
    }

    @Test fun joinAccountOfflineQueuesPendingEntry() = runBlocking {
        val result = repository.joinAccount(9, "token-123")
        assertEquals(ESTADO_PENDIENTE, result.estado)
        assertFalse(result.invitado)
        assertEquals(1, repository.pendingEntriesFor(9))
    }

    @Test fun catalogOfflineReturnsCachedRowsFlaggedAsFromCache() = runBlocking {
        seedTalleres(TallerDto(id = 1, title = "Huerto", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 10))
        val snapshot = repository.catalog()
        assertTrue(snapshot.fromCache)
        assertEquals(1, snapshot.rows.size)
        assertEquals("Huerto", snapshot.rows.first().title)
    }

    @Test fun exigeGeocercaReflectsCachedCoordinates() = runBlocking {
        seedTalleres(
            TallerDto(id = 1, title = "Con geocerca", descripcion = null, area = null,
                unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
                docenteLider = null, cuposMaximos = 10, latitud = -33.4, longitud = -70.6),
            TallerDto(id = 2, title = "Sin geocerca", descripcion = null, area = null,
                unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
                docenteLider = null, cuposMaximos = 10)
        )
        assertTrue(repository.exigeGeocerca(1))
        assertFalse(repository.exigeGeocerca(2))
        assertFalse(repository.exigeGeocerca(999))
    }

    @Test fun acreditarQueuesPendingRecordForOwner() = runBlocking {
        repository.acreditar("coordinador-1", 5, "participante-xyz")
        val cola = repository.pending("coordinador-1")
        assertEquals(1, cola.size)
        assertEquals("asistencia", cola.first().tipo)
        assertEquals("participante-xyz", cola.first().participante)
    }

    @Test fun authOperationsThrowOfflineExceptionWithoutNetwork() = runBlocking {
        val loginError = runCatching { repository.login("user", "pass") }.exceptionOrNull()
        assertTrue(loginError is OfflineException)
        val registerError = runCatching { repository.register("user", "mail@x.cl", "nick", "pass") }.exceptionOrNull()
        assertTrue(registerError is OfflineException)
        val confirmError = runCatching { repository.confirm("user", "123456") }.exceptionOrNull()
        assertTrue(confirmError is OfflineException)
        val resendError = runCatching { repository.resend("user") }.exceptionOrNull()
        assertTrue(resendError is OfflineException)
    }

    @Test fun networkOnlyOperationsThrowOfflineExceptionWithoutNetwork() = runBlocking {
        val recordingError = runCatching { repository.recordingStatus(1) }.exceptionOrNull()
        assertTrue(recordingError is OfflineException)
        val syncError = runCatching { repository.sync("owner", "token") }.exceptionOrNull()
        assertTrue(syncError is OfflineException)
        val talleresError = runCatching { repository.talleres() }.exceptionOrNull()
        assertTrue(talleresError is OfflineException)
        val bitacoraError = runCatching { repository.bitacora(1, "token") }.exceptionOrNull()
        assertTrue(bitacoraError is OfflineException)
        val preguntaError = runCatching { repository.preguntarBitacora(1, "token", "hola") }.exceptionOrNull()
        assertTrue(preguntaError is OfflineException)
        val marcarError = runCatching { repository.marcarPendiente(1, "token") }.exceptionOrNull()
        assertTrue(marcarError is OfflineException)
        val misInscripcionesError = runCatching { repository.misInscripciones("token") }.exceptionOrNull()
        assertTrue(misInscripcionesError is OfflineException)
    }

    @Test fun flushEntriesWithoutNetworkReturnsEmptyResultAndKeepsRow() = runBlocking {
        repository.joinAccount(3, "token")
        assertEquals(1, repository.pendingEntries())
        val result = repository.flushEntries("token")
        assertEquals(0, result.enviados)
        assertEquals(0, result.descartados)
        assertNull(result.motivo)
        assertEquals(1, repository.pendingEntries())
    }

    @Test fun cachedTallerFindsSeededRowById() = runBlocking {
        seedTalleres(TallerDto(id = 42, title = "Reciclaje", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null,
            docenteLider = null, cuposMaximos = 20))
        assertEquals("Reciclaje", repository.cachedTaller(42)?.title)
        assertNull(repository.cachedTaller(404))
    }

    @Test fun guestNameDefaultsToEmptyUntilSet() = runBlocking {
        assertEquals("", repository.guestName())
        repository.joinGuest(1, "  Maria  ")
        assertEquals("Maria", repository.guestName())
    }
}
