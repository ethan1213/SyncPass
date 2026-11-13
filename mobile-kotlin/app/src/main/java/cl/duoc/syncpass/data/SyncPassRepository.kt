package cl.duoc.syncpass.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.syncpass.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.Instant
import java.util.UUID

@Entity(tableName = "talleres")
data class TallerDto(
    @PrimaryKey val id: Long, val title: String, val descripcion: String?, val area: String?,
    val unidadVecinal: String?, val comuna: String?, val direccion: String?,
    val fechaHora: String?, val docenteLider: String?, val cuposMaximos: Int,
    val latitud: Double? = null, val longitud: Double? = null, val radioMetros: Int? = null,
    val inscritos: Int = 0
)

@Entity(tableName = "pending")
data class PendingRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val owner: String, val tipo: String, val tallerId: Long,
    val participante: String? = null, val rating: Int? = null,
    val comentario: String? = null, val utilidad: String? = null,
    val latitud: Double? = null, val longitud: Double? = null, val precisionMetros: Float? = null,
    val timestamp: String = Instant.now().toString()
)

@Dao interface PendingDao {
    @Query("SELECT * FROM pending WHERE owner = :owner ORDER BY timestamp")
    suspend fun forOwner(owner: String): List<PendingRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: PendingRecord)
    @Query("DELETE FROM pending WHERE id = :id AND owner = :owner")
    suspend fun delete(id: String, owner: String)
}

/**
 * Asistencia tomada sin conexion. Se guarda tal cual en el telefono (con el GPS
 * del momento) y se reenvia a /ingreso cuando vuelve la senal; el servidor es
 * idempotente por taller+participante, asi que reintentar no duplica nada.
 */
@Entity(tableName = "ingresos")
data class PendingEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tallerId: Long, val modo: String, val dispositivoId: String,
    val nombre: String? = null,
    val latitud: Double? = null, val longitud: Double? = null, val precisionMetros: Float? = null,
    val timestamp: String = Instant.now().toString()
)

@Dao interface IngresoDao {
    @Query("SELECT * FROM ingresos ORDER BY timestamp")
    suspend fun all(): List<PendingEntry>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(row: PendingEntry)
    @Query("DELETE FROM ingresos WHERE id = :id")
    suspend fun delete(id: String)
    @Query("SELECT COUNT(*) FROM ingresos")
    suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM ingresos WHERE tallerId = :tallerId")
    suspend fun countFor(tallerId: Long): Int
}

@Dao interface TallerDao {
    @Query("SELECT * FROM talleres ORDER BY fechaHora")
    suspend fun all(): List<TallerDto>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(rows: List<TallerDto>)
    @Query("DELETE FROM talleres")
    suspend fun clear()
}

@Database(entities = [PendingRecord::class, TallerDto::class, PendingEntry::class],
    version = 4, exportSchema = false)
abstract class SyncPassDb : RoomDatabase() {
    abstract fun pending(): PendingDao
    abstract fun talleres(): TallerDao
    abstract fun ingresos(): IngresoDao
}

