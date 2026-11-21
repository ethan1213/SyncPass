package cl.duoc.syncpass.taller;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla talleres. Spring Data genera la implementacion; aqui solo
 * se declara la consulta de catalogo con filtros opcionales.
 */
public interface TallerRepository extends JpaRepository<Taller, Long> {

    /** Cada filtro se ignora cuando llega nulo, asi un solo metodo sirve al catalogo y al buscador. */
    @Query("""
           SELECT t FROM Taller t
            WHERE (:proyectoId IS NULL OR t.proyectoId = :proyectoId)
              AND (:comuna IS NULL OR LOWER(t.comuna) = LOWER(CAST(:comuna AS string)))
              AND (:area IS NULL OR LOWER(t.area) = LOWER(CAST(:area AS string)))
              AND (:texto IS NULL
                   OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%'))
                   OR LOWER(t.descripcion) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%')))
            ORDER BY t.fechaHora ASC, t.id ASC
           """)
    List<Taller> buscar(@Param("proyectoId") Long proyectoId, @Param("comuna") String comuna,
                        @Param("area") String area, @Param("texto") String texto);

    boolean existsByProyectoId(Long proyectoId);
}
