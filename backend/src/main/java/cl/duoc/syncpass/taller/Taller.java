package cl.duoc.syncpass.taller;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Taller en terreno: la unidad que se publica, se acredita y se encuesta.
 *
 * Los tres campos de geocerca van juntos o no van: si el taller define
 * latitud/longitud/radio, la asistencia solo se acepta dentro de ese circulo.
 */
@Entity
@Table(name = "talleres")
public class Taller {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank @Size(max = 160) public String title;
    @NotBlank @Size(max = 2000) public String descripcion;
    @NotBlank @Size(max = 100) public String area;
    @NotBlank @Size(max = 160) public String unidadVecinal;
    @NotBlank @Size(max = 100) public String comuna;
    @NotBlank @Size(max = 200) public String direccion;
    @NotBlank @Size(max = 50) public String fechaHora;
    @NotBlank @Size(max = 120) public String docenteLider;
    @Min(1) @Max(10000) public int cuposMaximos;

    @Column(name = "proyecto_id") public Long proyectoId;

    @DecimalMin("-90.0") @DecimalMax("90.0") public Double latitud;
    @DecimalMin("-180.0") @DecimalMax("180.0") public Double longitud;
    @Min(10) @Max(5000) public Integer radioMetros;

    /** Bloqueo optimista: evita que dos coordinadores se pisen al editar. */
    @Version public Long version;

    /** Calculado al leer; no existe como columna. */
    @Transient public long inscritos;

    public double getQuorumPct() {
        return cuposMaximos > 0 ? Math.round(inscritos * 1000.0 / cuposMaximos) / 10.0 : 0;
    }

    /** true cuando el taller exige validar la ubicacion del participante. */
    public boolean tieneGeocerca() {
        return latitud != null && longitud != null && radioMetros != null;
    }
}
