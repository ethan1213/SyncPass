package cl.duoc.syncpass;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity @Table(name="talleres")
public class Taller {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @NotBlank @Size(max=160) public String title;
 @NotBlank @Size(max=2000) public String descripcion;
 @NotBlank @Size(max=100) public String area;
 @NotBlank @Size(max=160) public String unidadVecinal;
 @NotBlank @Size(max=100) public String comuna;
 @NotBlank @Size(max=200) public String direccion;
 @NotBlank @Size(max=50) public String fechaHora;
 @NotBlank @Size(max=120) public String docenteLider;
 @Min(1) @Max(10000) public int cuposMaximos;
 @Column(name="proyecto_id") public Long proyectoId;
 @Version public Long version;
 @Transient public long inscritos;
 public double getQuorumPct(){return cuposMaximos>0 ? Math.round(inscritos*1000.0/cuposMaximos)/10.0 : 0;}
}
