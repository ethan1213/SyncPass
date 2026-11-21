package cl.duoc.syncpass.inscripcion;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscripcionService {

    private final InscripcionRepository repository;

    public InscripcionService(InscripcionRepository repository) {
        this.repository = repository;
    }

    /** El usuario declara que planea asistir. Idempotente: no duplica si ya existia. */
    @Transactional
    public Inscripcion marcarPendiente(Long tallerId, String sub, String deviceId) {
        return buscar(tallerId, sub, deviceId).orElseGet(() -> {
            var fila = new Inscripcion();
            fila.tallerId = tallerId;
            fila.participanteSub = sub;
            fila.deviceId = deviceId;
            fila.estado = "pendiente";
            fila.creadoEn = Instant.now();
            return repository.save(fila);
        });
    }

    /**
     * Se llama al aceptar una acreditacion real. Si no habia preinscripcion la
     * crea directamente en "asistio": la asistencia espontanea nunca se bloquea.
     */
    @Transactional
    public void marcarAsistio(Long tallerId, String sub, String deviceId) {
        var fila = buscar(tallerId, sub, deviceId).orElseGet(() -> {
            var nueva = new Inscripcion();
            nueva.tallerId = tallerId;
            nueva.participanteSub = sub;
            nueva.deviceId = deviceId;
            nueva.creadoEn = Instant.now();
            return nueva;
        });
        fila.estado = "asistio";
        fila.asistioEn = Instant.now();
        repository.save(fila);
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listar(Long tallerId) {
        return repository.findByTallerId(tallerId);
    }

    /** Todas las inscripciones del usuario (pendiente y asistio), para que la app muestre ambos estados. */
    @Transactional(readOnly = true)
    public List<Inscripcion> misInscripciones(String sub) {
        return repository.findByParticipanteSub(sub);
    }

    private java.util.Optional<Inscripcion> buscar(Long tallerId, String sub, String deviceId) {
        return sub != null
            ? repository.findByTallerIdAndParticipanteSub(tallerId, sub)
            : repository.findByTallerIdAndDeviceId(tallerId, deviceId);
    }
}
