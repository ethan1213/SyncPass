package cl.duoc.syncpass.proyecto;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** Proyecto comunitario: agrupa los talleres de una misma intervencion territorial. */
@Entity
@Table(name = "proyectos_comunitarios")
public class Proyecto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank @Size(max = 160) public String title;
    @NotBlank @Size(max = 2000) public String descripcion;
    @NotBlank @Size(max = 100) public String area;
    @NotBlank @Size(max = 160) public String unidadVecinal;
    @NotBlank @Size(max = 10) public String fechaInicio;
    @NotBlank @Size(max = 10) public String fechaTermino;
    @NotBlank @Pattern(regexp = "planificado|activo|finalizado") public String estado;

    /** Bloqueo optimista, igual que en Taller. */
    @Version public Long version;
}
