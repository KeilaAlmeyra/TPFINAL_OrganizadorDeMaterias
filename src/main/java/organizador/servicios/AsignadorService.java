
package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Materia;
import organizador.modelos.ModoFuncionamiento;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AsignadorService {

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
    private final PrologService prologService;
    private final ConfiguracionService configuracionService;

    public AsignadorService(
            UsuarioRepository usuarios,
            MateriaRepository materias,
            SolicitudRepository solicitudes,
            AsignacionRepository asignaciones,
            RegistroCumplimientoRepository cumplimientos,
            ReservaRepository reservas,
            GestorEspacios gestorEspacios,
            GestorSolicitudes gestorSolicitudes,
            CalculadorPrioridad calculadorPrioridad,
            GestorAsignaciones gestorAsignaciones,
            PrologService prologService,
            ConfiguracionService configuracionService) {

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
        this.prologService = prologService;
        this.configuracionService = configuracionService;
    }

    /**
     * Recibe una solicitud y determina qué hacer según
     * el modo de funcionamiento del sistema.
     * <p>
     * En planificación:
     * Guarda la solicitud como pendiente.
     * <p>
     * En cursada:
     * Guarda y evalúa individualmente la solicitud.
     */
    @Transactional
    public ModoFuncionamiento crearSolicitud(
            String idProfesor,
            String idMateria,
            int cantidadAlumnos,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            String tipoActividad) {

        if (!Solicitud.CLASE_REGULAR.equals(tipoActividad)) {
            throw new IllegalArgumentException(
                    "Solo se pueden solicitar asignaciones para clases regulares. "
                            + "Los exámenes y parciales deben gestionarse mediante reservas."
            );
        }

        ModoFuncionamiento modo =
                configuracionService.obtenerModoActual();

        if (modo == ModoFuncionamiento.FUERA_DE_PERIODO) {
            throw new IllegalArgumentException(
                    "No se pueden presentar solicitudes fuera del período académico.");
        }

        Usuario profesor = usuarios.findById(idProfesor)
                .orElseThrow(() ->
                        new IllegalArgumentException("Profesor inexistente."));

        if (!profesor.esProfesor()) {
            throw new IllegalArgumentException(
                    "La solicitud debe estar asociada a un profesor."
            );
        }

        Materia materia = materias.findById(idMateria)
                .orElseThrow(() ->
                        new IllegalArgumentException("Materia inexistente."));

        Solicitud solicitud = new Solicitud(
                UUID.randomUUID().toString(),
                profesor,
                materia,
                cantidadAlumnos,
                dias,
                horaInicio,
                horaFin,
                tipoActividad
        );

        String error = gestorSolicitudes.validarSolicitud(solicitud);

        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        // Guardamos primero la solicitud.
        solicitudes.save(solicitud);

        if (modo == ModoFuncionamiento.CURSADA) {
            // Durante la cursada se evalúa solamente esta solicitud.
            procesarSolicitudIndividual(solicitud);
        }

        // En recepción queda pendiente hasta el cierre.
        return modo;
    }

    /**
     * Procesa una única solicitud durante la cursada.
     * <p>
     * Prolog comprueba compatibilidad y disponibilidad.
     * Si no encuentra espacio, la solicitud permanece pendiente.
     */
    private void procesarSolicitudIndividual(Solicitud solicitud) {

        List<RegistroCumplimiento> historial = cumplimientos.findAll();

        solicitud.setPuntajePrioridad(
                calculadorPrioridad.calcularPuntajePrioridad(
                        solicitud, historial
                )
        );

        List<Espacio> espacios = gestorEspacios.obtenerEspacios();

        List<Asignacion> ocupadas = new ArrayList<>(
                asignaciones.findAll()
        );

        List<Reserva> reservasDirectas = reservas.findAll();

        Espacio elegido = prologService.buscarEspacioCompatible(
                solicitud,
                espacios,
                ocupadas,
                reservasDirectas
        );

        if (elegido == null) {
            // Permanece pendiente para una futura revisión.
            solicitudes.save(solicitud);
            return;
        }

        Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                UUID.randomUUID().toString(),
                solicitud,
                elegido
        );

        solicitudes.save(solicitud);
        asignaciones.save(asignacion);
    }



    /**
     * Procesa todas las solicitudes pendientes juntas.
     * <p>
     * Se utiliza al finalizar la recepción y también puede
     * ser ejecutado por un administrador durante la cursada
     * para volver a intentar asignar solicitudes pendientes.
     */
    @Transactional
    public ResultadoProcesamiento procesarPendientes() {

        ModoFuncionamiento modo =
                configuracionService.obtenerModoActual();

        if (modo == ModoFuncionamiento.RECEPCION_SOLICITUDES) {
            throw new IllegalStateException(
                    "Todavía está abierto el período de recepción. "
                            + "Las solicitudes se procesarán al finalizar."
            );
        }

        if (modo == ModoFuncionamiento.FUERA_DE_PERIODO) {
            throw new IllegalStateException(
                    "No se pueden procesar solicitudes fuera del período académico."
            );
        }

        List<Solicitud> pendientes = solicitudes.findAll().stream()
                .filter(Solicitud::estaPendiente)
                .toList();

        List<RegistroCumplimiento> historial = cumplimientos.findAll();

        List<Solicitud> ordenadas = calculadorPrioridad
                .ordenarSolicitudes(pendientes, historial);

        List<Espacio> espacios = gestorEspacios.obtenerEspacios();

        List<Asignacion> ocupadas = new ArrayList<>(
                asignaciones.findAll()
        );

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
                    calculadorPrioridad.calcularPuntajePrioridad(
                            solicitud, historial
                    )
            );

            // Prolog evalúa tipo, capacidad y conflictos horarios.
            Espacio elegido = prologService.buscarEspacioCompatible(
                    solicitud,
                    espacios,
                    ocupadas,
                    reservasDirectas
            );

            if (elegido == null) {
                // No hay espacio compatible por ahora.
                // Guardamos el puntaje calculado y la solicitud
                // permanece pendiente para futuros reintentos.
                solicitudes.save(solicitud);
                continue;
            }

            Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                    UUID.randomUUID().toString(),
                    solicitud,
                    elegido
            );

            solicitudes.save(solicitud);
            asignaciones.save(asignacion);

            // La nueva asignación bloquea el espacio para
            // las siguientes solicitudes del mismo procesamiento.
            ocupadas.add(asignacion);

            aprobadas++;
        }

        return new ResultadoProcesamiento(aprobadas, rechazadas);
    }



    /**
     * Vuelve a intentar asignar las solicitudes pendientes
     * durante el período de cursada.
     * <p>
     * A diferencia de la planificación inicial, si Prolog
     * no encuentra un espacio, la solicitud sigue pendiente.
     */
    @Transactional
    public ResultadoProcesamiento reintentarPendientes() {

        ModoFuncionamiento modo =
                configuracionService.obtenerModoActual();

        if (modo != ModoFuncionamiento.CURSADA) {
            throw new IllegalStateException(
                    "Los pendientes solo se pueden reintentar durante la cursada."
            );
        }

        List<Solicitud> pendientes = solicitudes.findAll().stream()
                .filter(Solicitud::estaPendiente)
                .toList();

        List<RegistroCumplimiento> historial = cumplimientos.findAll();

        List<Solicitud> ordenadas = calculadorPrioridad
                .ordenarSolicitudes(pendientes, historial);

        List<Espacio> espacios = gestorEspacios.obtenerEspacios();

        List<Asignacion> ocupadas = new ArrayList<>(
                asignaciones.findAll()
        );

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
                    calculadorPrioridad.calcularPuntajePrioridad(
                            solicitud, historial
                    )
            );

            Espacio elegido = prologService.buscarEspacioCompatible(
                    solicitud,
                    espacios,
                    ocupadas,
                    reservasDirectas
            );

            if (elegido == null) {
                // No hay espacio por ahora.
                // Guardamos el puntaje y mantenemos la solicitud pendiente.
                solicitudes.save(solicitud);
                continue;
            }

            Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                    UUID.randomUUID().toString(),
                    solicitud,
                    elegido
            );

            solicitudes.save(solicitud);
            asignaciones.save(asignacion);

            // Evita ofrecer el mismo espacio a otra solicitud
            // que se superponga durante este mismo reintento.
            ocupadas.add(asignacion);

            aprobadas++;
        }

        return new ResultadoProcesamiento(aprobadas, rechazadas);
    }

    /**
     * Obtiene los profesores registrados.
     */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerProfesores() {

        return usuarios.findAll().stream()
                .filter(Usuario::esProfesor)
                .sorted(Comparator.comparing(Usuario::getNombre))
                .toList();
    }

    /**
     * Obtiene las materias registradas.
     */
    @Transactional(readOnly = true)
    public List<Materia> obtenerMaterias() {

        return materias.findAll().stream()
                .sorted(Comparator.comparing(Materia::getNombre))
                .toList();
    }

    /**
     * Prepara datos simples para que Thymeleaf
     * no acceda a listas JPA diferidas.
     */
    @Transactional(readOnly = true)
    public List<FilaSolicitud> obtenerResultados() {
        return obtenerResultados(null, true);
    }

    @Transactional(readOnly = true)
    public List<FilaSolicitud> obtenerResultados(
            String idProfesor,
            boolean esAdministrador) {

        Map<String, String> aulasPorSolicitud = asignaciones.findAll().stream()
                .collect(Collectors.toMap(
                        a -> a.getSolicitud().getId(),
                        a -> a.getEspacio().getNombre(),
                        (anterior, nuevo) -> anterior
                ));

        return solicitudes.findAll().stream()

                // El administrador ve todas.
                // El profesor solamente las que presentó él.
                .filter(s -> esAdministrador
                        || (idProfesor != null
                        && s.getProfesor().getId().equals(idProfesor)))

                .sorted(Comparator.comparing(Solicitud::getId))

                .map(s -> new FilaSolicitud(
                        s.getId(),
                        s.getProfesor().getNombre(),
                        s.getMateria().getNombre(),
                        s.getMateria().getAnioCarrera(),
                        s.getCantidadAlumnos(),
                        s.getMateria().getTipoEspacioFrecuente(),
                        s.getDias().stream()
                                .map(DayOfWeek::toString)
                                .collect(Collectors.joining(", ")),
                        s.getHoraInicio(),
                        s.getHoraFin(),
                        s.getEstadoSolicitud(),
                        s.getPuntajePrioridad(),
                        aulasPorSolicitud.getOrDefault(s.getId(), "-")
                ))
                .toList();
    }

    public record ResultadoProcesamiento(
            int aprobadas,
            int rechazadas) {
    }


    /**
     * Obtiene las asignaciones del profesor actual.
     * El administrador puede consultar las de todos.
     */
    @Transactional(readOnly = true)
    public List<FilaAsignacion> obtenerAsignaciones(
            String idProfesor,
            boolean esAdministrador) {

        List<Asignacion> resultado = esAdministrador
                ? asignaciones.findAll()
                : asignaciones.findBySolicitud_Profesor_Id(idProfesor);

        return resultado.stream()
                .sorted(Comparator.comparing(
                        (Asignacion a) -> a.getSolicitud().getMateria().getNombre()
                ))
                .map(a -> new FilaAsignacion(
                        a.getId(),
                        a.getSolicitud().getProfesor().getNombre(),
                        a.getSolicitud().getMateria().getNombre(),
                        a.getEspacio().getNombre(),
                        a.getDiasAsignados().stream()
                                .map(DayOfWeek::toString)
                                .collect(Collectors.joining(", ")),
                        a.getHoraInicioAsignada(),
                        a.getHoraFinAsignada(),
                        a.getEstadoAsignacion()
                ))
                .toList();
    }

    public record FilaAsignacion(
            String id,
            String profesor,
            String materia,
            String aula,
            String dias,
            LocalTime inicio,
            LocalTime fin,
            String estado
    ) {
    }


    public record FilaSolicitud(
            String id,
            String profesor,
            String materia,
            int anio,
            int cantidadAlumnos,
            String tipoEspacioRequerido,
            String dias,
            LocalTime inicio,
            LocalTime fin,
            String estado,
            double puntaje,
            String aula) {
    }
}
