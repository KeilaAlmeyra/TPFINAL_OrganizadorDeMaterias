
package organizador.repositorios;

import organizador.modelos.PropuestaAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropuestaAsignacionRepository
        extends JpaRepository<PropuestaAsignacion, String> {

    List<PropuestaAsignacion> findBySolicitud_Id(String solicitudId);

    List<PropuestaAsignacion> findBySolicitud_Profesor_Id(String profesorId);

    List<PropuestaAsignacion> findByEstado(String estado);
}
