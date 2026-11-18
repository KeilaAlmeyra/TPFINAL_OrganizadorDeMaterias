
package organizador.servicios;

import organizador.modelos.Solicitud;
import organizador.modelos.RegistroCumplimiento;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
/**
 * Calcula y ordena las solicitudes según
 * los criterios de prioridad establecidos.
 */
public class CalculadorPrioridad {

    private static final int MAX_REGISTROS = 10;
    private static final int MIN_REGISTROS = 3;

    /**
     * Calcula el puntaje total de prioridad.
     * El máximo es 75 puntos.
     */
    public double calcularPuntajePrioridad(
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

        double anio = calcularPuntajeAnio(
                solicitud.getMateria().getAnioCarrera()
        );

        double troncal = solicitud.getMateria().esTroncal() ? 15 : 0;

        double cumplimiento = calcularPuntajeCumplimiento(
                solicitud, historial
        );

        return alumnos + anio + troncal + cumplimiento;
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
     * Asigna puntos según el año de carrera.
     * Los años inferiores reciben mayor puntaje,
     * pero el año no determina por sí solo el orden.
     */
    private double calcularPuntajeAnio(int anio) {

        return switch (anio) {
            case 1 -> 15;
            case 2 -> 12;
            case 3 -> 9;
            case 4 -> 6;
            default -> anio >= 5 ? 3 : 0;
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

        if (registros.size() < MIN_REGISTROS) {
            return 20;
        }

        long cumplidos = registros.stream()
                .filter(registro -> !registro.esIncumplimiento())
                .count();

        double porcentaje =
                (double) cumplidos / registros.size() * 100;

        return porcentaje * 0.20;
    }

    /**
     * Ordena todas las solicitudes por puntaje total,
     * de mayor a menor.
     */
    public List<Solicitud> ordenarSolicitudes(
            List<Solicitud> solicitudes,
            List<RegistroCumplimiento> historial) {

        return solicitudes.stream()
                .sorted(
                        Comparator.comparingDouble(
                                (Solicitud s) ->
                                        calcularPuntajePrioridad(
                                                s, historial
                                        )
                        ).reversed()
                )
                .toList();
    }
}
