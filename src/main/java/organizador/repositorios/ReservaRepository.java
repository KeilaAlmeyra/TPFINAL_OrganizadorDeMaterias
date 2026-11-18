
package organizador.repositorios;

import organizador.modelos.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Reserva}.
 * Extiende de {@link JpaRepository} para gestionar la persistencia, consultas de disponibilidad,
 * detección de solapamientos horarios y filtrado por usuario, estado o espacio físico.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Objetivo:
     * Obtener la totalidad de las reservas registradas, ordenadas cronológicamente
     * por fecha y luego por hora de inicio.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Reserva}> Lista con todas las reservas ordenadas de forma ascendente
     *         por fecha y hora de inicio. Retorna una lista vacía si no hay registros.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos permanece inalterado.
     */
    List<Reserva> findAllByOrderByFechaAscHoraInicioAsc();

    /**
     * Objetivo:
     * Buscar reservas en las que el nombre de la persona que reservó coincida parcial o totalmente
     * con el criterio dado (sin distinguir mayúsculas de minúsculas), ordenadas cronológicamente.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code nombre} no debe ser nulo.
     *
     * Parámetros:
     * @param nombre Cadena de texto (String) con el término de búsqueda a comparar contra el campo {@code reservadoPor}.
     *
     * Retorno:
     * @return {@link List}<{@link Reserva}> Lista de reservas coincidentes ordenadas por fecha y hora de inicio ascendente.
     *         Retorna una lista vacía si no hay coincidencias.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en la base de datos no sufren modificaciones.
     */
    List<Reserva> findByReservadoPorContainingIgnoreCaseOrderByFechaAscHoraInicioAsc(
            String nombre);

    /**
     * Objetivo:
     * Recuperar las reservas vinculadas al correo electrónico del propietario de la cuenta,
     * ignorando mayúsculas y minúsculas, y ordenadas cronológicamente.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code correo} no debe ser nulo.
     *
     * Parámetros:
     * @param correo Dirección de correo electrónico (String) del propietario de las reservas.
     *
     * Retorno:
     * @return {@link List}<{@link Reserva}> Lista de reservas asociadas a la cuenta especificada,
     *         ordenadas por fecha y hora de inicio. Retorna una lista vacía si la cuenta no posee reservas.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no altera los registros existentes.
     */
    List<Reserva> findByPropietarioCorreoIgnoreCaseOrderByFechaAscHoraInicioAsc(
            String correo);

    /**
     * Objetivo:
     * Consultar las reservas que se encuentren en un estado específico (por ejemplo: PENDIENTE, REVISION),
     * presentadas en orden cronológico ascendente.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code estado} no debe ser nulo.
     *
     * Parámetros:
     * @param estado Estado de la reserva (String) utilizado como filtro.
     *
     * Retorno:
     * @return {@link List}<{@link Reserva}> Lista de reservas filtradas por el estado indicado,
     *         ordenadas por fecha y hora de inicio. Retorna una lista vacía si no hay registros en ese estado.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no produce efectos secundarios en la base de datos.
     */
    List<Reserva> findByEstadoOrderByFechaAscHoraInicioAsc(String estado);

    /**
     * Objetivo:
     * Buscar una reserva puntual por su identificador primario, asegurando que pertenezca
     * al usuario identificado por su correo electrónico (control de acceso/propiedad).
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code id} y el parámetro {@code correo} no deben ser nulos.
     *
     * Parámetros:
     * @param id Identificador numérico único (Long) de la reserva.
     * @param correo Correo electrónico (String) del propietario de la reserva para validar pertenencia.
     *
     * Retorno:
     * @return {@link Optional}<{@link Reserva}> Contenedor con la entidad {@link Reserva} si existe y coincide
     *         con el propietario indicado; {@link Optional#empty()} en caso contrario.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos se mantiene inmutable.
     */
    Optional<Reserva> findByIdAndPropietarioCorreoIgnoreCase(
            Long id, String correo);

    /**
     * Objetivo:
     * Contabilizar la cantidad de reservas confirmadas o sin estado definido que se solapan en horario
     * con un intervalo determinado para un mismo espacio físico y fecha.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - Los parámetros {@code espacioId}, {@code fecha}, {@code inicio} y {@code fin} no deben ser nulos.
     * - {@code inicio} debe ser cronológicamente anterior a {@code fin}.
     *
     * Parámetros:
     * @param espacioId Identificador único (String) del espacio físico a evaluar.
     * @param fecha Fecha (LocalDate) en la que se desea verificar la disponibilidad.
     * @param inicio Hora de inicio (LocalTime) del intervalo horario solicitado.
     * @param fin Hora de finalización (LocalTime) del intervalo horario solicitado.
     *
     * Retorno:
     * @return {@code long} Número entero de reservas que entran en conflicto o superposición horaria
     *         ({@code horaInicio < fin} y {@code horaFin > inicio}) dentro del espacio y fecha dados.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no modifica registros.
     * - Un valor de retorno de 0 indica que el espacio se encuentra libre en dicho rango horario.
     */
    @Query("""
        select count(r) from Reserva r
        where r.espacio.id = :espacioId
          and r.fecha = :fecha
          and (r.estado = 'CONFIRMADA' or r.estado is null)
          and r.horaInicio < :fin
          and r.horaFin > :inicio
        """)
    long contarSuperposiciones(@Param("espacioId") String espacioId,
                               @Param("fecha") LocalDate fecha,
                               @Param("inicio") LocalTime inicio,
                               @Param("fin") LocalTime fin);

    /**
     * Objetivo:
     * Determinar si existe al menos una reserva vinculada a un espacio físico específico.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code espacioId} no debe ser nulo.
     *
     * Parámetros:
     * @param espacioId Identificador único (String) del espacio a verificar.
     *
     * Retorno:
     * @return {@code true} si existe al menos una reserva asociada al identificador provisto;
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos permanece inmutable.
     */
    boolean existsByEspacio_Id(String espacioId);
}