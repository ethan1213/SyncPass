package cl.duoc.syncpass.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** copy/equals/toString de los UiState de cada pantalla (clases de datos reales). */
class UiStateModelsTest {
    @Test fun homeUiStateCopyAndEquals() {
        val estado = HomeUiState(pending = 1, message = "m", offline = true, waiting = true, entries = 2)
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(pending = 2))
        assertEquals(estado.pending, estado.toString().let { estado.pending })
    }

    @Test fun scannerUiStateCopyAndEquals() {
        val estado = ScannerUiState(code = "x", message = "m", exigeGeocerca = true, busy = true)
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(busy = false))
    }

    @Test fun registerUiStateCopyAndEquals() {
        val estado = RegisterUiState(username = "u", email = "e", nickname = "n", password = "p",
            code = "c", esperandoCodigo = true, busy = true, message = "m", error = "err")
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(error = null))
    }

    @Test fun surveyUiStateCopyAndEquals() {
        val estado = SurveyUiState(rating = 4, comment = "c", message = "m", busy = true)
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(rating = 1))
    }

    @Test fun bitacoraUiStateCopyAndEquals() {
        val estado = BitacoraUiState(question = "q", status = "s", busy = true)
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(busy = false))
    }

    @Test fun loginUiStateCopyAndEquals() {
        val estado = LoginUiState(username = "u", password = "p", busy = true, error = "e")
        assertEquals(estado, estado.copy())
        assertNotEquals(estado, estado.copy(error = null))
    }
}
