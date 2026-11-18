
package organizador.repositorios;

import organizador.modelos.PropuestaAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropuestaAsignacionRepository
        extends JpaRepository<PropuestaAsignacion, String> {

    List<PropuestaAsignacion> findBySolicitud_Id(String solicitudId);
    boolean existsBySolicitud_IdAndEstado(
            String solicitudId,
            String estado
    );

    List<PropuestaAsignacion> findBySolicitud_Profesor_Id(String profesorId);

    List<PropuestaAsignacion> findByEstado(String estado);

    List<PropuestaAsignacion> findBySolicitud_Profesor_IdAndEstado(
            String profesorId,
            String estado
    );

    List<PropuestaAsignacion> findBySolicitud_Profesor_IdAndEstadoNot(
            String profesorId,
            String estado
    );
}
