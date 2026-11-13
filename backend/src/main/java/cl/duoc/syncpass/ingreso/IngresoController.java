package cl.duoc.syncpass.ingreso;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Entrada al taller desde el QR o el codigo numerico.
 *
 * /invitado es publico a proposito: alguien que llega a la plaza sin cuenta
 * debe poder quedar en la lista. /cuenta exige token y usa la identidad real.
 */
@RestController
@RequestMapping("/api/eventos/{tallerId}/ingreso")
public class IngresoController {

    private final IngresoService service;

    public IngresoController(IngresoService service) {
        this.service = service;
    }

    public record Invitado(@NotNull UUID dispositivoId, @NotBlank @Size(max = 80) String nombre,
                           Double latitud, Double longitud, Float precisionMetros) {}

    public record Ubicacion(Double latitud, Double longitud, Float precisionMetros) {}

    @PostMapping("/invitado")
    public IngresoService.Resultado invitado(@PathVariable Long tallerId, @Valid @RequestBody Invitado input) {
        return service.comoInvitado(tallerId, input.dispositivoId(), input.nombre(),
            input.latitud(), input.longitud(), input.precisionMetros());
    }

    @PostMapping("/cuenta")
    public IngresoService.Resultado cuenta(@PathVariable Long tallerId,
                                           @RequestBody(required = false) Ubicacion input,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.conCuenta(tallerId, jwt.getSubject(),
            input == null ? null : input.latitud(),
            input == null ? null : input.longitud(),
            input == null ? null : input.precisionMetros());
    }
}
