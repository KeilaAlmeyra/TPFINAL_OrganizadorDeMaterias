package organizador.servicios;

import organizador.modelos.*;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestorAsignacionesTest {

    private final GestorAsignaciones gestor = new GestorAsignaciones();

    private Usuario crearProfesor() {
        return new Usuario(
                "12345678",
                "Profesor de prueba",
                "profesor@universidad.edu",
                "profesor"
        );
    }

    private Materia crearMateria() {
        return new Materia(
                "MAT01",
                "Matematica",
                4,
                true,
                1,
                "aula"
        );
    }

    private Solicitud crearSolicitud(String id) {
        return new Solicitud(
                id,
                crearProfesor(),
                crearMateria(),
                25,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                Solicitud.CLASE_REGULAR
        );
    }

    private Espacio crearAula() {
        return new Espacio(
                "AULA01",
                "Aula 101",
                30,
                "aula",
                "Primer piso",
                "disponible"
        );
    }

    @Test
    void encuentraEspacioCompatibleLibre() {

        Solicitud solicitud = crearSolicitud("S1");
        Espacio aula = crearAula();

        Espacio resultado = gestor.buscarEspacioCompatible(
                solicitud,
                List.of(aula),
                List.of()
        );

        assertNotNull(resultado);
        assertEquals("AULA01", resultado.getId());
    }

    @Test
    void noEncuentraEspacioOcupadoEnElMismoHorario() {

        Solicitud primera = crearSolicitud("S1");
        primera.cambiarEstado(Solicitud.APROBADA);

        Espacio aula = crearAula();

        Asignacion asignacion = new Asignacion(
                "AS1",
                primera,
                aula,
                primera.getDias(),
                primera.getHoraInicio(),
                primera.getHoraFin()
        );

        Solicitud segunda = crearSolicitud("S2");

        Espacio resultado = gestor.buscarEspacioCompatible(
                segunda,
                List.of(aula),
                List.of(asignacion)
        );

        assertNull(resultado);
    }

    @Test
    void noEncuentraEspacioConCapacidadInsuficiente() {

        Solicitud solicitud = crearSolicitud("S1");

        Espacio aulaPequena = new Espacio(
                "AULA02",
                "Aula pequeña",
                20,
                "aula",
                "Segundo piso",
                "disponible"
        );

        Espacio resultado = gestor.buscarEspacioCompatible(
                solicitud,
                List.of(aulaPequena),
                List.of()
        );

        assertNull(resultado);
    }


    @Test
    void noPermiteReservasConHorariosSuperpuestos() {

        // Primera solicitud: lunes de 10:00 a 12:00.
        Solicitud primera = crearSolicitud("S1");
        primera.cambiarEstado(Solicitud.APROBADA);

        Espacio aula = crearAula();

        Asignacion asignacion = new Asignacion(
                "AS1",
                primera,
                aula,
                primera.getDias(),
                primera.getHoraInicio(),
                primera.getHoraFin()
        );

        // Segunda solicitud: lunes de 11:00 a 13:00.
        Solicitud segunda = new Solicitud(
                "S2",
                crearProfesor(),
                crearMateria(),
                20,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                Solicitud.CLASE_REGULAR
        );

        Espacio resultado = gestor.buscarEspacioCompatible(
                segunda,
                List.of(aula),
                List.of(asignacion)
        );

        assertNull(resultado);
    }

}

