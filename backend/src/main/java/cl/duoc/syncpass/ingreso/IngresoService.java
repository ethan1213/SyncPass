package cl.duoc.syncpass.ingreso;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import cl.duoc.syncpass.common.GeoFence;
import cl.duoc.syncpass.inscripcion.InscripcionService;
import cl.duoc.syncpass.perfil.UsuarioZonaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Ingreso a un taller: el participante declara su presencia al llegar.
 *
 * Es idempotente por (taller, participante). Esa propiedad es la que permite
 * que el telefono reintente sin miedo una asistencia que tomo sin señal: si ya
 * estaba, responde "ya_presente" en vez de duplicar la fila.
 */
@Service
public class IngresoService {

    private final JdbcTemplate db;
    private final InscripcionService inscripciones;
    private final UsuarioZonaRepository zonas;

    public IngresoService(JdbcTemplate db, InscripcionService inscripciones, UsuarioZonaRepository zonas) {
        this.db = db;
        this.inscripciones = inscripciones;
        this.zonas = zonas;
    }

    public record Resultado(String estado, String registroId, String participante, boolean invitado) {}

    /** Ingreso sin cuenta: el telefono se identifica con un UUID propio. Sin rol, asi que nunca pasa un taller restringido por rol. */
    @Transactional
    public Resultado comoInvitado(Long tallerId, UUID dispositivoId, String nombre,
                                  Double lat, Double lon, Float precision) {
        String participante = "guest:" + dispositivoId;
        return registrar(tallerId, participante, participante, true, nombre.trim(), lat, lon, precision,
            dispositivoId.toString(), null, List.of());
    }

    /** Ingreso con sesion iniciada: el participante es el sub del token. */
    @Transactional
    public Resultado conCuenta(Long tallerId, String sub, Double lat, Double lon, Float precision,
                               List<String> rolesUsuario) {
        return registrar(tallerId, sub, sub, false, null, lat, lon, precision, null, sub, rolesUsuario);
    }

    private Resultado registrar(Long tallerId, String owner, String participante, boolean invitado,
                                String nombreInvitado, Double lat, Double lon, Float precision,
                                String deviceId, String sub, List<String> rolesUsuario) {
        // FOR UPDATE bloquea la fila del taller: dos personas no pueden tomar el ultimo cupo a la vez.
        var filas = db.queryForList(
            "SELECT cupos_maximos,latitud,longitud,radio_metros,roles_permitidos,zona_restringida " +
            "FROM talleres WHERE id=? FOR UPDATE", tallerId);
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
        var sitio = filas.get(0);

        verificarDominio(sitio, sub, rolesUsuario);

        var yaEstaba = db.queryForList(
            "SELECT id FROM registros WHERE tipo='asistencia' AND taller_id=? AND participante=?",
            tallerId, participante);
        if (!yaEstaba.isEmpty()) {
            inscripciones.marcarAsistio(tallerId, sub, deviceId);
            return new Resultado("ya_presente", yaEstaba.get(0).get("id").toString(), participante, invitado);
        }

        int acreditados = contarAsistencias(tallerId);
        if (acreditados >= ((Number) sitio.get("cupos_maximos")).intValue())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No quedan cupos en este taller");

        verificarUbicacion(sitio, lat, lon, precision);

        String id = UUID.randomUUID().toString();
        var ahora = Timestamp.from(Instant.now());
        db.update("""
                  INSERT INTO registros(id,owner,tipo,taller_id,participante,nombre_invitado,
                                        registrado_en,recibido_en,latitud,longitud,precision_metros)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?)
                  """,
            id, owner, "asistencia", tallerId, participante, nombreInvitado, ahora, ahora, lat, lon, precision);
        inscripciones.marcarAsistio(tallerId, sub, deviceId);
        return new Resultado("presente", id, participante, invitado);
    }

    private int contarAsistencias(Long tallerId) {
        Integer total = db.queryForObject(
            "SELECT COUNT(*) FROM registros WHERE tipo='asistencia' AND taller_id=?", Integer.class, tallerId);
        return total == null ? 0 : total;
    }

    /**
     * Dominio del taller: si exige rol, el usuario debe tener al menos uno de los
     * permitidos (un invitado no tiene ninguno, asi que queda excluido siempre).
     * Si exige zona, la cuenta debe tener esa misma zona declarada en usuario_zona
     * (un invitado tampoco tiene fila ahi, asi que tambien queda excluido).
     */
    private void verificarDominio(java.util.Map<String, Object> sitio, String sub, List<String> rolesUsuario) {
        String rolesPermitidos = (String) sitio.get("roles_permitidos");
        if (rolesPermitidos != null && !rolesPermitidos.isBlank()) {
            var permitidos = java.util.Arrays.stream(rolesPermitidos.split(",")).map(String::trim).toList();
            boolean ok = rolesUsuario.stream().anyMatch(permitidos::contains);
            if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Este taller es solo para: " + rolesPermitidos);
        }
        String zonaRestringida = (String) sitio.get("zona_restringida");
        if (zonaRestringida != null && !zonaRestringida.isBlank()) {
            String zonaUsuario = sub == null ? null : zonas.findById(sub).map(z -> z.zona).orElse(null);
            if (zonaUsuario == null || !zonaUsuario.equalsIgnoreCase(zonaRestringida))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este taller es solo para la zona: " + zonaRestringida);
        }
    }

    /** Traduce el veredicto de la geocerca al codigo HTTP que espera el cliente. */
    private void verificarUbicacion(java.util.Map<String, Object> sitio, Double lat, Double lon, Float precision) {
        var sitioLat = sitio.get("latitud") == null ? null : ((Number) sitio.get("latitud")).doubleValue();
        var sitioLon = sitio.get("longitud") == null ? null : ((Number) sitio.get("longitud")).doubleValue();
        var radio = sitio.get("radio_metros") == null ? null : ((Number) sitio.get("radio_metros")).doubleValue();
        switch (GeoFence.evaluar(sitioLat, sitioLon, radio, lat, lon, precision)) {
            case FALTA_UBICACION -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Se requiere ubicación GPS para este taller");
            case FUERA_DE_RADIO -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Fuera del radio del taller o ubicación imprecisa");
            case ACEPTADA -> { }
        }
    }
}
