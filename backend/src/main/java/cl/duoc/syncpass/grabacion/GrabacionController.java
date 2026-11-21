package cl.duoc.syncpass.grabacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Aviso de grabacion y resumen. La lectura es publica: el invitado la ve al entrar con el QR. */
@RestController
@RequestMapping("/api/eventos/{tallerId}/grabacion")
public class GrabacionController {

    private final GrabacionService service;

    public GrabacionController(GrabacionService service) {
        this.service = service;
    }

    public record Decision(@NotNull Boolean grabado) {}

    @GetMapping
    public GrabacionService.Estado obtener(@PathVariable Long tallerId) {
        return service.obtener(tallerId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public GrabacionService.Estado declarar(@PathVariable Long tallerId, @Valid @RequestBody Decision input) {
        return service.declarar(tallerId, input.grabado());
    }
}