private val catalogMigration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `talleres` (
            `id` INTEGER NOT NULL PRIMARY KEY, `title` TEXT NOT NULL,
            `descripcion` TEXT, `area` TEXT, `unidadVecinal` TEXT, `comuna` TEXT,
            `direccion` TEXT, `fechaHora` TEXT, `docenteLider` TEXT,
            `cuposMaximos` INTEGER NOT NULL, `inscritos` INTEGER NOT NULL)""")
    }
}
private val entryQueueMigration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ingresos (
              id TEXT NOT NULL PRIMARY KEY,
              tallerId INTEGER NOT NULL,
              modo TEXT NOT NULL,
              dispositivoId TEXT NOT NULL,
              nombre TEXT,
              latitud REAL,
              longitud REAL,
              precisionMetros REAL,
              timestamp TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val geoMigration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE talleres ADD COLUMN latitud REAL")
        db.execSQL("ALTER TABLE talleres ADD COLUMN longitud REAL")
        db.execSQL("ALTER TABLE talleres ADD COLUMN radioMetros INTEGER")
        db.execSQL("ALTER TABLE pending ADD COLUMN latitud REAL")
        db.execSQL("ALTER TABLE pending ADD COLUMN longitud REAL")
        db.execSQL("ALTER TABLE pending ADD COLUMN precisionMetros REAL")
    }
}

data class SyncBatch(val registros: List<PendingPayload>)
data class PendingPayload(val id: String, val tipo: String, val tallerId: Long,
    val participante: String?, val rating: Int?, val comentario: String?,
    val utilidad: String?, val timestamp: String,
    val latitud: Double?, val longitud: Double?, val precisionMetros: Float?)
data class SyncResult(val acceptedIds: List<String>)
data class LoginRequest(val username: String, val password: String)
data class LoginResult(val accessToken: String, val idToken: String, val refreshToken: String, val expiresIn: Int)
data class RegisterRequest(val username: String, val email: String, val nickname: String, val password: String)
data class RegisterResult(val userConfirmed: Boolean, val destination: String)
data class ConfirmRequest(val username: String, val code: String)
data class UsernameRequest(val username: String)
data class BitacoraDto(val contenido: String, val resumen: String, val actualizadoEn: String, val fragmentos: List<String>)
data class PreguntaBitacora(val pregunta: String)
data class RespuestaBitacora(val respuesta: String, val fragmento: Int, val cita: String)
data class GuestJoinRequest(val dispositivoId: String, val nombre: String,
    val latitud: Double? = null, val longitud: Double? = null, val precisionMetros: Float? = null)
data class JoinLocation(val latitud: Double? = null, val longitud: Double? = null,
    val precisionMetros: Float? = null)
data class JoinResult(val estado: String, val registroId: String, val participante: String, val invitado: Boolean)
data class RecordingStatus(val grabado: Boolean, val resumenDisponible: Boolean, val mensaje: String)

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

interface SyncPassApi {
    @POST("api/auth/login") suspend fun login(@Body request: LoginRequest): LoginResult
    @POST("api/auth/register") suspend fun register(@Body request: RegisterRequest): RegisterResult
    @POST("api/auth/confirm") suspend fun confirm(@Body request: ConfirmRequest)
    @POST("api/auth/resend") suspend fun resend(@Body request: UsernameRequest)
    @GET("api/eventos") suspend fun talleres(): List<TallerDto>
    @POST("api/eventos/{tallerId}/ingreso/invitado") suspend fun joinGuest(
        @Path("tallerId") tallerId: Long, @Body request: GuestJoinRequest): JoinResult
    @GET("api/eventos/{tallerId}/grabacion") suspend fun recordingStatus(
        @Path("tallerId") tallerId: Long): RecordingStatus
    @POST("api/eventos/{tallerId}/ingreso/cuenta") suspend fun joinAccount(
        @Path("tallerId") tallerId: Long, @Header("Authorization") authorization: String,
        @Body location: JoinLocation): JoinResult
    @POST("api/sync/batch") suspend fun sync(
        @Header("Authorization") authorization: String, @Body batch: SyncBatch): SyncResult
    @GET("api/eventos/{tallerId}/bitacora") suspend fun bitacora(
        @retrofit2.http.Path("tallerId") tallerId: Long,
        @Header("Authorization") authorization: String): BitacoraDto
    @POST("api/eventos/{tallerId}/bitacora/preguntas") suspend fun preguntarBitacora(
        @retrofit2.http.Path("tallerId") tallerId: Long,
        @Header("Authorization") authorization: String,
        @Body pregunta: PreguntaBitacora): RespuestaBitacora
}

class SyncPassRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.applicationContext.getSharedPreferences("syncpass_guest", Context.MODE_PRIVATE)
    private val syncMutex = Mutex()
    private val entryMutex = Mutex()
    private val db = Room.databaseBuilder(context.applicationContext, SyncPassDb::class.java, "syncpass.db")
        .addMigrations(catalogMigration, geoMigration, entryQueueMigration).build()
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().build()).addConverterFactory(GsonConverterFactory.create()).build()
        .create(SyncPassApi::class.java)
    private val connectivity = ConnectivityMonitor(context)

    /** Cambios de conexion: la capa de presentacion reacciona a este flujo. */
    val online: Flow<Boolean> get() = connectivity.online
    fun isOnline(): Boolean = connectivity.isOnline()
    private fun requireNetwork() { if (!connectivity.isOnline()) throw OfflineException() }

    /** Cognito exige red: sin ella se avisa y queda disponible el modo invitado. */
    private fun requireNetworkForAuth() {
        if (!connectivity.isOnline()) throw OfflineException(
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
        if (!connectivity.isOnline()) {
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
        if (!connectivity.isOnline()) {
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
        if (!connectivity.isOnline()) return FlushResult(0, 0)
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
        if (!connectivity.isOnline()) return CatalogSnapshot(db.talleres().all(), fromCache = true)
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
    suspend fun queue(row: PendingRecord) = db.pending().save(row)
    suspend fun sync(owner: String, accessToken: String): Int = syncMutex.withLock {
        requireNetwork()
        var total = 0
        for (batch in db.pending().forOwner(owner).chunked(100)) {
            val result = api.sync("Bearer $accessToken", SyncBatch(batch.map {
                PendingPayload(it.id, it.tipo, it.tallerId, it.participante, it.rating,
                    it.comentario, it.utilidad, it.timestamp, it.latitud, it.longitud, it.precisionMetros)
            }))
            val accepted = result.acceptedIds.toSet()
            for (row in batch) if (row.id in accepted) {
                db.pending().delete(row.id, owner)
                total++
            }
        }
        total
    }
}
