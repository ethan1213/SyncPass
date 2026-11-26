package cl.duoc.syncpass.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Response

/**
 * mensajeDeError traduce cada tipo de falla al texto que ve la persona usuaria.
 *
 * Se corre con Robolectric porque org.json.JSONObject (usado para leer el
 * cuerpo de un HttpException) esta "no mockeado" en un JUnit puro sin
 * shadows de Android.
 */
@RunWith(RobolectricTestRunner::class)
class ApiErrorTest {
    @Test fun offlineExceptionKeepsItsOwnMessage() {
        assertTrue(mensajeDeError(OfflineException()).contains("Sin conexion"))
        assertEquals("mensaje custom", mensajeDeError(OfflineException("mensaje custom")))
    }

    @Test fun illegalStateUsesItsMessageOrFallback() {
        assertEquals("ubicacion invalida", mensajeDeError(IllegalStateException("ubicacion invalida")))
        assertEquals("No se pudo completar la acción.", mensajeDeError(IllegalStateException()))
    }

    @Test fun httpExceptionReadsMessageFromJsonBody() {
        val body = """{"message":"No hay cupos disponibles"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        val error = HttpException(Response.error<Any>(409, body))
        assertEquals("No hay cupos disponibles", mensajeDeError(error))
    }

    @Test fun httpExceptionFallsBackToCodeWhenBodyHasNoMessage() {
        val body = "".toResponseBody("application/json".toMediaTypeOrNull())
        val error = HttpException(Response.error<Any>(500, body))
        assertEquals("No se pudo completar la solicitud (500).", mensajeDeError(error))
    }

    @Test fun httpExceptionFallsBackWhenBodyIsNotJson() {
        val body = "oops".toResponseBody("text/plain".toMediaTypeOrNull())
        val error = HttpException(Response.error<Any>(400, body))
        assertEquals("No se pudo completar la solicitud (400).", mensajeDeError(error))
    }

    @Test fun conflictWithoutBodyHasReadableMessage() {
        val body = "".toResponseBody("application/json".toMediaTypeOrNull())
        val error = HttpException(Response.error<Any>(409, body))
        assertEquals("Este registro ya existe o fue modificado. Actualiza y vuelve a intentarlo.",
            mensajeDeError(error))
    }

    @Test fun anyOtherThrowableGetsGenericNetworkMessage() {
        assertEquals("No se pudo conectar con la API. Revisa la conexión e inténtalo otra vez.",
            mensajeDeError(RuntimeException("boom")))
        assertEquals("No se pudo conectar con la API. Revisa la conexión e inténtalo otra vez.",
            mensajeDeError(java.io.IOException()))
    }
}
