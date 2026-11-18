package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Solicitud;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.time.LocalTime;

/**
 * Representa el servicio encargado de la lógica de evaluación de compatibilidad
 * de espacios físicos y de la formalización y creación de asignaciones para las solicitudes académicas.
 */
@Service
public class GestorAsignaciones {

    /**
     * Objetivo:
     * Buscar y seleccionar el primer espacio físico que cumpla con los requerimientos
     * de tipo de espacio y capacidad para una solicitud dada, verificando además que no presente
     * superposiciones horarias ni de días con asignaciones activas existentes.
     *
     * Precondiciones:
     * - Si {@code solicitud} o {@code espacios} son nulos, el método no evalúa y retorna {@code null}.
     * - {@code solicitud} debe tener definidos su materia (con su tipo de espacio frecuente),
     *   la cantidad de alumnos, los días y el rango horario (hora de inicio y hora de fin).
     *
     * Parámetros:
     * @param solicitud Instancia de {@link Solicitud} para la cual se desea encontrar un aula o espacio compatible.
     * @param espacios Lista de {@link Espacio} candidatos disponibles para evaluar.
     * @param asignacionesExistentes Lista de {@link Asignacion} ya registradas en el sistema contra las cuales contrastar posibles conflictos de horario y día.
     *
     * Retorno:
     * @return El primer {@link Espacio} que satisface las restricciones de capacidad, tipo y disponibilidad horaria;
     *         o {@code null} si ningún espacio es apto o si los parámetros requeridos son nulos.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no modifica el estado de los espacios, de la solicitud ni de las asignaciones existentes.
     */
    public Espacio buscarEspacioCompatible(
            Solicitud solicitud,
            List<Espacio> espacios,
            List<Asignacion> asignacionesExistentes) {

        if (solicitud == null || espacios == null) {
            return null;
        }

        for (Espacio espacio : espacios) {

            // Verificar tipo, capacidad y disponibilidad.
            boolean compatible = espacio.esCompatible(
                    solicitud.getMateria().getTipoEspacioFrecuente(),
                    solicitud.getCantidadAlumnos()
            );

            if (!compatible) {
                continue;
            }

            // Comprobar si el espacio está ocupado.
            boolean ocupado = false;

            if (asignacionesExistentes != null) {
                for (Asignacion asignacion : asignacionesExistentes) {

                    if (asignacion == null || !asignacion.estaActiva()) {
                        continue;
                    }

                    boolean mismoEspacio =
                            asignacion.getEspacio().getId()
                                    .equals(espacio.getId());

                    boolean mismoDia = false;

                    for (DayOfWeek dia : solicitud.getDias()) {
                        if (asignacion.getDiasAsignados().contains(dia)) {
                            mismoDia = true;
                            break;
                        }
                    }

                    boolean mismoHorario =
                            solicitud.getHoraInicio().isBefore(
                                    asignacion.getHoraFinAsignada())
                                    && asignacion.getHoraInicioAsignada().isBefore(
                                    solicitud.getHoraFin());

                    if (mismoEspacio && mismoDia && mismoHorario) {
                        ocupado = true;
                        break;
                    }
                }
            }

            if (!ocupado) {
                return espacio;
            }
        }

        return null;
    }

    /**
     * Objetivo:
     * Aprobar una solicitud y generar su asignación respetando exactamente los días,
     * la hora de inicio y la hora de fin definidos originalmente en dicha solicitud.
     *
     * Precondiciones:
     * - {@code solicitud} no debe ser nula.
     * - Deben cumplirse las precondiciones exigidas por el método sobrecargado: identificador válido,
     *   solicitud en estado pendiente, espacio no nulo y compatible con la materia y cantidad de alumnos.
     *
     * Parámetros:
     * @param id Identificador único (String) para la nueva asignación.
     * @param solicitud Entidad {@link Solicitud} pendiente que se desea aprobar.
     * @param espacio Entidad {@link Espacio} asignado a la solicitud.
     *
     * Retorno:
     * @return Una nueva instancia de {@link Asignacion} configurada con los parámetros originales de la solicitud.
     *
     * Postcondiciones:
     * - El estado de {@code solicitud} cambia a {@code Solicitud.APROBADA}.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code solicitud} es {@code null} o si alguno de los datos delegados no es válido.
     */
    public Asignacion crearAsignacion(
            String id,
            Solicitud solicitud,
            Espacio espacio) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud es obligatoria."
            );
        }

        return crearAsignacion(
                id,
                solicitud,
                espacio,
                solicitud.getDias(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin()
        );
    }

    /**
     * Objetivo:
     * Aprobar una solicitud y generar una asignación permitiendo especificar días
     * y horarios puntuales (por ejemplo, provenientes de una propuesta alternativa).
     *
     * Precondiciones:
     * - {@code id} no debe ser nulo ni estar en blanco.
     * - {@code solicitud} no debe ser nula y debe encontrarse en estado pendiente ({@code solicitud.estaPendiente() == true}).
     * - {@code espacio} no debe ser nulo y debe ser compatible con el tipo de espacio de la materia y la cantidad de alumnos de la solicitud.
     * - {@code dias} no debe ser nulo ni estar vacío.
     * - {@code horaInicio} y {@code horaFin} no deben ser nulos, y {@code horaFin} debe ser posterior a {@code horaInicio}.
     *
     * Parámetros:
     * @param id Identificador único (String) que tendrá la asignación generada.
     * @param solicitud Entidad {@link Solicitud} pendiente que se formaliza.
     * @param espacio Entidad {@link Espacio} físico asignado.
     * @param dias Lista de {@link DayOfWeek} en los que se concede el espacio.
     * @param horaInicio Horario de inicio ({@link LocalTime}) asignado.
     * @param horaFin Horario de fin ({@link LocalTime}) asignado.
     *
     * Retorno:
     * @return Una nueva instancia de {@link Asignacion} con los datos y horarios configurados.
     *
     * Postcondiciones:
     * - El estado interno de {@code solicitud} se muta a {@code Solicitud.APROBADA}.
     * - Se instancia y devuelve un nuevo objeto {@link Asignacion}.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si los datos básicos (id, solicitud, espacio, días u horarios) son nulos o inválidos.
     * @throws IllegalArgumentException Si {@code solicitud} no se encuentra en estado pendiente.
     * @throws IllegalArgumentException Si {@code espacio} no resulta compatible con la solicitud.
     */
    public Asignacion crearAsignacion(
            String id,
            Solicitud solicitud,
            Espacio espacio,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {

        if (id == null || id.isBlank()
                || solicitud == null || espacio == null
                || dias == null || dias.isEmpty()
                || horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {

            throw new IllegalArgumentException(
                    "Los datos de la asignación no son válidos."
            );
        }

        if (!solicitud.estaPendiente()) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar pendiente."
            );
        }

        if (!espacio.esCompatible(
                solicitud.getMateria().getTipoEspacioFrecuente(),
                solicitud.getCantidadAlumnos())) {

            throw new IllegalArgumentException(
                    "El espacio no es compatible con la solicitud."
            );
        }

        solicitud.cambiarEstado(Solicitud.APROBADA);

        return new Asignacion(
                id,
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin
        );
    }

}