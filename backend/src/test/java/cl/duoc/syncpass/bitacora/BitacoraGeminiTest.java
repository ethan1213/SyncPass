package cl.duoc.syncpass.bitacora;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BitacoraGeminiTest {
    private final HttpClient http = mock(HttpClient.class);
    private final BitacoraGemini gemini =
        new BitacoraGemini(new ObjectMapper(), "clave-ficticia", "gemini-3.5-flash-lite", http);

    @Test
    void leeSoloElTextoFinalYEnviaLaClaveComoCabecera() throws Exception {
        respuesta(200, """
            {"candidates":[{"finishReason":"STOP","content":{"parts":[
              {"thought":true,"text":"pensamiento interno"},{"text":"Airear cada siete días."}
            ]}}]}
            """);

        assertEquals("Airear cada siete días.", gemini.resumir("Se acordó airear el compost cada siete días."));
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any());
        assertEquals("POST", request.getValue().method());
        assertEquals("/v1beta/models/gemini-3.5-flash-lite:generateContent", request.getValue().uri().getPath());
        assertNull(request.getValue().uri().getQuery());
        assertEquals("clave-ficticia", request.getValue().headers().firstValue("x-goog-api-key").orElseThrow());
    }

    @Test
    void aceptaGrabacionWebmYDevuelveLaTranscripcion() throws Exception {
        respuesta(200, """
            {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"El saldo fue de 17000 pesos."}]}}]}
            """);
        var audio = new MockMultipartFile("audio", "taller.webm", "audio/webm", new byte[]{1, 2});
        assertEquals("El saldo fue de 17000 pesos.", gemini.transcribir(audio));
        verify(http).send(any(), any());
    }

    @Test
    void descartaRespuestasTruncadasYCuotasAgotadas() throws Exception {
        respuesta(200, """
            {"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"Fragmento incompleto"}]}}]}
            """);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> gemini.resumir("Apuntes")).getStatusCode());

        respuesta(429, "{}");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
            assertThrows(ResponseStatusException.class, () -> gemini.resumir("Apuntes")).getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private void respuesta(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.<String>send(any(), any())).thenReturn(response);
    }
}
