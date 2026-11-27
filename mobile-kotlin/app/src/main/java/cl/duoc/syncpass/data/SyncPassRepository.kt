package cl.duoc.syncpass.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.util.UUID

/** Estado que devuelve la app cuando la asistencia quedo guardada sin conexion. */
const val ESTADO_PENDIENTE = "pendiente_local"

/** Resultado de vaciar la cola local contra la API. */
data class FlushResult(val enviados: Int, val descartados: Int, val motivo: String? = null)

/** Catalogo entregado a la UI junto con su procedencia (nube o Room). */
data class CatalogSnapshot(val rows: List<TallerDto>, val fromCache: Boolean)

/** Se lanza cuando la operacion exige red y el telefono no la tiene. */
class OfflineException(message: String =
    "Sin conexion. El registro queda guardado en el telefono y se enviara al recuperar la senal.")
    : java.io.IOException(message)

/**
 * [injectedApi] e [injectedOnline] son las dos semillas de inyeccion de
 * dependencias del repositorio: en produccion se omiten y se construyen el
 * Retrofit real mediante [crearApiRemota] y el [ConnectivityMonitor]
 * del sistema; las pruebas unitarias pasan un fake de [SyncPassApi] y fuerzan
 * el estado de conexion para ejercitar las ramas en linea (login/registro/
 * catalogo/sync) sin tocar la red ni depender del shadow de Robolectric.
 */
