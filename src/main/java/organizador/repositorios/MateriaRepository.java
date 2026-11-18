package organizador.repositorios;

import organizador.modelos.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Materia}.
 * Extiende de {@link JpaRepository} para proporcionar operaciones CRUD estándar,
 * paginación y ordenamiento sobre las materias o asignaturas académicas en la base de datos.
 */
public interface MateriaRepository extends JpaRepository<Materia, String> {
}