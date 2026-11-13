package cl.duoc.syncpass.bitacora;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Bitacora del taller: leerla exige cuenta; publicarla o borrarla, rol de gestion. */
@RestController
@RequestMapping("/api/eventos/{tallerId}/bitacora")
public class BitacoraController {

    private final BitacoraService service;

    public BitacoraController(BitacoraService service) {
        this.service = service;
    }

    public record Entrada(@NotBlank @Size(max = 20000) String contenido) {}

    public record Pregunta(@NotBlank @Size(max = 300) String pregunta) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
    public BitacoraService.Bitacora obtener(@PathVariable Long tallerId) {
        return service.obtener(tallerId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public BitacoraService.Bitacora publicar(@PathVariable Long tallerId, @Valid @RequestBody Entrada input) {
        return service.publicar(tallerId, input.contenido());
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
