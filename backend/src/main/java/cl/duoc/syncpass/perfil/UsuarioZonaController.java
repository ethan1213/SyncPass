package cl.duoc.syncpass.perfil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Asigna la zona/unidad vecinal de una cuenta. Solo admin la administra. */
@RestController
@RequestMapping("/api/usuarios/{sub}/zona")
public class UsuarioZonaController {

    private final UsuarioZonaRepository repository;

    public UsuarioZonaController(UsuarioZonaRepository repository) {
        this.repository = repository;
    }

    public record ZonaInput(@NotBlank @Size(max = 120) String zona) {}

    @PutMapping
    @PreAuthorize("hasAnyRole('admin')")
    public UsuarioZona asignar(@PathVariable String sub, @Valid @RequestBody ZonaInput input) {
        var fila = repository.findById(sub).orElseGet(() -> {
            var nueva = new UsuarioZona();
            nueva.sub = sub;
            return nueva;
        });
        fila.zona = input.zona();
        return repository.save(fila);
    }
}
