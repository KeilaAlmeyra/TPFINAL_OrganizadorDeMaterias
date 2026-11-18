package organizador.repositorios;

import organizador.modelos.RegistroCumplimiento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link RegistroCumplimiento}.
 * Extiende de {@link JpaRepository} para proveer operaciones CRUD estándar,
 * paginación y gestión de persistencia sobre el historial y seguimiento del cumplimiento
 * de las asignaciones o normativas en la base de datos.
 */
public interface RegistroCumplimientoRepository
        extends JpaRepository<RegistroCumplimiento, String> {
}