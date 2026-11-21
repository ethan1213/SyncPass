package cl.duoc.syncpass.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas puras usadas antes de enviar credenciales o acreditar un QR. */
class UiStateValidationTest {
    @Test fun loginRequiresBothFieldsAndDoesNotSubmitTwice() {
        assertFalse(LoginUiState().puedeEnviar)
        assertFalse(LoginUiState(username = "ethan").puedeEnviar)
        assertFalse(LoginUiState(password = "secreto").puedeEnviar)
        assertTrue(LoginUiState(username = "ethan", password = "secreto").puedeEnviar)
        assertFalse(LoginUiState(username = "ethan", password = "secreto", busy = true).puedeEnviar)
    }

    @Test fun scannerAcceptsOnlyParticipantQrFormat() {
        assertTrue(ScannerUiState(code = "SP1:123e4567-e89b-12d3-a456-426614174000").codigoValido)
        assertFalse(ScannerUiState(code = "SPT:2").codigoValido)
        assertFalse(ScannerUiState(code = "SP1:123").codigoValido)
        assertFalse(ScannerUiState(code = "123e4567-e89b-12d3-a456-426614174000").codigoValido)
    }
}
