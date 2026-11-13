package cl.duoc.syncpass.sync;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import cl.duoc.syncpass.common.GeoFence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recibe el lote que el telefono acumulo sin señal.
 *
 * El identificador de cada registro lo genera el telefono (UUID) y es la clave
 * primaria en la base: por eso reenviar el mismo lote no duplica nada, que es
 * justo lo que ocurre cuando la conexion se corta a mitad del envio.
 */
@Service
public class SyncService {

    private static final Set<String> GESTION = Set.of("admin", "coordinador", "monitor");
    private static final String QR_PARTICIPANTE = "(?i)SP1:[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

    private final JdbcTemplate db;

    public SyncService(JdbcTemplate db) {
        this.db = db;
    }

    public record Registro(@NotNull UUID id,
                           @NotBlank @Pattern(regexp = "asistencia|encuesta") String tipo,
                           @NotNull @Positive Long tallerId,
                           @Size(max = 100) String participante,
                           @Min(1) @Max(5) Integer rating,
                           @Size(max = 2000) String comentario,
                           @Size(max = 30) String utilidad,
                           @NotNull @PastOrPresent Instant timestamp,
                           @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
                           @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
                           @DecimalMin("0.0") Float precisionMetros) {}

    public record Lote(@NotNull @Size(min = 1, max = 100) List<@Valid Registro> registros) {}

    /**
     * @param sub    identidad del token: queda como dueño del registro.
     * @param grupos grupos de Cognito del token.
     * @return ids aceptados, para que el telefono borre esas filas de su cola.
     */
    @Transactional
    public List<String> recibir(Lote lote, String sub, List<String> grupos) {
        boolean esGestion = grupos.stream().anyMatch(GESTION::contains);
        var aceptados = new ArrayList<String>();

        for (var registro : lote.registros()) {
            if (registro.tipo().equals("asistencia") && !esGestion)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el equipo puede acreditar asistencia por QR");

            if (registro.tipo().equals("encuesta") && registro.rating() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta calificación");

            if (yaRecibido(registro, sub)) {
                aceptados.add(registro.id().toString());
                continue;
            }

            if (registro.tipo().equals("asistencia")) validarAsistencia(registro);

            String participante = registro.tipo().equals("encuesta") ? sub : registro.participante();
            if (participante == null || participante.isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta participante");

            insertar(registro, sub, participante);
            aceptados.add(registro.id().toString());
        }
        return aceptados;
    }

    /** Reenvio del mismo registro: se acepta si es del mismo dueño, se rechaza si no. */
    private boolean yaRecibido(Registro registro, String sub) {
        var previo = db.queryForList("SELECT owner FROM registros WHERE id=?", registro.id().toString());
        if (previo.isEmpty()) return false;
        if (!sub.equals(previo.get(0).get("owner")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese registro pertenece a otra cuenta");
        return true;
    }

    private void validarAsistencia(Registro registro) {
        if (registro.participante() == null || !registro.participante().matches(QR_PARTICIPANTE))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR SyncPass inválido");

        var sitios = db.queryForList(
            "SELECT latitud,longitud,radio_metros FROM talleres WHERE id=?", registro.tallerId());
        if (sitios.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");

        var sitio = sitios.get(0);
        var lat = sitio.get("latitud") == null ? null : ((Number) sitio.get("latitud")).doubleValue();
        var lon = sitio.get("longitud") == null ? null : ((Number) sitio.get("longitud")).doubleValue();
        var radio = sitio.get("radio_metros") == null ? null : ((Number) sitio.get("radio_metros")).doubleValue();

        switch (GeoFence.evaluar(lat, lon, radio,
                registro.latitud(), registro.longitud(), registro.precisionMetros())) {
            case FALTA_UBICACION -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Se requiere ubicación GPS para este taller");
            case FUERA_DE_RADIO -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Fuera del radio del taller o ubicación imprecisa");
            case ACEPTADA -> { }
        }
    }

    private void insertar(Registro r, String owner, String participante) {
        db.update("""
                  INSERT INTO registros(id,owner,tipo,taller_id,participante,rating,comentario,utilidad,
                                        registrado_en,recibido_en,latitud,longitud,precision_metros)
                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                  """,
            r.id().toString(), owner, r.tipo(), r.tallerId(), participante, r.rating(), r.comentario(),
            r.utilidad(), Timestamp.from(r.timestamp()), Timestamp.from(Instant.now()),
            r.latitud(), r.longitud(), r.precisionMetros());
    }

    /** Asistentes por taller para el panel de quorum en vivo. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> quorum() {
        return db.queryForList("""
                               SELECT t.id, COUNT(r.id) AS asistentes
                                 FROM talleres t
                                 LEFT JOIN registros r ON r.taller_id = t.id AND r.tipo = 'asistencia'
                                GROUP BY t.id
                                ORDER BY t.id
                               """);
    }
}
