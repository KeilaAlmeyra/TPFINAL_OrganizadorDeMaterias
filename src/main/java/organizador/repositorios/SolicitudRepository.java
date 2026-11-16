
package organizador.repositorios;

import organizador.modelos.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepository extends JpaRepository<Solicitud, String> {
}
