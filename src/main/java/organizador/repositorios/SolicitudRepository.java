package organizador.repositorios;

import organizador.modelos.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Solicitud}.
 * Extiende de {@link JpaRepository} para proveer operaciones CRUD estándar,
 * paginación y métodos de consulta sobre las solicitudes de asignación de espacios o recursos.
 */
public interface SolicitudRepository extends JpaRepository<Solicitud, String> {

    /**
     * Objetivo:
     * Verificar si existe al menos una solicitud asociada a una materia específica.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code materiaId} no debe ser nulo.
     *
     * Parámetros:
     * @param materiaId Identificador único (String) de la materia a verificar.
     *
     * Retorno:
     * @return {@code true} si existe al menos una solicitud vinculada al identificador de la materia;
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos permanece inmutable.
     */
    boolean existsByMateria_Id(String materiaId);
}