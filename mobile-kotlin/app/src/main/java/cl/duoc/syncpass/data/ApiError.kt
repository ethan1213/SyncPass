package cl.duoc.syncpass.data

import org.json.JSONObject
import retrofit2.HttpException

/**
 * Traduce cualquier fallo a un mensaje que el usuario pueda leer en terreno.
 *
 * Vive en la capa de datos porque conoce el formato de error de la API
 * ({"code":...,"message":...}); las pantallas solo muestran el texto.
 */
fun mensajeDeError(error: Throwable): String = when (error) {
    is OfflineException -> error.message ?: "Sin conexión."
    is IllegalStateException -> error.message ?: "No se pudo completar la acción."
    is HttpException -> mensajeDelCuerpo(error)
        ?: "No se pudo completar la solicitud (${error.code()})."
    else -> "No se pudo conectar con la API. Revisa la conexión e inténtalo otra vez."
}

private fun mensajeDelCuerpo(error: HttpException): String? = runCatching {
    JSONObject(error.response()?.errorBody()?.string().orEmpty()).optString("message")
}.getOrNull()?.takeIf { it.isNotBlank() }
