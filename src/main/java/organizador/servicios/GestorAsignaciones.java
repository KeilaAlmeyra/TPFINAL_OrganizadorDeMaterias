package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Solicitud;

import java.time.DayOfWeek;
import java.util.List;

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
    public Asignacion crearAsignacion(
            String id,
            Solicitud solicitud,
            Espacio espacio) {

        if (solicitud == null || espacio == null) {
            throw new IllegalArgumentException(
                    "La solicitud y el espacio son obligatorios."
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

        // Aprobar antes de crear la asignación,
        // porque el constructor de Asignacion lo requiere.
        solicitud.cambiarEstado(Solicitud.APROBADA);

        return new Asignacion(
                id,
                solicitud,
                espacio,
                solicitud.getDias(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin()
        );
    }
}