class SyncPassRepository(
    context: Context,
    injectedApi: SyncPassApi? = null,
    private val injectedOnline: (() -> Boolean)? = null,
    databaseName: String = "syncpass.db",
) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE)
    private val syncMutex = Mutex()
    private val entryMutex = Mutex()
    private val db = abrirBaseLocal(context, databaseName)
    private val api = injectedApi ?: crearApiRemota()
    private val connectivity = ConnectivityMonitor(context)
    private fun connected(): Boolean = injectedOnline?.invoke() ?: connectivity.isOnline()

    /** Cambios de conexion: la capa de presentacion reacciona a este flujo. */
    val online: Flow<Boolean> get() = connectivity.online
    fun isOnline(): Boolean = connected()
    private fun requireNetwork() { if (!connected()) throw OfflineException() }

    /** Cognito exige red: sin ella se avisa y queda disponible el modo invitado. */
    private fun requireNetworkForAuth() {
        if (!connected()) throw OfflineException(
            "Sin conexión. Para iniciar sesión necesitas internet; mientras tanto puedes entrar como invitado con el código del taller.")
    }

    suspend fun login(username: String, password: String): LoginResult {
        requireNetworkForAuth()
        return api.login(LoginRequest(username, password))
    }
    suspend fun register(username: String, email: String, nickname: String, password: String): RegisterResult {
        requireNetworkForAuth()
        return api.register(RegisterRequest(username, email, nickname, password))
    }
    suspend fun confirm(username: String, code: String) {
        requireNetworkForAuth()
        api.confirm(ConfirmRequest(username, code))
    }
    suspend fun resend(username: String) {
        requireNetworkForAuth()
        api.resend(UsernameRequest(username))
    }

    suspend fun cachedTalleres(): List<TallerDto> = db.talleres().all()
    suspend fun cachedTaller(id: Long): TallerDto? = db.talleres().all().find { it.id == id }
    fun guestName(): String = preferences.getString("name", "").orEmpty()
    private suspend fun locationFor(tallerId: Long): JoinLocation {
        val geo = cachedTaller(tallerId)?.latitud != null
        if (!geo) return JoinLocation()
        val point = currentLocation(appContext)
            ?: throw IllegalStateException("No se obtuvo ubicación GPS reciente. Activa la ubicación y vuelve a intentar.")
        return JoinLocation(point.latitude, point.longitude, point.accuracy)
    }
    private fun deviceId(): String = preferences.getString("device_id", null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString("device_id", it).apply() }

    /**
     * Asistencia de invitado. Sin conexion no se pierde: el GPS se captura igual
     * (no necesita datos) y la fila queda en Room esperando el enlace.
     */
    suspend fun joinGuest(tallerId: Long, name: String): JoinResult {
        val identifier = deviceId()
        val point = locationFor(tallerId)
        preferences.edit().putString("name", name.trim()).apply()
        if (!connected()) {
            db.ingresos().save(PendingEntry(tallerId = tallerId, modo = "invitado",
                dispositivoId = identifier, nombre = name.trim(),
                latitud = point.latitud, longitud = point.longitud,
                precisionMetros = point.precisionMetros))
            return JoinResult(ESTADO_PENDIENTE, "", "guest:$identifier", true)
        }
        return api.joinGuest(tallerId, GuestJoinRequest(identifier, name.trim(),
            point.latitud, point.longitud, point.precisionMetros))
    }

    /** Igual que [joinGuest], pero con la cuenta ya iniciada. */
    suspend fun joinAccount(tallerId: Long, token: String): JoinResult {
        val point = locationFor(tallerId)
        if (!connected()) {
            db.ingresos().save(PendingEntry(tallerId = tallerId, modo = "cuenta",
                dispositivoId = deviceId(), latitud = point.latitud,
                longitud = point.longitud, precisionMetros = point.precisionMetros))
            return JoinResult(ESTADO_PENDIENTE, "", "", false)
        }
        return api.joinAccount(tallerId, "Bearer $token", point)
    }

    suspend fun pendingEntries(): Int = db.ingresos().count()
    suspend fun pendingEntriesFor(tallerId: Long): Int = db.ingresos().countFor(tallerId)

    /**
     * Reenvia la cola de asistencia. Los invitados no necesitan token; los
     * registros de cuenta esperan a que haya sesion. Un rechazo definitivo del
     * servidor (sin cupos, fuera del radio) se descarta con su motivo, para no
     * reintentar para siempre algo que nunca va a entrar.
     */
    suspend fun flushEntries(token: String?): FlushResult = entryMutex.withLock {
        if (!connected()) return FlushResult(0, 0)
        var enviados = 0
        var descartados = 0
        var motivo: String? = null
        for (row in db.ingresos().all()) {
            try {
                if (row.modo == "invitado") {
                    api.joinGuest(row.tallerId, GuestJoinRequest(row.dispositivoId,
                        row.nombre.orEmpty(), row.latitud, row.longitud, row.precisionMetros))
                } else {
                    if (token == null) continue
                    api.joinAccount(row.tallerId, "Bearer $token",
                        JoinLocation(row.latitud, row.longitud, row.precisionMetros))
                }
                db.ingresos().delete(row.id)
                enviados++
            } catch (e: HttpException) {
                if (e.code() in 400..499 && e.code() !in setOf(401, 408, 429)) {
                    db.ingresos().delete(row.id)
                    descartados++
                    motivo = "Taller #" + row.tallerId + ": " +
                        e.message().ifBlank { "rechazado por el servidor" }
                }
            } catch (_: Exception) {
                // Falla de red: la fila queda para el proximo intento.
            }
        }
        FlushResult(enviados, descartados, motivo)
    }
    suspend fun recordingStatus(tallerId: Long): RecordingStatus {
        requireNetwork()
        return api.recordingStatus(tallerId)
    }

    /**
     * Decide de donde salen los datos: con conexion manda la nube y refresca la
     * copia local; sin conexion responde Room sin golpear la red.
     */
    suspend fun catalog(): CatalogSnapshot {
        if (!connected()) return CatalogSnapshot(db.talleres().all(), fromCache = true)
        return try {
            CatalogSnapshot(talleres(), fromCache = false)
        } catch (_: Exception) {
            CatalogSnapshot(db.talleres().all(), fromCache = true)
        }
    }

    suspend fun talleres(): List<TallerDto> {
        requireNetwork()
        val rows = api.talleres()
        db.withTransaction {
            db.talleres().clear()
            db.talleres().save(rows)
        }
        return rows
    }

    /** true cuando el taller valida la ubicacion antes de aceptar la asistencia. */
    suspend fun exigeGeocerca(tallerId: Long): Boolean = cachedTaller(tallerId)?.latitud != null

    /**
     * Acreditacion por QR del equipo: entra a la cola local con el GPS del
     * momento. Nunca viaja en caliente, porque el escaneo ocurre en terreno y
     * la señal no esta garantizada.
     */
    suspend fun acreditar(owner: String, tallerId: Long, participante: String) {
        val punto = locationFor(tallerId)
        db.pending().save(PendingRecord(owner = owner, tipo = "asistencia", tallerId = tallerId,
            participante = participante, latitud = punto.latitud, longitud = punto.longitud,
            precisionMetros = punto.precisionMetros))
    }

    suspend fun pending(owner: String) = db.pending().forOwner(owner)
    suspend fun bitacora(tallerId: Long, token: String): BitacoraDto {
        requireNetwork()
        return api.bitacora(tallerId, "Bearer $token")
    }
    suspend fun preguntarBitacora(tallerId: Long, token: String, pregunta: String): RespuestaBitacora {
        requireNetwork()
        return api.preguntarBitacora(tallerId, "Bearer $token", PreguntaBitacora(pregunta))
    }
    suspend fun prepararResumen(tallerId: Long, token: String, contenido: String, autorizaProveedor: Boolean): BorradorBitacora {
        requireNetwork()
        return api.prepararResumen(tallerId, "Bearer $token", PeticionResumen(contenido, autorizaProveedor))
    }
    suspend fun publicarBitacora(tallerId: Long, token: String, contenido: String, resumen: String): BitacoraDto {
        requireNetwork()
        return api.publicarBitacora(tallerId, "Bearer $token", PublicacionBitacora(contenido, resumen))
    }
    /** RF12: declara que se planea asistir. Exige red; no se encola offline (es una preferencia, no una acreditacion). */
    suspend fun marcarPendiente(tallerId: Long, token: String): InscripcionDto {
        requireNetwork()
        return api.marcarPendiente(tallerId, "Bearer $token")
    }
    suspend fun misInscripciones(token: String): List<InscripcionDto> {
        requireNetwork()
        return api.misInscripciones("Bearer $token")
    }
    suspend fun queue(row: PendingRecord) = db.pending().save(row)
    suspend fun sync(owner: String, accessToken: String): Int = syncMutex.withLock {
        requireNetwork()
        var total = 0
        val catalogoActual = db.talleres().all().map { it.id }.toSet()
        val enviables = db.pending().forOwner(owner).filter {
            catalogoActual.isEmpty() || it.tallerId in catalogoActual
        }
        suspend fun enviar(batch: List<PendingRecord>) {
            val payload = SyncBatch(batch.map {
                PendingPayload(it.id, it.tipo, it.tallerId, it.participante, it.rating,
                    it.comentario, it.utilidad, it.timestamp, it.latitud, it.longitud, it.precisionMetros)
            })
            val result = try {
                api.sync("Bearer $accessToken", payload)
            } catch (error: HttpException) {
                if (error.code() != 404 && error.code() != 409) throw error
                if (batch.size > 1) {
                    val mitad = batch.size / 2
                    enviar(batch.take(mitad))
                    enviar(batch.drop(mitad))
                }
                return
            }
            val accepted = result.acceptedIds.toSet()
            for (row in batch) if (row.id in accepted) {
                db.pending().delete(row.id, owner)
                total++
            }
        }
        for (batch in enviables.chunked(100)) enviar(batch)
        total
    }
}
