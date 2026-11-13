package cl.duoc.syncpass.proyecto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import cl.duoc.syncpass.taller.TallerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reglas del mantenedor de proyectos comunitarios. */
@Service
public class ProyectoService {

    private final ProyectoRepository repository;
    private final TallerRepository talleres;

    public ProyectoService(ProyectoRepository repository, TallerRepository talleres) {
        this.repository = repository;
        this.talleres = talleres;
    }

    @Transactional(readOnly = true)
    public List<Proyecto> listar(String estado) {
        return estado == null || estado.isBlank()
            ? repository.findAllByOrderByFechaInicioDescIdDesc()
            : repository.findByEstadoOrderByFechaInicioDescIdDesc(estado.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public Proyecto obtener(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    @Transactional
    public Proyecto crear(Proyecto input) {
        validarFechas(input);
        input.id = null;
        input.version = null;
        return repository.saveAndFlush(input);
    }

    @Transactional
    public Proyecto actualizar(Long id, Proyecto input) {
        validarFechas(input);
        var actual = obtener(id);
        if (!Objects.equals(actual.version, input.version))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El proyecto cambió; recarga antes de editar");
        input.id = id;
        return repository.saveAndFlush(input);
    }

    /** No se borra un proyecto que todavia tiene talleres colgando. */
    @Transactional
    public void eliminar(Long id) {
        var proyecto = obtener(id);
        if (talleres.existsByProyectoId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El proyecto tiene talleres asociados: reasígnalos o bórralos primero");
        repository.delete(proyecto);
        repository.flush();
    }

    private void validarFechas(Proyecto input) {
        try {
            if (LocalDate.parse(input.fechaTermino).isBefore(LocalDate.parse(input.fechaInicio)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de término debe ser posterior al inicio");
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa fechas ISO válidas (AAAA-MM-DD)");
        }
    }
}
