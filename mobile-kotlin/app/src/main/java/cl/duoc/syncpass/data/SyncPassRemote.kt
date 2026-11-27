package cl.duoc.syncpass.data

import cl.duoc.syncpass.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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
/** RF12: preinscripcion a un taller ("pendiente" hasta que una acreditacion real la pasa a "asistio"). */
data class InscripcionDto(val id: Long, val tallerId: Long, val estado: String)

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
    @POST("api/eventos/{tallerId}/inscripcion") suspend fun marcarPendiente(
        @Path("tallerId") tallerId: Long, @Header("Authorization") authorization: String): InscripcionDto
    @GET("api/mis-inscripciones") suspend fun misInscripciones(
        @Header("Authorization") authorization: String): List<InscripcionDto>
}

internal fun crearApiRemota(): SyncPassApi = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .client(OkHttpClient.Builder().build())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(SyncPassApi::class.java)
