package cl.duoc.syncpass.inscripcion;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Talleres que el propio usuario marco como "voy a asistir" y aun no acredita. */
@RestController
@RequestMapping("/api/mis-inscripciones")
public class MisInscripcionesController {

    private final InscripcionService service;

    public MisInscripcionesController(InscripcionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Inscripcion> mias(@AuthenticationPrincipal Jwt jwt) {
        return service.misInscripciones(jwt.getSubject());
    }
}
