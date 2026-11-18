package organizador.servicios;

import organizador.modelos.Solicitud;
import organizador.modelos.RegistroCumplimiento;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Representa el componente de servicio encargado de calcular la ponderación numérica
 * (prioridad) de cada solicitud de aula y ordenar los lotes de solicitudes en función de
 * criterios académicos, institucionales y de conducta histórica de los docentes.
 */
@Service
public class CalculadorPrioridad {

    private static final int MAX_REGISTROS = 10;
    private static final int MIN_REGISTROS = 3;

    /**
     * Objetivo:
     * Calcular el puntaje secundario ponderado de una solicitud de aula específica,
     * evaluando cuatro dimensiones: tamaño de la comisión, relevancia del plan de estudios (materia troncal),
     * criticidad del tipo de actividad y antecedentes de cumplimiento del docente.
     *
     * Precondiciones:
     * - {@code solicitud} no debe ser nula.
     * - {@code solicitud} debe tener asociadas instancias válidas y no nulas de {@code Materia} y {@code Profesor}.
     *
     * Parámetros:
     * @param solicitud Instancia de {@link Solicitud} cuyos atributos serán evaluados para la asignación de puntaje.
     * @param historial Colección {@link List}<{@link RegistroCumplimiento}> con los antecedentes históricos del sistema, o {@code null}.
     *
     * Retorno:
     * @return {@code double} Valor numérico correspondiente a la suma ponderada calculada, con un valor máximo posible de 85 puntos.
     *
     * Postcondiciones:
     * - El estado de la solicitud y de los registros históricos permanece inalterado (método libre de efectos secundarios).
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code solicitud} es {@code null}.
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
     * Objetivo:
     * Determinar los puntos otorgados a una solicitud en función de la escala o rango de cantidad de alumnos esperados.
     *
     * Precondiciones:
     * - Ninguna específica sobre el entorno; {@code cantidad} puede asumir cualquier valor entero.
     *
     * Parámetros:
     * @param cantidad Cantidad entera de estudiantes inscritos o estimados para la comisión.
     *
     * Retorno:
     * @return {@code double} Puntos asignados según el tramo de volumen de alumnos:
     *         25 si es > 60, 20 si está entre 41 y 60, 15 si está entre 21 y 40,
     *         10 si está entre 1 y 20, o 0 si es menor o igual a 0.
     *
     * Postcondiciones:
     * - Ninguna (cálculo puro sin mutación de estado).
     */
    private double calcularPuntajeAlumnos(int cantidad) {

        if (cantidad > 60) return 25;
        if (cantidad >= 41) return 20;
        if (cantidad >= 21) return 15;
        if (cantidad >= 1) return 10;

        return 0;
    }

    /**
     * Objetivo:
     * Obtener el puntaje correspondiente al nivel de prioridad e impacto pedagógico que reviste el tipo de actividad solicitada.
     *
     * Precondiciones:
     * - Ninguna (acepta valores nulos o cadenas arbitrarias).
     *
     * Parámetros:
     * @param actividad Cadena {@link String} representativa del tipo de evento (ej. {@code EXAMEN_FINAL}, {@code PARCIAL}, {@code CLASE_REGULAR}).
     *
     * Retorno:
     * @return {@code double} Puntaje fijado por la regla de negocio para la actividad indicada (25 para exámenes finales,
     *         22 para parciales, 20 para recuperatorios, 15 para clases obligatorias, 10 para regulares, o 0 si es nula o desconocida).
     *
     * Postcondiciones:
     * - Ninguna (evaluación inmutable).
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
     * Objetivo:
     * Calcular el componente de puntaje por cumplimiento docente evaluando la tasa de cumplimiento
     * sobre los últimos 10 registros evaluables del profesor; otorga el beneficio del puntaje pleno
     * por defecto en caso de no contar con un historial mínimo significativo.
     *
     * Precondiciones:
     * - {@code solicitud} debe tener un {@code Profesor} con identificador accesible y no nulo.
     *
     * Parámetros:
     * @param solicitud Entidad {@link Solicitud} de la cual se extrae el identificador del docente.
     * @param historial Lista {@link List}<{@link RegistroCumplimiento}> con todos los registros históricos del sistema, o {@code null}.
     *
     * Retorno:
     * @return {@code double} Puntos obtenidos (escala de 0.0 a 20.0). Retorna 20.0 puntos si {@code historial} es nulo
     *         o si el docente cuenta con menos de 3 registros evaluables en su historial.
     *
     * Postcondiciones:
     * - No altera las listas ni las entidades involucradas; se ejecuta de forma aislada.
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
     * Objetivo:
     * Ordenar un conjunto de solicitudes académicas aplicando un orden multinivel: primero en forma ascendente
     * por año de carrera de la materia y, en caso de empate, en forma descendente por el puntaje secundario ponderado.
     *
     * Precondiciones:
     * - {@code solicitudes} no debe ser nula y no debe contener elementos nulos.
     * - Cada solicitud en la lista debe tener configurada una materia con su respectivo año de carrera y un profesor válido.
     *
     * Parámetros:
     * @param solicitudes Lista {@link List}<{@link Solicitud}> con las solicitudes a ordenar.
     * @param historial Historial global {@link List}<{@link RegistroCumplimiento}> necesario para la ponderación del cumplimiento.
     *
     * Retorno:
     * @return {@link List}<{@link Solicitud}> Nueva lista inmutable con las solicitudes ordenadas según la política institucional.
     *
     * Postcondiciones:
     * - La lista original proporcionada como argumento no sufre mutaciones.
     * - La lista resultante contiene exactamente los mismos elementos referenciados organizados bajo la regla:
     *   {@code anioCarrera (ascendente) -> puntajeSecundario (descendente)}.
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