package cl.duoc.syncpass;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eventos/{tallerId}/bitacora")
public class BitacoraController {
 private final JdbcTemplate db;
 public BitacoraController(JdbcTemplate db) { this.db=db; }

 public record Entrada(@NotBlank @Size(max=20000) String contenido) {}
 public record Pregunta(@NotBlank @Size(max=300) String pregunta) {}
 public record Bitacora(String contenido,String resumen,String actualizadoEn,List<String> fragmentos) {}
 public record Respuesta(String respuesta,int fragmento,String cita) {}

 @GetMapping
 @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
 public Bitacora get(@PathVariable Long tallerId) {
  return db.query("SELECT contenido,resumen,actualizado_en FROM taller_bitacoras WHERE taller_id=?",
   (rs,row)->new Bitacora(rs.getString(1),rs.getString(2),rs.getObject(3).toString(),fragmentos(rs.getString(1))),tallerId)
   .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Bitácora no publicada"));
 }

 @PutMapping
 @PreAuthorize("hasAnyRole('admin','coordinador')")
 public Bitacora save(@PathVariable Long tallerId,@Valid @RequestBody Entrada input) {
  if(db.queryForObject("SELECT COUNT(*) FROM talleres WHERE id=?",Integer.class,tallerId)==0)
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Taller no encontrado");
  var content=input.contenido().trim();
  var parts=fragmentos(content);
  var summary=resumir(parts);
  if(summary.length()>3000) summary=summary.substring(0,3000);
  var now=OffsetDateTime.now();
  int updated=db.update("UPDATE taller_bitacoras SET contenido=?,resumen=?,actualizado_en=? WHERE taller_id=?",content,summary,now,tallerId);
  if(updated==0) db.update("INSERT INTO taller_bitacoras(taller_id,contenido,resumen,actualizado_en) VALUES (?,?,?,?)",tallerId,content,summary,now);
  return get(tallerId);
 }

 @PostMapping("/preguntas")
 @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
 public Respuesta ask(@PathVariable Long tallerId,@Valid @RequestBody Pregunta input) {
  var parts=get(tallerId).fragmentos();
  var terms=terms(input.pregunta());
  int best=-1,score=0;
  for(int i=0;i<parts.size();i++) {
   var tokens=terms(parts.get(i));
   int current=(int)terms.stream().filter(tokens::contains).count();
   if(current>score) {score=current;best=i;}
  }
  if(best<0) return new Respuesta("No encontré información suficiente en la bitácora para responder.",0,"");
  return new Respuesta(parts.get(best),best+1,parts.get(best));
 }

 static List<String> fragmentos(String content) {
  return Arrays.stream(content.split("(?<=[.!?])\\s+|\\R+"))
   .map(String::trim).filter(s->!s.isBlank()).limit(100).toList();
 }
 static Set<String> terms(String text) {
  var common=Set.of("para","como","donde","cuando","sobre","este","esta","estos","estas","taller","clase","que","con","los","las","una","uno","del","por","porque","cual","cuales","se","de","el","la","en","un","y","a");
  return Arrays.stream(java.text.Normalizer.normalize(text.toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD)
    .replaceAll("\\p{M}","").split("[^a-z0-9]+"))
   .filter(s->s.length()>2 && !common.contains(s)).collect(Collectors.toSet());
 }
 static String resumir(List<String> parts) {
  var frequencies=new HashMap<String,Integer>();
  parts.forEach(part->terms(part).forEach(term->frequencies.merge(term,1,Integer::sum)));
  return java.util.stream.IntStream.range(0,parts.size()).boxed()
   .sorted(Comparator.<Integer>comparingInt(i->terms(parts.get(i)).stream()
    .mapToInt(term->frequencies.getOrDefault(term,0)).sum()).reversed())
   .limit(3).sorted().map(parts::get).collect(Collectors.joining(" "));
 }
}
