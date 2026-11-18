package organizador.repositorios;

import organizador.modelos.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Asignacion}.
 * Extiende de {@link JpaRepository} para proporcionar operaciones CRUD estándar
 * y métodos de consulta personalizados sobre las asignaciones en la base de datos.
 */
public interface AsignacionRepository extends JpaRepository<Asignacion, String> {

    /**
     * Objetivo:
     * Recuperar todas las asignaciones vinculadas a las solicitudes realizadas por un profesor específico.
     *
     * Precondiciones:
     * - La base de datos debe estar accesible.
     * - El identificador del profesor no debe ser nulo.
     *
     * Parámetros:
     * @param profesorId Identificador único (String) del profesor cuyas asignaciones se desean consultar.
     *
     * Retorno:
     * @return {@link List}<{@link Asignacion}> Lista de asignaciones asociadas al profesor indicado.
     *         Retorna una lista vacía si no se encuentran registros coincidentes.
     *
     * Postcondiciones:
     * - El estado de la base de datos permanece inmutable (operación de solo lectura).
     * - Si existen registros, cada elemento retornado cumple con la relación Solicitud -> Profesor -> Id == profesorId.
     */
    List<Asignacion> findBySolicitud_Profesor_Id(String profesorId);

    /**
     * Objetivo:
     * Determinar la existencia de al menos una asignación asociada a un espacio físico específico.
     *
     * Precondiciones:
     * - La base de datos debe estar accesible.
     * - El identificador del espacio no debe ser nulo.
     *
     * Parámetros:
     * @param espacioId Identificador único (String) del espacio físico a verificar.
     *
     * Retorno:
     * @return {@code true} si existe al menos un registro de asignación vinculado al identificador del espacio;
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - El estado de la base de datos permanece inmutable (operación de solo lectura).
     */
    boolean existsByEspacio_Id(String espacioId);
}