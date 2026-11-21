package cl.duoc.syncpass.inscripcion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    Optional<Inscripcion> findByTallerIdAndParticipanteSub(Long tallerId, String participanteSub);
    Optional<Inscripcion> findByTallerIdAndDeviceId(Long tallerId, String deviceId);
    List<Inscripcion> findByTallerId(Long tallerId);
    List<Inscripcion> findByParticipanteSub(String participanteSub);
}
