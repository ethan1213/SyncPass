package cl.duoc.syncpass.bitacora;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
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
    private final BitacoraGemini gemini;

    public BitacoraService(JdbcTemplate db, BitacoraGemini gemini) {
        this.db = db;
        this.gemini = gemini;
    }

    public record Bitacora(String contenido, String resumen, String actualizadoEn, List<String> fragmentos) {}
    public record Archivo(String formato, byte[] contenido) {}

    @Transactional
    public void guardarArchivo(long tallerId, Archivo archivo) {
        if (db.queryForList("SELECT id FROM talleres WHERE id=? FOR UPDATE", Long.class, tallerId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
        if (db.update("UPDATE taller_archivos SET formato=?, contenido=? WHERE taller_id=?",
            archivo.formato(), archivo.contenido(), tallerId) == 0)
            db.update("INSERT INTO taller_archivos(taller_id, formato, contenido) VALUES (?,?,?)",
                tallerId, archivo.formato(), archivo.contenido());
    }

    @Transactional(readOnly = true)
    public Archivo archivo(long tallerId) {
        return db.query("SELECT formato, contenido FROM taller_archivos WHERE taller_id=?",
            (fila, indice) -> new Archivo(fila.getString(1), fila.getBytes(2)), tallerId).stream()
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay archivo guardado"));
    }

    public record Respuesta(String respuesta, int fragmento, String cita, String origen) {}

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
    public Bitacora publicar(Long tallerId, String contenido, String resumenRevisado) {
        exigirTaller(tallerId);
        var texto = contenido.trim();
        var resumen = resumenRevisado == null || resumenRevisado.isBlank()
            ? resumenExtractivo(texto) : resumenRevisado.strip();
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

    /** La consulta a PostgreSQL termina antes de llamar al proveedor. */
    public Respuesta responder(Long tallerId, String pregunta) {
        var bitacora = obtener(tallerId);
        String normalizada = Normalizer.normalize(pregunta.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").strip();
        if (normalizada.matches("hola( que tal| como estas)?|buenas( tardes| noches)?|buenos dias|hey"))
            return conversacion("saludo");
        if (normalizada.matches("(muchas )?gracias( por la ayuda)?|te agradezco"))
            return conversacion("agradecimiento");
        if (normalizada.matches("que puedo preguntar|que puedes hacer|en que puedes ayudarme|ayuda"))
            return conversacion("ayuda");
        if (normalizada.matches("(resume|resumeme|dame un resumen de|puedes resumir|explica) (el |este |la )?(taller|sesion|clase)|resumen"))
            return new Respuesta(bitacora.resumen(), 0, "", "resumen_publicado");

        var fragmentos = bitacora.fragmentos();
        if (gemini.disponible()) {
            try {
                var seleccion = gemini.clasificarPregunta(pregunta, bitacora.resumen(), fragmentos);
                if (seleccion.intencion().equals("respuesta"))
                    return citar(fragmentos, seleccion.fragmentos(), "gemini");
                if (seleccion.intencion().equals("sin_datos")) return sinDatos("gemini");
                return conversacion(seleccion.intencion());
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode() != HttpStatus.SERVICE_UNAVAILABLE) throw ex;
            }
        }
        int mejor = BitacoraTexto.mejorFragmento(fragmentos, pregunta);
        return mejor < 0 ? sinDatos("extractivo") : citar(fragmentos, List.of(mejor + 1), "extractivo");
    }

    private static Respuesta citar(List<String> fragmentos, List<Integer> indices, String origen) {
        int primero = indices.getFirst();
        String cita = fragmentos.get(primero - 1);
        if (indices.size() == 1) return new Respuesta(cita, primero, cita, origen);
        String respuesta = String.join("\n\n", indices.stream()
            .map(n -> "Fragmento " + n + ": " + fragmentos.get(n - 1)).toList());
        return new Respuesta(respuesta, primero, cita, origen);
    }

    private static Respuesta sinDatos(String origen) {
        return new Respuesta("No encontré información suficiente en la bitácora para responder.", 0, "", origen);
    }

    private static Respuesta conversacion(String intencion) {
        String texto = switch (intencion) {
            case "saludo" -> "¡Hola! Puedo ayudarte a consultar el resumen y los apuntes de este taller.";
            case "agradecimiento" -> "De nada. Si quieres, pregúntame por los temas, acuerdos o tareas de este taller.";
            default -> "Puedes preguntarme qué se explicó, qué se acordó o pedir el resumen de este taller.";
        };
        return new Respuesta(texto, 0, "", "conversacion");
    }

    static String resumenExtractivo(String contenido) {
        String resumen = BitacoraTexto.resumir(BitacoraTexto.fragmentos(contenido));
        return resumen.length() <= MAX_RESUMEN ? resumen : resumen.substring(0, MAX_RESUMEN);
    }

    void exigirTaller(Long tallerId) {
        Integer existe = db.queryForObject("SELECT COUNT(*) FROM talleres WHERE id=?", Integer.class, tallerId);
        if (existe == null || existe == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado");
    }
}
