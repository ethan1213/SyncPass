package cl.duoc.syncpass.data

import cl.duoc.syncpass.data.model.TallerComunitario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los modelos de datos (DTOs de red/Room y payloads) son clases reales usadas
 * por el repositorio; ejercitar copy/equals/hashCode/toString confirma que su
 * forma de dato es la esperada y cubre el codigo que el compilador genera
 * para cada una.
 */
class DataModelsTest {
    @Test fun tallerDtoCopyEqualsAndToString() {
        val original = TallerDto(id = 1, title = "Taller", descripcion = "desc", area = "social",
            unidadVecinal = "12", comuna = "Maipu", direccion = "Calle 1", fechaHora = "2026-01-01",
            docenteLider = "Ana", cuposMaximos = 30, latitud = -33.1, longitud = -70.1,
            radioMetros = 50, inscritos = 5)
        val copia = original.copy(inscritos = 6)
        assertNotEquals(original, copia)
        assertEquals(original, original.copy())
        assertEquals(original.hashCode(), original.copy().hashCode())
        assertTrue(original.toString().contains("Taller"))
    }

    @Test fun pendingRecordDefaultsGenerateUniqueIdsAndTimestamp() {
        val uno = PendingRecord(owner = "a", tipo = "encuesta", tallerId = 1)
        val dos = PendingRecord(owner = "a", tipo = "encuesta", tallerId = 1)
        assertNotEquals(uno.id, dos.id)
        assertEquals(uno, uno.copy())
        assertTrue(uno.toString().contains("encuesta"))
    }

    @Test fun pendingEntryDefaultsGenerateUniqueIds() {
        val uno = PendingEntry(tallerId = 2, modo = "invitado", dispositivoId = "dev-1")
        val dos = PendingEntry(tallerId = 2, modo = "invitado", dispositivoId = "dev-1")
        assertNotEquals(uno.id, dos.id)
        assertEquals(uno.modo, uno.copy().modo)
    }

    @Test fun networkPayloadsSupportCopyAndEquals() {
        val batch = SyncBatch(listOf(PendingPayload("1", "encuesta", 1, "p", 5, "c", "u", "t",
            null, null, null)))
        assertEquals(batch, batch.copy())
        assertEquals(SyncResult(listOf("1")), SyncResult(listOf("1")))
        assertEquals(LoginRequest("u", "p"), LoginRequest("u", "p"))
        assertEquals(LoginResult("a", "i", "r", 3600), LoginResult("a", "i", "r", 3600))
        assertEquals(RegisterRequest("u", "e", "n", "p"), RegisterRequest("u", "e", "n", "p"))
        assertEquals(RegisterResult(true, "email"), RegisterResult(true, "email"))
        assertEquals(ConfirmRequest("u", "123"), ConfirmRequest("u", "123"))
        assertEquals(UsernameRequest("u"), UsernameRequest("u"))
        assertEquals(BitacoraDto("c", "r", "fecha", listOf("f1")), BitacoraDto("c", "r", "fecha", listOf("f1")))
        assertEquals(PreguntaBitacora("q"), PreguntaBitacora("q"))
        assertEquals(RespuestaBitacora("r", 1, "cita"), RespuestaBitacora("r", 1, "cita"))
        assertEquals(GuestJoinRequest("d", "n"), GuestJoinRequest("d", "n"))
        assertEquals(JoinLocation(1.0, 2.0, 3f), JoinLocation(1.0, 2.0, 3f))
        assertEquals(JoinResult("estado", "id", "p", false), JoinResult("estado", "id", "p", false))
        assertEquals(RecordingStatus(true, false, "msg"), RecordingStatus(true, false, "msg"))
        assertEquals(FlushResult(1, 2, "motivo"), FlushResult(1, 2, "motivo"))
        assertEquals(CatalogSnapshot(emptyList(), true), CatalogSnapshot(emptyList(), true))
        assertEquals(InscripcionDto(1, 2, "pendiente"), InscripcionDto(1, 2, "pendiente"))
    }

    @Test fun tallerDtoCarriesDominioRolesAndZona() {
        val restringido = TallerDto(id = 2, title = "Taller", descripcion = null, area = null,
            unidadVecinal = null, comuna = null, direccion = null, fechaHora = null, docenteLider = null,
            cuposMaximos = 10, rolesPermitidos = "monitor,coordinador", zonaRestringida = "UV 12")
        assertEquals("monitor,coordinador", restringido.rolesPermitidos)
        assertNotEquals(restringido, restringido.copy(zonaRestringida = "UV 13"))
    }

    @Test fun tallerComunitarioModelCopyAndEquals() {
        val taller = TallerComunitario(1, "T", "social", "UV1", "Maipu", "2026-01-01", 10, 2)
        assertTrue(taller.isOfflineAvailable)
        assertEquals(taller, taller.copy())
        assertNotEquals(taller, taller.copy(inscritos = 3))
    }
}
