package cl.duoc.syncpass;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
@RestController @RequestMapping("/api/eventos")
public class TallerController {
 private final TallerRepository repository;
 private final JdbcTemplate db;
 public TallerController(TallerRepository repository,JdbcTemplate db) { this.repository=repository;this.db=db; }
 private Taller counted(Taller t){t.inscritos=db.queryForObject("SELECT COUNT(*) FROM registros WHERE taller_id=? AND tipo='asistencia'",Long.class,t.id);return t;}
 @GetMapping public List<Taller> all() { return repository.findAll().stream().map(this::counted).toList(); }
 @GetMapping("/{id}") public Taller get(@PathVariable Long id) { return counted(repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND))); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('admin','coordinador')")
 public Taller create(@Valid @RequestBody Taller input) { input.id=null; input.version=null; return repository.saveAndFlush(input); }
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('admin','coordinador')")
 public Taller update(@PathVariable Long id,@Valid @RequestBody Taller input) {
   Taller old=get(id);
   if(!Objects.equals(old.version,input.version)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El taller cambió; recarga antes de editar");
   input.id=id; return repository.saveAndFlush(input);
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('admin','coordinador')")
 public void delete(@PathVariable Long id) { repository.delete(get(id)); repository.flush(); }
}
