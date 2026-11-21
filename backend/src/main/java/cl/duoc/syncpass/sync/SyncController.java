package cl.duoc.syncpass.sync;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Punto de entrada del lote offline y del panel de quorum. */
@RestController
@RequestMapping("/api")
public class SyncController {

    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    @PostMapping("/sync/batch")
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
    public Map<String, Object> sync(@Valid @RequestBody SyncService.Lote lote, @AuthenticationPrincipal Jwt jwt) {
        var grupos = Optional.ofNullable(jwt.<String>getClaimAsStringList("cognito:groups")).orElse(List.of());
        return Map.of("acceptedIds", service.recibir(lote, jwt.getSubject(), grupos));
    }

    @GetMapping("/quorum")
    @PreAuthorize("hasAnyRole('admin','coordinador','monitor')")
    public List<Map<String, Object>> quorum() {
        return service.quorum();
    }

    /** Identidad del token: la usan la web y Postman para comprobar el rol. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("sub", jwt.getSubject(),
            "roles", Optional.ofNullable(jwt.getClaimAsStringList("cognito:groups")).orElse(List.of()));
    }
}
