package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.modelos.Materia;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.AsignacionRepository;
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
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas con una H2 en memoria: no cambian la base de datos real. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:asignador_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class AsignadorPruebaServiceTest {
    @Autowired private AsignadorPruebaService asignador;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private MateriaRepository materias;
    @Autowired private EspacioRepository espacios;
    @Autowired private SolicitudRepository solicitudes;
    @Autowired private AsignacionRepository asignaciones;
    @Autowired private RegistroCumplimientoRepository cumplimientos;
    @Autowired private ReservaRepository reservas;

    @BeforeEach
    void prepararEscenario() {
        // Respetar el orden de las claves foráneas.
        cumplimientos.deleteAll();
        asignaciones.deleteAll();
        solicitudes.deleteAll();
        reservas.deleteAll();
        materias.deleteAll();
        usuarios.deleteAll();
        espacios.deleteAll();

        usuarios.save(new Usuario("TEST-P1", "Profesor primer año",
                "primero@example.com", "profesor"));
        usuarios.save(new Usuario("TEST-P3", "Profesor tercer año",
                "tercero@example.com", "profesor"));
        materias.save(new Materia("TEST-M1", "Materia primer año",
                4, true, 1, "aula"));
        materias.save(new Materia("TEST-M3", "Materia tercer año",
                4, true, 3, "aula"));
        espacios.save(new Espacio("TEST-AULA", "Aula de prueba", 30,
                "aula", "disponible"));
    }

    @Test
    void elPrimerAnioTienePrioridadAunqueSoliciteDespues() {
        // Tercer año pide primero, con examen final (más puntos secundarios).
        asignador.crearSolicitud("TEST-P3", "TEST-M3", 20,
                List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0),
                LocalTime.of(16, 0), Solicitud.EXAMEN_FINAL);
        // Primer año pide después, con clase regular.
        asignador.crearSolicitud("TEST-P1", "TEST-M1", 20,
                List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0),
                LocalTime.of(16, 0), Solicitud.CLASE_REGULAR);

        var resultado = asignador.procesarPendientes();

        assertEquals(1, resultado.aprobadas());
        assertEquals(1, resultado.rechazadas());
        assertEquals(1, asignaciones.count());
        assertEquals("TEST-P1", asignaciones.findAll().get(0)
                .getSolicitud().getProfesor().getId());
    }

    @Test
    void permiteReutilizarElAulaCuandoLosHorariosNoSeSuperponen() {
        asignador.crearSolicitud("TEST-P1", "TEST-M1", 20,
                List.of(DayOfWeek.WEDNESDAY), LocalTime.of(14, 0),
                LocalTime.of(16, 0), Solicitud.CLASE_REGULAR);
        asignador.crearSolicitud("TEST-P3", "TEST-M3", 20,
                List.of(DayOfWeek.WEDNESDAY), LocalTime.of(16, 0),
                LocalTime.of(18, 0), Solicitud.CLASE_REGULAR);

        var resultado = asignador.procesarPendientes();

        assertEquals(2, resultado.aprobadas());
        assertEquals(0, resultado.rechazadas());
        assertEquals(2, asignaciones.count());
    }

    @Test
    void noAsignaUnAulaSinCapacidadSuficiente() {
        asignador.crearSolicitud("TEST-P1", "TEST-M1", 31,
                List.of(DayOfWeek.TUESDAY), LocalTime.of(14, 0),
                LocalTime.of(16, 0), Solicitud.CLASE_REGULAR);

        var resultado = asignador.procesarPendientes();

        assertEquals(0, resultado.aprobadas());
        assertEquals(1, resultado.rechazadas());
        assertTrue(asignaciones.findAll().isEmpty());
    }
}
