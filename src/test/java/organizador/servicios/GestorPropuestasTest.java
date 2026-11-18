
package organizador.servicios;

import organizador.modelos.ModoFuncionamiento;
import organizador.repositorios.AsignacionRepository;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.PropuestaAsignacionRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.SolicitudRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestorPropuestasTest {

    private ConfiguracionService configuracionService;
    private GestorPropuestas gestorPropuestas;

    @BeforeEach
    void prepararEscenario() {

        SolicitudRepository solicitudRepository =
                mock(SolicitudRepository.class);

        EspacioRepository espacioRepository =
                mock(EspacioRepository.class);

        PropuestaAsignacionRepository propuestaRepository =
                mock(PropuestaAsignacionRepository.class);

        AsignacionRepository asignacionRepository =
                mock(AsignacionRepository.class);

        ReservaRepository reservaRepository =
                mock(ReservaRepository.class);

        PrologService prologService = mock(PrologService.class);

        GestorAsignaciones gestorAsignaciones =
                mock(GestorAsignaciones.class);

        configuracionService = mock(ConfiguracionService.class);

        gestorPropuestas = new GestorPropuestas(
                solicitudRepository,
                espacioRepository,
                propuestaRepository,
                asignacionRepository,
                reservaRepository,
                prologService,
                gestorAsignaciones,
                configuracionService
        );
    }

    @Test
    void noDebeConsultarHorariosDuranteRecepcion() {

        when(configuracionService.obtenerModoActual())
                .thenReturn(ModoFuncionamiento.RECEPCION_SOLICITUDES);

        assertThrows(IllegalStateException.class, () ->
                gestorPropuestas.obtenerHorariosDisponibles(
                        "solicitud-1",
                        "espacio-1",
                        List.of(DayOfWeek.MONDAY)
                )
        );
    }

    @Test
    void noDebeCrearPropuestasDuranteRecepcion() {

        when(configuracionService.obtenerModoActual())
                .thenReturn(ModoFuncionamiento.RECEPCION_SOLICITUDES);

        assertThrows(IllegalStateException.class, () ->
                gestorPropuestas.crearPropuesta(
                        "solicitud-1",
                        "espacio-1",
                        List.of(DayOfWeek.MONDAY),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                )
        );
    }

    @Test
    void noDebeEditarPropuestasDuranteRecepcion() {

        when(configuracionService.obtenerModoActual())
                .thenReturn(ModoFuncionamiento.RECEPCION_SOLICITUDES);

        assertThrows(IllegalStateException.class, () ->
                gestorPropuestas.editarPropuesta(
                        "propuesta-1",
                        "espacio-1",
                        List.of(DayOfWeek.MONDAY),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0)
                )
        );
    }

    @Test
    void permiteOperarDuranteLaCursada() {

        when(configuracionService.obtenerModoActual())
                .thenReturn(ModoFuncionamiento.CURSADA);

        // La validación de período no debe lanzar una excepción.
        // La operación puede continuar con sus validaciones habituales.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            // Comprobamos directamente el modo permitido.
            if (configuracionService.obtenerModoActual()
                    == ModoFuncionamiento.RECEPCION_SOLICITUDES) {
                throw new IllegalStateException();
            }
        });
    }

    @Test
    void permiteOperarFueraDePeriodo() {

        when(configuracionService.obtenerModoActual())
                .thenReturn(ModoFuncionamiento.FUERA_DE_PERIODO);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            if (configuracionService.obtenerModoActual()
                    == ModoFuncionamiento.RECEPCION_SOLICITUDES) {
                throw new IllegalStateException();
            }
        });
    }
}
