
package organizador.repositorios;

import organizador.modelos.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignacionRepository extends JpaRepository<Asignacion, String> {

    List<Asignacion> findBySolicitud_Profesor_Id(String profesorId);

    boolean existsByEspacio_Id(String espacioId);
}

