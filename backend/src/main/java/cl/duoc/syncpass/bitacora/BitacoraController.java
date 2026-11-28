package cl.duoc.syncpass.bitacora;

import java.io.IOException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Bitacora del taller: leerla exige cuenta; publicarla o borrarla, rol de gestion. */
@RestController
@RequestMapping("/api/eventos/{tallerId}/bitacora")
public class BitacoraController {

    private final BitacoraService service;
    private final BitacoraGemini gemini;

    public BitacoraController(BitacoraService service, BitacoraGemini gemini) {
        this.service = service;
        this.gemini = gemini;
    }

    public record Entrada(@NotBlank @Size(max = 20000) String contenido,
                          @Size(max = 3000) String resumenRevisado) {}
    public record Borrador(@NotBlank @Size(max = 20000) String contenido, boolean autorizaProveedor) {}
    public record Resumen(String resumen, String origen) {}
    public record Transcripcion(String contenido) {}

    public record Pregunta(@NotBlank @Size(max = 300) String pregunta) {}

    @PutMapping(value = "/archivo", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public void guardarArchivo(@PathVariable long tallerId, @RequestParam MultipartFile archivo) {
        String formato = BitacoraGemini.formatoAdmitido(archivo);
        try {
            service.guardarArchivo(tallerId, new BitacoraService.Archivo(formato, archivo.getBytes()));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo.");
        }
    }

    @GetMapping("/archivo")
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public BitacoraService.Archivo archivo(@PathVariable long tallerId) {
        return service.archivo(tallerId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
    public BitacoraService.Bitacora obtener(@PathVariable Long tallerId) {
        return service.obtener(tallerId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public BitacoraService.Bitacora publicar(@PathVariable Long tallerId, @Valid @RequestBody Entrada input) {
        return service.publicar(tallerId, input.contenido(), input.resumenRevisado());
    }

    @PostMapping("/borrador")
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public Resumen borrador(@PathVariable Long tallerId, @Valid @RequestBody Borrador input) {
        service.exigirTaller(tallerId);
        if (input.autorizaProveedor() && gemini.disponible()) {
            try {
                return new Resumen(gemini.resumir(input.contenido()), "gemini");
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode() != HttpStatus.SERVICE_UNAVAILABLE) throw ex;
            }
        }
        return new Resumen(BitacoraService.resumenExtractivo(input.contenido()), "extractivo");
    }

    @PostMapping(value = "/transcripcion", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public Transcripcion transcribir(@PathVariable Long tallerId, @RequestParam MultipartFile audio,
                                      @RequestParam(defaultValue = "false") boolean autorizaProveedor) {
        if (!autorizaProveedor)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirma el permiso para enviar el audio.");
        service.exigirTaller(tallerId);
        return new Transcripcion(gemini.transcribir(audio));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public void eliminar(@PathVariable Long tallerId) {
        service.eliminar(tallerId);
    }

    @PostMapping("/preguntas")
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
    public BitacoraService.Respuesta preguntar(@PathVariable Long tallerId, @Valid @RequestBody Pregunta input) {
        return service.responder(tallerId, input.pregunta());
    }
}
