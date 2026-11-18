
package organizador.servicios;

import organizador.modelos.Configuracion;
import organizador.modelos.Espacio;
import organizador.modelos.Materia;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.AsignacionRepository;
import organizador.repositorios.ConfiguracionRepository;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.MateriaRepository;
import organizador.repositorios.RegistroCumplimientoRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.SolicitudRepository;
import organizador.repositorios.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas con una H2 en memoria:
 * no modifican la base de datos real.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:asignador_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class AsignadorPruebaServiceTest {

    @Autowired
    private AsignadorPruebaService asignador;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private MateriaRepository materias;

    @Autowired
    private EspacioRepository espacios;

    @Autowired
    private SolicitudRepository solicitudes;

    @Autowired
    private AsignacionRepository asignaciones;

    @Autowired
    private RegistroCumplimientoRepository cumplimientos;

    @Autowired
    private ReservaRepository reservas;

    @Autowired
    private ConfiguracionRepository configuraciones;

    @BeforeEach
    void prepararEscenario() {

        // Limpiar los datos respetando las claves foráneas.
        cumplimientos.deleteAll();
        asignaciones.deleteAll();
        solicitudes.deleteAll();
        reservas.deleteAll();
        materias.deleteAll();
        usuarios.deleteAll();
        espacios.deleteAll();
        configuraciones.deleteAll();

        // Configuración inicial:
        // hoy estamos dentro del período de recepción.
        LocalDate hoy = LocalDate.now();

        configuraciones.save(new Configuracion(
                "principal",
                hoy,
                hoy,
                hoy,
                hoy.plusMonths(4)
        ));

        usuarios.save(new Usuario(
                "TEST-P1",
                "Profesor primer año",
                "primero@example.com",
                "profesor"
        ));

        usuarios.save(new Usuario(
                "TEST-P3",
                "Profesor tercer año",
                "tercero@example.com",
                "profesor"
        ));

        materias.save(new Materia(
                "TEST-M1",
                "Materia primer año",
                4,
                true,
                1,
                "aula"
        ));

        materias.save(new Materia(
                "TEST-M3",
                "Materia tercer año",
                4,
                true,
                3,
                "aula"
        ));

        espacios.save(new Espacio(
                "TEST-AULA",
                "Aula de prueba",
                30,
                "aula",
                "disponible"
        ));
    }

    /**
     * Simula que terminó la recepción de solicitudes.
     * La fecha actual sigue estando dentro del período de cursada.
     */
    private void cerrarRecepcion() {

        LocalDate hoy = LocalDate.now();

        configuraciones.save(new Configuracion(
                "principal",
                hoy.minusDays(1),
                hoy.minusDays(1),
                hoy.minusDays(1),
                hoy.plusMonths(4)
        ));
    }

    @Test
    void elPrimerAnioTienePrioridadAunqueSoliciteDespues() {

        // Tercer año solicita primero.
        asignador.crearSolicitud(
                "TEST-P3",
                "TEST-M3",
                20,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                Solicitud.CLASE_REGULAR
        );

        // Primer año solicita después.
        asignador.crearSolicitud(
                "TEST-P1",
                "TEST-M1",
                20,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                Solicitud.CLASE_REGULAR
        );

        // Finaliza la recepción y comienza el procesamiento.
        cerrarRecepcion();

        var resultado = asignador.procesarPendientes();

        assertEquals(1, resultado.aprobadas());
        assertEquals(0, resultado.rechazadas());
        assertEquals(1, asignaciones.count());

        assertEquals(
                "TEST-P1",
                asignaciones.findAll().get(0)
                        .getSolicitud()
                        .getProfesor()
                        .getId()
        );

        Solicitud solicitudTercerAnio = solicitudes.findAll().stream()
                .filter(s -> s.getProfesor().getId().equals("TEST-P3"))
                .findFirst()
                .orElseThrow();

        assertEquals(
                Solicitud.PENDIENTE,
                solicitudTercerAnio.getEstadoSolicitud()
        );
    }

    @Test
    void permiteReutilizarElAulaCuandoLosHorariosNoSeSuperponen() {

        asignador.crearSolicitud(
                "TEST-P1",
                "TEST-M1",
                20,
                List.of(DayOfWeek.WEDNESDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                Solicitud.CLASE_REGULAR
        );

        asignador.crearSolicitud(
                "TEST-P3",
                "TEST-M3",
                20,
                List.of(DayOfWeek.WEDNESDAY),
                LocalTime.of(16, 0),
                LocalTime.of(18, 0),
                Solicitud.CLASE_REGULAR
        );

        cerrarRecepcion();

        var resultado = asignador.procesarPendientes();

        assertEquals(2, resultado.aprobadas());
        assertEquals(0, resultado.rechazadas());
        assertEquals(2, asignaciones.count());
    }

    @Test
    void noAsignaUnAulaSinCapacidadSuficiente() {

        asignador.crearSolicitud(
                "TEST-P1",
                "TEST-M1",
                31,
                List.of(DayOfWeek.TUESDAY),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                Solicitud.CLASE_REGULAR
        );

        cerrarRecepcion();

        var resultado = asignador.procesarPendientes();

        assertEquals(0, resultado.aprobadas());
        assertEquals(0, resultado.rechazadas());
        assertTrue(asignaciones.findAll().isEmpty());

        Solicitud solicitud = solicitudes.findAll().get(0);

        assertEquals(
                Solicitud.PENDIENTE,
                solicitud.getEstadoSolicitud()
        );
    }
}
