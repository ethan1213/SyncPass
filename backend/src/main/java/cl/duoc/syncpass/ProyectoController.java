package cl.duoc.syncpass;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/proyectos")
@PreAuthorize("hasAnyRole('admin','coordinador')")
public class ProyectoController {
 private final ProyectoRepository repository;
 public ProyectoController(ProyectoRepository repository) { this.repository=repository; }
 @GetMapping public List<Proyecto> all() { return repository.findAll(); }
 @GetMapping("/{id}") public Proyecto get(@PathVariable Long id) {
  return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public Proyecto create(@Valid @RequestBody Proyecto input) {
  input.id=null;input.version=null;return repository.saveAndFlush(input);
 }
 @PutMapping("/{id}") public Proyecto update(@PathVariable Long id,@Valid @RequestBody Proyecto input) {
  var old=get(id);
  if(!Objects.equals(old.version,input.version)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El proyecto cambió; recarga antes de editar");
  input.id=id;return repository.saveAndFlush(input);
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable Long id) { repository.delete(get(id));repository.flush(); }
}
