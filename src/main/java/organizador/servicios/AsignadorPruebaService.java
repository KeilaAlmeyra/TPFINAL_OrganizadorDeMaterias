package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Materia;
import organizador.modelos.RegistroCumplimiento;
import organizador.modelos.Reserva;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.AsignacionRepository;
import organizador.repositorios.MateriaRepository;
import organizador.repositorios.RegistroCumplimientoRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.SolicitudRepository;
import organizador.repositorios.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Conecta las solicitudes almacenadas en H2 con los gestores existentes.
 * Es un servicio adicional: no modifica las clases del proyecto.
 */
@Service
public class AsignadorPruebaService {

    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final SolicitudRepository solicitudes;
    private final AsignacionRepository asignaciones;
    private final RegistroCumplimientoRepository cumplimientos;
    private final ReservaRepository reservas;
    private final GestorEspacios gestorEspacios;
    private final GestorSolicitudes gestorSolicitudes;
    private final CalculadorPrioridad calculadorPrioridad;
    private final GestorAsignaciones gestorAsignaciones;

    public AsignadorPruebaService(UsuarioRepository usuarios,
                                 MateriaRepository materias,
                                 SolicitudRepository solicitudes,
                                 AsignacionRepository asignaciones,
                                 RegistroCumplimientoRepository cumplimientos,
                                 ReservaRepository reservas,
                                 GestorEspacios gestorEspacios,
                                 GestorSolicitudes gestorSolicitudes,
                                 CalculadorPrioridad calculadorPrioridad,
                                 GestorAsignaciones gestorAsignaciones) {
        this.usuarios = usuarios;
        this.materias = materias;
        this.solicitudes = solicitudes;
        this.asignaciones = asignaciones;
        this.cumplimientos = cumplimientos;
        this.reservas = reservas;
        this.gestorEspacios = gestorEspacios;
        this.gestorSolicitudes = gestorSolicitudes;
        this.calculadorPrioridad = calculadorPrioridad;
        this.gestorAsignaciones = gestorAsignaciones;
    }

    /** Valida y guarda una nueva solicitud con estado PENDIENTE. */
    @Transactional
    public void crearSolicitud(String idProfesor, String idMateria,
                              int cantidadAlumnos, List<DayOfWeek> dias,
                              LocalTime horaInicio, LocalTime horaFin,
                              String tipoActividad) {
        Usuario profesor = usuarios.findById(idProfesor)
                .orElseThrow(() -> new IllegalArgumentException("Profesor inexistente."));
        Materia materia = materias.findById(idMateria)
                .orElseThrow(() -> new IllegalArgumentException("Materia inexistente."));

        Solicitud solicitud = new Solicitud(
                UUID.randomUUID().toString(), profesor, materia,
                cantidadAlumnos, dias, horaInicio, horaFin, tipoActividad);

        String error = gestorSolicitudes.validarSolicitud(solicitud);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        solicitudes.save(solicitud);
    }

    /**
     * Procesa todas las pendientes juntas, ordenando primero por año y
     * luego por puntaje. Guarda las asignaciones en la misma transacción.
     */
    @Transactional
    public ResultadoProcesamiento procesarPendientes() {
        List<Solicitud> pendientes = solicitudes.findAll().stream()
                .filter(Solicitud::estaPendiente)
                .toList();
        List<RegistroCumplimiento> historial = cumplimientos.findAll();
        List<Solicitud> ordenadas = calculadorPrioridad
                .ordenarSolicitudes(pendientes, historial);
        List<Espacio> espacios = gestorEspacios.obtenerEspacios();
        List<Asignacion> ocupadas = new ArrayList<>(asignaciones.findAll());
        List<Reserva> reservasDirectas = reservas.findAll();

        int aprobadas = 0;
        int rechazadas = 0;

        for (Solicitud solicitud : ordenadas) {
            String error = gestorSolicitudes.validarSolicitud(solicitud);
            if (error != null) {
                solicitud.cambiarEstado(Solicitud.RECHAZADA);
                solicitudes.save(solicitud);
                rechazadas++;
                continue;
            }

            solicitud.setPuntajePrioridad(
                    calculadorPrioridad.calcularPuntajeSecundario(solicitud, historial));

            // Como las asignaciones son semanales y las reservas directas tienen
            // fecha, se excluye conservadoramente cualquier aula con una reserva
            // futura que coincida en día de semana y horario.
            List<Espacio> candidatos = espacios.stream()
                    .filter(espacio -> reservasDirectas.stream()
                            .noneMatch(reserva -> ocupaReservaDirecta(
                                    solicitud, espacio, reserva)))
                    .toList();

            Espacio elegido = gestorAsignaciones.buscarEspacioCompatible(
                    solicitud, candidatos, ocupadas);

            if (elegido == null) {
                solicitud.cambiarEstado(Solicitud.RECHAZADA);
                solicitudes.save(solicitud);
                rechazadas++;
                continue;
            }

            Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                    UUID.randomUUID().toString(), solicitud, elegido);
            solicitudes.save(solicitud);
            asignaciones.save(asignacion);
            ocupadas.add(asignacion); // Impide asignar dos veces la misma aula.
            aprobadas++;
        }
        return new ResultadoProcesamiento(aprobadas, rechazadas);
    }

    private boolean ocupaReservaDirecta(Solicitud solicitud,
                                        Espacio espacio, Reserva reserva) {
        return reserva.getEspacio().getId().equals(espacio.getId())
                && !reserva.getFecha().isBefore(LocalDate.now())
                && solicitud.getDias().contains(reserva.getFecha().getDayOfWeek())
                && solicitud.getHoraInicio().isBefore(reserva.getHoraFin())
                && reserva.getHoraInicio().isBefore(solicitud.getHoraFin());
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerProfesores() {
        return usuarios.findAll().stream()
                .filter(Usuario::esProfesor)
                .sorted(Comparator.comparing(Usuario::getNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Materia> obtenerMaterias() {
        return materias.findAll().stream()
                .sorted(Comparator.comparing(Materia::getNombre))
                .toList();
    }

    /** Prepara datos simples para que Thymeleaf no acceda a listas JPA diferidas. */
    @Transactional(readOnly = true)
    public List<FilaSolicitud> obtenerResultados() {
        Map<String, String> aulasPorSolicitud = asignaciones.findAll().stream()
                .collect(Collectors.toMap(
                        a -> a.getSolicitud().getId(),
                        a -> a.getEspacio().getNombre(),
                        (anterior, nuevo) -> anterior));

        return solicitudes.findAll().stream()
                .sorted(Comparator.comparing(Solicitud::getId))
                .map(s -> new FilaSolicitud(
                        s.getId(), s.getProfesor().getNombre(),
                        s.getMateria().getNombre(), s.getMateria().getAnioCarrera(),
                        s.getDias().stream().map(DayOfWeek::toString)
                                .collect(Collectors.joining(", ")),
                        s.getHoraInicio(), s.getHoraFin(),
                        s.getEstadoSolicitud(), s.getPuntajePrioridad(),
                        aulasPorSolicitud.getOrDefault(s.getId(), "-")
                ))
                .toList();
    }

    public record ResultadoProcesamiento(int aprobadas, int rechazadas) { }

    public record FilaSolicitud(String id, String profesor, String materia,
                               int anio, String dias, LocalTime inicio,
                               LocalTime fin, String estado, double puntaje,
                               String aula) { }
}
