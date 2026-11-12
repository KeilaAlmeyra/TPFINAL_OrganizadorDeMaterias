package organizador.servicios;

import organizador.modelos.Solicitud;
import organizador.modelos.RegistroCumplimiento;

import java.util.Comparator;
import java.util.List;

/**
 * Calcula y ordena las solicitudes según
 * los criterios de prioridad establecidos.
 */
public class CalculadorPrioridad {

    private static final int MAX_REGISTROS = 10;
    private static final int MIN_REGISTROS = 3;

    /**
     * Calcula el puntaje secundario de una solicitud.
     * El máximo es 85 puntos.
     */
    public double calcularPuntajeSecundario(
            Solicitud solicitud,
            List<RegistroCumplimiento> historial) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud no puede ser nula."
            );
        }

        double alumnos = calcularPuntajeAlumnos(
                solicitud.getCantidadAlumnos()
        );

        double troncal = solicitud.getMateria().esTroncal() ? 15 : 0;

        double actividad = calcularPuntajeActividad(
                solicitud.getTipoActividad()
        );

        double cumplimiento = calcularPuntajeCumplimiento(
                solicitud, historial
        );

        return alumnos + troncal + actividad + cumplimiento;
    }

    /**
     * Asigna puntos según la cantidad de alumnos.
     */
    private double calcularPuntajeAlumnos(int cantidad) {

        if (cantidad > 60) return 25;
        if (cantidad >= 41) return 20;
        if (cantidad >= 21) return 15;
        if (cantidad >= 1) return 10;

        return 0;
    }

    /**
     * Asigna puntos según el tipo de actividad.
     */
    private double calcularPuntajeActividad(String actividad) {

        if (actividad == null) return 0;

        return switch (actividad) {
            case Solicitud.EXAMEN_FINAL -> 25;
            case Solicitud.PARCIAL -> 22;
            case Solicitud.RECUPERATORIO -> 20;
            case Solicitud.CLASE_OBLIGATORIA -> 15;
            case Solicitud.CLASE_REGULAR -> 10;
            default -> 0;
        };
    }

    /**
     * Calcula el cumplimiento del profesor
     * utilizando sus últimos 10 registros evaluables.
     *
     * Si todavía no tiene al menos 3 registros,
     * no recibe una penalización por falta de historial.
     */
    private double calcularPuntajeCumplimiento(
            Solicitud solicitud,
            List<RegistroCumplimiento> historial) {

        if (historial == null) {
            return 20;
        }

        // Obtenemos los registros del profesor de esta solicitud.
        List<RegistroCumplimiento> registros = historial.stream()
                .filter(registro ->
                        registro.getAsignacion()
                                .getSolicitud()
                                .getProfesor()
                                .getId()
                                .equals(solicitud.getProfesor().getId())
                )
                .filter(RegistroCumplimiento::esEvaluable)
                .sorted(
                        Comparator.comparing(
                                RegistroCumplimiento::getFecha
                        ).reversed()
                )
                .limit(MAX_REGISTROS)
                .toList();

        // No penalizamos si hay pocos antecedentes.
        if (registros.size() < MIN_REGISTROS) {
            return 20;
        }

        // Contamos cuántos registros fueron cumplidos.
        long cumplidos = registros.stream()
                .filter(registro -> !registro.esIncumplimiento())
                .count();

        double porcentaje =
                (double) cumplidos / registros.size() * 100;

        // Convertimos el porcentaje a un máximo de 20 puntos.
        return porcentaje * 0.20;
    }

    /**
     * Ordena las solicitudes:
     * primero por año de carrera y después por puntaje.
     */
    public List<Solicitud> ordenarSolicitudes(
            List<Solicitud> solicitudes,
            List<RegistroCumplimiento> historial) {

        return solicitudes.stream()
                .sorted(
                        Comparator.comparingInt(
                                        (Solicitud s) ->
                                                s.getMateria().getAnioCarrera()
                                )
                                .thenComparing(
                                        Comparator.comparingDouble(
                                                (Solicitud s) ->
                                                        calcularPuntajeSecundario(
                                                                s, historial
                                                        )
                                        ).reversed()
                                )
                )
                .toList();
    }
}

