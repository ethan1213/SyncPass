package cl.duoc.syncpass.inscripcion;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Preinscripcion a un taller: cualquier cuenta puede marcar que planea asistir;
 * solo admin/coordinador ven la lista completa de un taller.
 */
@RestController
@RequestMapping("/api/eventos/{tallerId}")
public class InscripcionController {

    private final InscripcionService service;

    public InscripcionController(InscripcionService service) {
        this.service = service;
    }

    @PostMapping("/inscripcion")
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
    public Inscripcion marcar(@PathVariable Long tallerId, @AuthenticationPrincipal Jwt jwt) {
        return service.marcarPendiente(tallerId, jwt.getSubject(), null);
    }

    @GetMapping("/inscripciones")
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public List<Inscripcion> listar(@PathVariable Long tallerId) {
        return service.listar(tallerId);
    }
}
