package cl.duoc.syncpass.bitacora;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Bitacora del taller: resumen publicado y preguntas respondidas con cita. */
@Service
public class BitacoraService {

    private static final int MAX_RESUMEN = 3000;

    private final JdbcTemplate db;

    public BitacoraService(JdbcTemplate db) {
        this.db = db;
    }

    public record Bitacora(String contenido, String resumen, String actualizadoEn, List<String> fragmentos) {}

    public record Respuesta(String respuesta, int fragmento, String cita) {}

    @Transactional(readOnly = true)
    public Bitacora obtener(Long tallerId) {
        return db.query("SELECT contenido,resumen,actualizado_en FROM taller_bitacoras WHERE taller_id=?",
                (rs, fila) -> new Bitacora(rs.getString(1), rs.getString(2), rs.getObject(3).toString(),
                    BitacoraTexto.fragmentos(rs.getString(1))), tallerId)
            .stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no publicada"));
    }

    /** Crea o reemplaza la bitacora y recalcula el resumen a partir del texto. */
    @Transactional
    public Bitacora publicar(Long tallerId, String contenido) {
        exigirTaller(tallerId);
        var texto = contenido.trim();
        var resumen = BitacoraTexto.resumir(BitacoraTexto.fragmentos(texto));
        if (resumen.length() > MAX_RESUMEN) resumen = resumen.substring(0, MAX_RESUMEN);
        var ahora = OffsetDateTime.now();

        int actualizadas = db.update(
            "UPDATE taller_bitacoras SET contenido=?,resumen=?,actualizado_en=? WHERE taller_id=?",
            texto, resumen, ahora, tallerId);
        if (actualizadas == 0)
            db.update("INSERT INTO taller_bitacoras(taller_id,contenido,resumen,actualizado_en) VALUES (?,?,?,?)",
                tallerId, texto, resumen, ahora);
        return obtener(tallerId);
    }

    @Transactional
    public void eliminar(Long tallerId) {
        if (db.update("DELETE FROM taller_bitacoras WHERE taller_id=?", tallerId) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no publicada");
    }

    /** Responde citando el fragmento mas cercano; si no hay coincidencia, lo dice. */
    @Transactional(readOnly = true)
    public Respuesta responder(Long tallerId, String pregunta) {
        var fragmentos = obtener(tallerId).fragmentos();
        int mejor = BitacoraTexto.mejorFragmento(fragmentos, pregunta);
        if (mejor < 0)
            return new Respuesta("No encontré información suficiente en la bitácora para responder.", 0, "");
        return new Respuesta(fragmentos.get(mejor), mejor + 1, fragmentos.get(mejor));
    }

    private void exigirTaller(Long tallerId) {
        Integer existe = db.queryForObject("SELECT COUNT(*) FROM talleres WHERE id=?", Integer.class, tallerId);
        if (existe == null || existe == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
    }
}
