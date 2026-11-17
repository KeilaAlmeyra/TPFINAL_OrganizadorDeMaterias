
package organizador.repositorios;

import organizador.modelos.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository
        extends JpaRepository<Configuracion, String> {
}
