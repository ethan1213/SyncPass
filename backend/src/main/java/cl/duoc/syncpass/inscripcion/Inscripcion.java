package cl.duoc.syncpass.inscripcion;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Preinscripcion a un taller: queda "pendiente" cuando el usuario declara
 * que planea asistir, y pasa a "asistio" cuando una acreditacion real
 * (ingreso de cuenta o invitado) se acepta para esa misma persona y taller.
 * No bloquea la asistencia espontanea: si no hubo preinscripcion, el ingreso
 * la crea directamente en "asistio".
 */
@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "taller_id") public Long tallerId;

    /** Cuenta real (sub de Cognito); nulo si la inscripcion es de un invitado. */
    @Column(name = "participante_sub") public String participanteSub;

    /** Identificador del telefono del invitado; nulo si es una cuenta real. */
    @Column(name = "device_id") public String deviceId;

    /** "pendiente" o "asistio". */
    public String estado;

    @Column(name = "creado_en") public Instant creadoEn;
    @Column(name = "asistio_en") public Instant asistioEn;
}
