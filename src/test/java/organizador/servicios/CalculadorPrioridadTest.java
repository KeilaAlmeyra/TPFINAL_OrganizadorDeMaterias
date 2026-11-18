
package organizador.servicios;

import organizador.modelos.Materia;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadorPrioridadTest {

    private final CalculadorPrioridad calculador =
            new CalculadorPrioridad();

    private final Usuario profesor = new Usuario(
            "P1",
            "Profesor de prueba",
            "profesor@test.com",
            "profesor"
    );

    private Solicitud crearSolicitud(
            int cantidadAlumnos,
            int anio,
            boolean troncal) {

        Materia materia = new Materia(
                "M1",
                "Materia de prueba",
                4,
                troncal,
                anio,
                "aula"
        );

        return new Solicitud(
                "S1",
                profesor,
                materia,
                cantidadAlumnos,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                Solicitud.CLASE_REGULAR
        );
    }

    @Test
    void calculaCorrectamenteElPuntajePorCantidadDeAlumnos() {

        // Año 3: 9 puntos.
        // No troncal: 0 puntos.
        // Sin historial: 20 puntos.

        assertEquals(
                39.0,
                calculador.calcularPuntajePrioridad(
                        crearSolicitud(20, 3, false), List.of()
                ),
                0.001
        );

        assertEquals(
                44.0,
                calculador.calcularPuntajePrioridad(
                        crearSolicitud(21, 3, false), List.of()
                ),
                0.001
        );

        assertEquals(
                49.0,
                calculador.calcularPuntajePrioridad(
                        crearSolicitud(41, 3, false), List.of()
                ),
                0.001
        );

        assertEquals(
                54.0,
                calculador.calcularPuntajePrioridad(
                        crearSolicitud(61, 3, false), List.of()
                ),
                0.001
        );
    }

    @Test
    void calculaCorrectamenteElPuntajePorAnio() {

        // 20 alumnos: 10 puntos.
        // No troncal: 0 puntos.
        // Sin historial: 20 puntos.

        assertEquals(45.0, calcular(20, 1, false), 0.001);
        assertEquals(42.0, calcular(20, 2, false), 0.001);
        assertEquals(39.0, calcular(20, 3, false), 0.001);
        assertEquals(36.0, calcular(20, 4, false), 0.001);
        assertEquals(33.0, calcular(20, 5, false), 0.001);
    }

    @Test
    void unaMateriaTroncalSumaQuincePuntos() {

        double noTroncal = calcular(20, 1, false);
        double troncal = calcular(20, 1, true);

        assertEquals(15.0, troncal - noTroncal, 0.001);
    }

    @Test
    void elPuntajeMaximoEsSetentaYCinco() {

        // Más de 60 alumnos: 25.
        // Primer año: 15.
        // Troncal: 15.
        // Sin historial suficiente: 20.

        assertEquals(
                75.0,
                calcular(61, 1, true),
                0.001
        );
    }

    private double calcular(
            int alumnos,
            int anio,
            boolean troncal) {

        return calculador.calcularPuntajePrioridad(
                crearSolicitud(alumnos, anio, troncal),
                List.of()
        );
    }
}
