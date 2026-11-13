package cl.duoc.syncpass.grabacion;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Estado de grabacion de la sesion.
 *
 * Quien dicta el taller marca si se grabo; el resumen aparece cuando se publica
 * la bitacora. Son dos hechos distintos y por eso se informan por separado.
 */
@Service
public class GrabacionService {

    private final JdbcTemplate db;

    public GrabacionService(JdbcTemplate db) {
        this.db = db;
    }

    public record Estado(boolean grabado, boolean resumenDisponible, String mensaje) {}

    @Transactional(readOnly = true)
    public Estado obtener(Long tallerId) {
        exigirTaller(tallerId);
        boolean grabado = Boolean.TRUE.equals(
            db.query("SELECT grabado FROM taller_grabaciones WHERE taller_id=?",
                (rs, fila) -> rs.getBoolean(1), tallerId).stream().findFirst().orElse(false));
        boolean resumenListo = contar("SELECT COUNT(*) FROM taller_bitacoras WHERE taller_id=?", tallerId) > 0;
        return new Estado(grabado, resumenListo, mensaje(grabado, resumenListo));
    }

    @Transactional
    public Estado declarar(Long tallerId, boolean grabado) {
        exigirTaller(tallerId);
        var ahora = OffsetDateTime.now();
        int actualizadas = db.update(
            "UPDATE taller_grabaciones SET grabado=?,actualizado_en=? WHERE taller_id=?", grabado, ahora, tallerId);
        if (actualizadas == 0)
            db.update("INSERT INTO taller_grabaciones(taller_id,grabado,actualizado_en) VALUES(?,?,?)",
                tallerId, grabado, ahora);
        return obtener(tallerId);
    }

    private static String mensaje(boolean grabado, boolean resumenListo) {
        if (resumenListo) return "El resumen ya está disponible en la bitácora.";
        return grabado
            ? "La sesión se grabó. El resumen aparecerá cuando se publique la transcripción."
            : "El equipo aún no ha publicado un resumen de esta sesión.";
    }

    private void exigirTaller(Long tallerId) {
        if (contar("SELECT COUNT(*) FROM talleres WHERE id=?", tallerId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
    }

    private int contar(String sql, Long tallerId) {
        Integer total = db.queryForObject(sql, Integer.class, tallerId);
        return total == null ? 0 : total;
    }
}
