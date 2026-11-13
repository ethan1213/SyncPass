package cl.duoc.syncpass.taller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Mantenedor de talleres (CRUD completo).
 *
 * Lectura publica: la portada y el acceso por codigo o QR no exigen cuenta.
 * Escritura reservada a admin y coordinador.
 */
@RestController
@RequestMapping("/api/eventos")
public class TallerController {

    private final TallerService service;

    public TallerController(TallerService service) {
        this.service = service;
    }

    /** GET /api/eventos?comuna=&area=&proyectoId=&q= — todos los filtros son opcionales. */
    @GetMapping
    public List<Taller> listar(@RequestParam(required = false) Long proyectoId,
                               @RequestParam(required = false) String comuna,
                               @RequestParam(required = false) String area,
                               @RequestParam(required = false, name = "q") String texto) {
        return service.buscar(proyectoId, comuna, area, texto);
    }

    @GetMapping("/{id}")
    public Taller obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public Taller crear(@Valid @RequestBody Taller input) {
        return service.crear(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public Taller actualizar(@PathVariable Long id, @Valid @RequestBody Taller input) {
        return service.actualizar(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('admin','coordinador')")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
