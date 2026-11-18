package organizador.repositorios;

import organizador.modelos.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Configuracion}.
 * Extiende de {@link JpaRepository} para proporcionar la gestión completa de persistencia,
 * operaciones CRUD estándar y paginación sobre los parámetros de configuración del sistema en la base de datos.
 */
public interface ConfiguracionRepository
        extends JpaRepository<Configuracion, String> {
}