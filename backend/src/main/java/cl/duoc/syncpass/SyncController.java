package cl.duoc.syncpass;
import java.time.Instant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api")
public class SyncController {
 private final JdbcTemplate db;
 public SyncController(JdbcTemplate db) { this.db=db; }
 public record Registro(@NotNull UUID id,@NotBlank @Pattern(regexp="asistencia|encuesta") String tipo,
   @NotNull @Positive Long tallerId,@Size(max=100) String participante,
   @Min(1) @Max(5) Integer rating,@Size(max=2000) String comentario,
   @Size(max=30) String utilidad,@NotNull @PastOrPresent Instant timestamp,
   @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
   @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
   @DecimalMin("0.0") Float precisionMetros) {}
 public record Lote(@NotNull @Size(min=1,max=100) List<@Valid Registro> registros) {}
 @PostMapping("/sync/batch") @Transactional
 @PreAuthorize("hasAnyRole('admin','coordinador','monitor','participante')")
 public Map<String,Object> sync(@Valid @RequestBody Lote lote,@AuthenticationPrincipal Jwt jwt) {
   List<String> groups=Optional.ofNullable(jwt.<String>getClaimAsStringList("cognito:groups")).orElse(List.of());
   boolean staff=groups.stream().anyMatch(Set.of("admin","coordinador","monitor")::contains);
   var accepted=new ArrayList<String>();
   for(var r:lote.registros()) {
     if(r.tipo().equals("asistencia") && !staff) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
     if(r.tipo().equals("encuesta") && r.rating()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta calificación");
     var prior=db.queryForList("SELECT owner FROM registros WHERE id=?",r.id().toString());
     if(!prior.isEmpty()) {
       if(!jwt.getSubject().equals(prior.get(0).get("owner"))) throw new ResponseStatusException(HttpStatus.CONFLICT);
       accepted.add(r.id().toString()); continue;
     }
     if(r.tipo().equals("asistencia") && (r.participante()==null || !r.participante().matches("(?i)SP1:[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")))
       throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"QR SyncPass inválido");
     if(r.tipo().equals("asistencia")) {
       var site=db.queryForList("SELECT latitud,longitud,radio_metros FROM talleres WHERE id=?",r.tallerId());
       if(site.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Taller no encontrado");
       var point=site.get(0);
       if(point.get("latitud")!=null) {
         if(r.latitud()==null || r.longitud()==null || r.precisionMetros()==null)
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere ubicación GPS para este taller");
         double distance=GeoFence.distanceMeters(((Number)point.get("latitud")).doubleValue(),
           ((Number)point.get("longitud")).doubleValue(),r.latitud(),r.longitud());
         double radius=((Number)point.get("radio_metros")).doubleValue();
         if(r.precisionMetros()>100 || distance+r.precisionMetros()>radius)
           throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Fuera del radio del taller o ubicación imprecisa");
       }
     }
     String participant=r.tipo().equals("encuesta") ? jwt.getSubject() : r.participante();
     if(participant==null || participant.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Falta participante");
     db.update("INSERT INTO registros(id,owner,tipo,taller_id,participante,rating,comentario,utilidad,registrado_en,recibido_en,latitud,longitud,precision_metros) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
       r.id().toString(),jwt.getSubject(),r.tipo(),r.tallerId(),participant,r.rating(),r.comentario(),r.utilidad(),java.sql.Timestamp.from(r.timestamp()),java.sql.Timestamp.from(Instant.now()),r.latitud(),r.longitud(),r.precisionMetros());
     accepted.add(r.id().toString());
   }
   return Map.of("acceptedIds",accepted);
 }
 @GetMapping("/quorum") @PreAuthorize("hasAnyRole('admin','coordinador','monitor')")
 public List<Map<String,Object>> quorum() {
   return db.queryForList("SELECT t.id,COUNT(r.id) AS asistentes FROM talleres t LEFT JOIN registros r ON r.taller_id=t.id AND r.tipo='asistencia' GROUP BY t.id ORDER BY t.id");
 }
 @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt) {
   return Map.of("sub",jwt.getSubject(),"roles",Optional.ofNullable(jwt.getClaimAsStringList("cognito:groups")).orElse(List.of()));
 }
}
