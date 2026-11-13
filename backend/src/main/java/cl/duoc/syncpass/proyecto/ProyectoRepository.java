package cl.duoc.syncpass.proyecto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    List<Proyecto> findAllByOrderByFechaInicioDescIdDesc();

    List<Proyecto> findByEstadoOrderByFechaInicioDescIdDesc(String estado);
}
