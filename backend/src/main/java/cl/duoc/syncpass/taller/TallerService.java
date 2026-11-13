package cl.duoc.syncpass.taller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de negocio del mantenedor de talleres.
 *
 * El controlador solo traduce HTTP; todo lo que decide si una operacion es
 * valida vive aqui, de modo que la misma regla sirva a la web, al telefono y a
 * cualquier prueba automatizada.
 */
@Service
public class TallerService {

    private final TallerRepository repository;
    private final JdbcTemplate db;

    public TallerService(TallerRepository repository, JdbcTemplate db) {
        this.repository = repository;
        this.db = db;
    }

    /**
     * Catalogo con filtros opcionales.
     *
     * Los inscritos se resuelven con UNA consulta agrupada en vez de una por
     * taller: antes el listado hacia N+1 consultas y crecia con el catalogo.
     */
    @Transactional(readOnly = true)
    public List<Taller> buscar(Long proyectoId, String comuna, String area, String texto) {
        var talleres = repository.buscar(proyectoId, limpiar(comuna), limpiar(area), limpiar(texto));
        var conteos = conteoAsistencias();
        talleres.forEach(t -> t.inscritos = conteos.getOrDefault(t.id, 0L));
        return talleres;
    }

    @Transactional(readOnly = true)
    public Taller obtener(Long id) {
        var taller = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado"));
        taller.inscritos = conteoAsistencia(id);
        return taller;
    }

    @Transactional
    public Taller crear(Taller input) {
        validar(input);
        input.id = null;
        input.version = null;
        var creado = repository.saveAndFlush(input);
        creado.inscritos = 0;
        return creado;
    }

    @Transactional
    public Taller actualizar(Long id, Taller input) {
        validar(input);
        var actual = obtener(id);
        if (!Objects.equals(actual.version, input.version))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El taller cambió; recarga antes de editar");
        if (input.cuposMaximos < actual.inscritos)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya hay " + actual.inscritos + " personas acreditadas: no puedes dejar menos cupos que eso");
        input.id = id;
        var guardado = repository.saveAndFlush(input);
        guardado.inscritos = actual.inscritos;
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        var taller = obtener(id);
        if (taller.inscritos > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El taller tiene " + taller.inscritos + " asistencia(s) registrada(s): no se puede borrar el historial");
        repository.delete(taller);
        repository.flush();
    }

    /** Asistentes acreditados de un taller. */
    @Transactional(readOnly = true)
    public long conteoAsistencia(Long tallerId) {
        Long total = db.queryForObject(
            "SELECT COUNT(*) FROM registros WHERE taller_id=? AND tipo='asistencia'", Long.class, tallerId);
        return total == null ? 0 : total;
    }

    /** Asistentes de todos los talleres en una sola pasada: id -> inscritos. */
    private Map<Long, Long> conteoAsistencias() {
        var conteos = new HashMap<Long, Long>();
        db.query("SELECT taller_id, COUNT(*) AS total FROM registros WHERE tipo='asistencia' GROUP BY taller_id",
            rs -> { conteos.put(rs.getLong("taller_id"), rs.getLong("total")); });
        return conteos;
    }

    /** La geocerca es todo o nada: no sirve media coordenada. */
    private void validar(Taller input) {
        int definidos = (input.latitud != null ? 1 : 0)
            + (input.longitud != null ? 1 : 0)
            + (input.radioMetros != null ? 1 : 0);
        if (definidos != 0 && definidos != 3)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Completa latitud, longitud y radio de la geocerca");
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
