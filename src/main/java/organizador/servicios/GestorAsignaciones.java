package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Solicitud;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.time.LocalTime;

@Service

public class GestorAsignaciones {

    /**
     * Busca un espacio compatible para una solicitud.
     *
     * Devuelve el primer espacio que cumple los requisitos
     * o null si no encuentra ninguno.
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
     * Aprueba una solicitud y genera su asignación.
     */

    /**
     * Aprueba una solicitud y genera su asignación original.
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
     * Aprueba una solicitud y genera su asignación
     * utilizando los días y horarios de una propuesta alternativa.
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
