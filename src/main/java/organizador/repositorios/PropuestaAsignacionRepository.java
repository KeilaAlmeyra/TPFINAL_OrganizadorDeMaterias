
package organizador.repositorios;

import organizador.modelos.PropuestaAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link PropuestaAsignacion}.
 * Extiende de {@link JpaRepository} para proveer operaciones CRUD estándar y métodos de
 * consulta especializados sobre las propuestas de asignación registradas en el sistema.
 */
public interface PropuestaAsignacionRepository
        extends JpaRepository<PropuestaAsignacion, String> {

    /**
     * Objetivo:
     * Obtener el listado de propuestas de asignación asociadas a una solicitud específica.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y conectada.
     * - El parámetro {@code solicitudId} no debe ser nulo.
     *
     * Parámetros:
     * @param solicitudId Identificador único (String) de la solicitud a consultar.
     *
     * Retorno:
     * @return {@link List}<{@link PropuestaAsignacion}> Lista de propuestas vinculadas a la solicitud indicada.
     *         Retorna una lista vacía si no existen coincidencias.
     *
     * Postcondiciones:
     * - La operación es de solo lectura; el estado de los datos permanece inalterado.
     * - Todos los elementos devueltos cumplen la relación Solicitud -> Id == solicitudId.
     */
    List<PropuestaAsignacion> findBySolicitud_Id(String solicitudId);
    boolean existsBySolicitud_IdAndEstado(
            String solicitudId,
            String estado
    );

    /**
     * Objetivo:
     * Recuperar todas las propuestas de asignación correspondientes a las solicitudes
     * gestionadas o creadas por un profesor en particular.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y conectada.
     * - El parámetro {@code profesorId} no debe ser nulo.
     *
     * Parámetros:
     * @param profesorId Identificador único (String) del profesor cuyas propuestas se requieren.
     *
     * Retorno:
     * @return {@link List}<{@link PropuestaAsignacion}> Lista de propuestas asociadas al profesor indicado.
     *         Retorna una lista vacía si no se hallan registros.
     *
     * Postcondiciones:
     * - La operación es de solo lectura; no modifica registros en la base de datos.
     * - Todos los elementos devueltos cumplen la relación Solicitud -> Profesor -> Id == profesorId.
     */
    List<PropuestaAsignacion> findBySolicitud_Profesor_Id(String profesorId);

    /**
     * Objetivo:
     * Consultar todas las propuestas de asignación que se encuentren en un estado determinado
     * (por ejemplo: PENDIENTE, APROBADA, RECHAZADA).
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y conectada.
     * - El parámetro {@code estado} no debe ser nulo.
     *
     * Parámetros:
     * @param estado Cadena de texto (String) que representa el estado por el cual filtrar las propuestas.
     *
     * Retorno:
     * @return {@link List}<{@link PropuestaAsignacion}> Lista de propuestas que coinciden con el estado especificado.
     *         Retorna una lista vacía si ninguna propuesta tiene dicho estado.
     *
     * Postcondiciones:
     * - La operación es de solo lectura; la base de datos permanece inmutable.
     * - Cada entidad en la lista resultante tiene un atributo {@code estado} equivalente al parámetro provisto.
     */
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
