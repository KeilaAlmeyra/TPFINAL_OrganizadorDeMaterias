
package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.PropuestaAsignacion;
import organizador.modelos.Reserva;
import organizador.modelos.Solicitud;
import organizador.repositorios.AsignacionRepository;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.PropuestaAsignacionRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.SolicitudRepository;
import organizador.modelos.ModoFuncionamiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.ArrayList;

/**
 * Representa el servicio encargado de la administración y ciclo de vida
 * de las propuestas alternativas de asignación.
 *
 * Permite formular nuevas ofertas de espacios y horarios para solicitudes
 * no resueltas, validar su factibilidad mediante Prolog y gestionar
 * la aceptación o rechazo por parte del docente responsable.
 */
@Service
public class GestorPropuestas {

    private final SolicitudRepository solicitudRepository;
    private final EspacioRepository espacioRepository;
    private final PropuestaAsignacionRepository propuestaRepository;
    private final AsignacionRepository asignacionRepository;
    private final ReservaRepository reservaRepository;
    private final PrologService prologService;

    private final GestorAsignaciones gestorAsignaciones;

    private final ConfiguracionService configuracionService;

    /**
     * Inicializa el servicio inyectando los repositorios de persistencia
     * y los servicios colaboradores requeridos.
     *
     * @param solicitudRepository Repositorio de solicitudes.
     * @param espacioRepository Repositorio de espacios físicos.
     * @param propuestaRepository Repositorio de propuestas de asignación.
     * @param asignacionRepository Repositorio de asignaciones confirmadas.
     * @param reservaRepository Repositorio de reservas de espacios.
     * @param prologService Servicio encargado de validar reglas y disponibilidad mediante Prolog.
     * @param gestorAsignaciones Servicio auxiliar para formalizar y crear asignaciones.
     */
    public GestorPropuestas(
            SolicitudRepository solicitudRepository,
            EspacioRepository espacioRepository,
            PropuestaAsignacionRepository propuestaRepository,
            AsignacionRepository asignacionRepository,
            ReservaRepository reservaRepository,
            PrologService prologService,
            GestorAsignaciones gestorAsignaciones,
            ConfiguracionService configuracionService) {

        this.solicitudRepository = solicitudRepository;
        this.espacioRepository = espacioRepository;
        this.propuestaRepository = propuestaRepository;
        this.asignacionRepository = asignacionRepository;
        this.reservaRepository = reservaRepository;
        this.prologService = prologService;
        this.gestorAsignaciones = gestorAsignaciones;
        this.configuracionService = configuracionService;
    }

    /**
     * Obtiene los espacios compatibles con los requisitos originales
     * de una solicitud pendiente.
     */
    @Transactional(readOnly = true)
    public List<Espacio> obtenerEspaciosCompatibles(String solicitudId) {

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalArgumentException(
                    "La solicitud ya no está pendiente.");
        }

        String tipoRequerido =
                solicitud.getMateria().getTipoEspacioFrecuente();

        int cantidadAlumnos = solicitud.getCantidadAlumnos();

        return espacioRepository.findAll().stream()
                .filter(espacio -> espacio.esCompatible(
                        tipoRequerido,
                        cantidadAlumnos
                ))
                .toList();
    }

    /**
     * Impide gestionar propuestas alternativas durante la recepción
     * de solicitudes semestrales.
     */
    private void validarPeriodoParaPropuestas() {

        if (configuracionService.obtenerModoActual()
                == ModoFuncionamiento.RECEPCION_SOLICITUDES) {

            throw new IllegalStateException(
                    "No se pueden gestionar propuestas alternativas "
                            + "durante el período de recepción de solicitudes.");
        }
    }

    /**
     * Busca los horarios disponibles para un espacio y varios días.
     *
     * Conserva la duración original de la solicitud.
     * Los inicios posibles son cada una hora, entre las 08:00 y las 22:00.
     * El horario debe finalizar como máximo a las 23:00.
     *
     * Prolog comprueba que el espacio esté libre en todos los días.
     */
    @Transactional(readOnly = true)
    public List<HorarioDisponible> obtenerHorariosDisponibles(
            String solicitudId,
            String espacioId,
            List<DayOfWeek> dias) {

        validarPeriodoParaPropuestas();

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalArgumentException(
                    "La solicitud ya no está pendiente.");
        }

        if (dias == null || dias.isEmpty()) {
            return List.of();
        }

        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el espacio seleccionado."));

        if (!espacio.esCompatible(
                solicitud.getMateria().getTipoEspacioFrecuente(),
                solicitud.getCantidadAlumnos())) {
            return List.of();
        }

        Duration duracion = Duration.between(
                solicitud.getHoraInicio(),
                solicitud.getHoraFin()
        );

        if (duracion.isZero() || duracion.isNegative()) {
            return List.of();
        }

        List<Asignacion> asignaciones = asignacionRepository.findAll();
        List<Reserva> reservas = reservaRepository.findAll();

        List<HorarioDisponible> horarios = new ArrayList<>();

        for (int hora = 8; hora <= 22; hora++) {

            LocalTime inicio = LocalTime.of(hora, 0);
            LocalTime fin = inicio.plus(duracion);

            if (fin.isAfter(LocalTime.of(23, 0))) {
                continue;
            }

            boolean disponible = prologService.validarDisponibilidadPropuesta(
                    solicitud,
                    espacio,
                    dias,
                    inicio,
                    fin,
                    asignaciones,
                    reservas
            );

            if (disponible) {
                horarios.add(new HorarioDisponible(inicio, fin));
            }
        }

        return horarios;
    }

    /**
     * Representa una franja horaria disponible.
     */
    public record HorarioDisponible(
            LocalTime inicio,
            LocalTime fin) {
    }
    /**
     * Crea y persiste una propuesta de asignación alternativa
     * para una solicitud académica pendiente, comprobando su
     * compatibilidad y disponibilidad mediante Prolog.
     *
     * Precondiciones:
     * - La solicitud y el espacio deben existir.
     * - La solicitud debe encontrarse pendiente.
     * - Los días y horarios deben definir un rango válido.
     * - El espacio debe ser compatible y estar disponible.
     *
     * @param solicitudId Identificador de la solicitud.
     * @param espacioId Identificador del espacio físico ofrecido.
     * @param dias Días de la semana propuestos.
     * @param horaInicio Horario de inicio propuesto.
     * @param horaFin Horario de finalización propuesto.
     * @return La propuesta creada y persistida.
     *
     * Postcondiciones:
     * - La propuesta queda almacenada en estado pendiente.
     * - La solicitud original conserva sus datos y estado.
     *
     * @throws IllegalArgumentException Si no existe la solicitud o el
     * espacio, si la solicitud no está pendiente o si no hay disponibilidad.
     * @throws IllegalStateException Si ya existe una propuesta pendiente
     * para esa solicitud.
     */
    @Transactional
    public PropuestaAsignacion crearPropuesta(
            String solicitudId,
            String espacioId,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {
        validarPeriodoParaPropuestas();

        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        if (!solicitud.estaPendiente()) {
            throw new IllegalArgumentException(
                    "Solo se pueden proponer alternativas para solicitudes pendientes.");
        }

        if (propuestaRepository.existsBySolicitud_IdAndEstado(
                solicitudId,
                PropuestaAsignacion.PENDIENTE)) {

            throw new IllegalStateException(
                    "Esta solicitud ya tiene una propuesta esperando respuesta del profesor.");
        }

        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el espacio seleccionado."));

        List<Asignacion> asignaciones = asignacionRepository.findAll();
        List<Reserva> reservas = reservaRepository.findAll();

        boolean disponible = prologService.validarDisponibilidadPropuesta(
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin,
                asignaciones,
                reservas
        );

        if (!disponible) {
            throw new IllegalArgumentException(
                    "El espacio no es compatible o no está disponible "
                            + "en los días y horarios propuestos.");
        }

        PropuestaAsignacion propuesta = new PropuestaAsignacion(
                UUID.randomUUID().toString(),
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin
        );

        return propuestaRepository.save(propuesta);
    }

    /**
     * Obtiene únicamente las propuestas pendientes de respuesta
     * correspondientes a un profesor.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacion> obtenerPropuestasPendientesProfesor(
            String profesorId) {

        return propuestaRepository.findBySolicitud_Profesor_IdAndEstado(
                profesorId,
                PropuestaAsignacion.PENDIENTE
        );
    }

    /**
     * Obtiene todas las propuestas que esperan respuesta del profesor.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacion> obtenerTodasLasPropuestasPendientes() {

        return propuestaRepository.findByEstado(
                PropuestaAsignacion.PENDIENTE
        );
    }
    /**
     * Obtiene las propuestas que ya fueron aceptadas o rechazadas.
     * Se conservan para consultar el historial.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacion> obtenerHistorialPropuestasProfesor(
            String profesorId) {

        return propuestaRepository.findBySolicitud_Profesor_IdAndEstadoNot(
                profesorId,
                PropuestaAsignacion.PENDIENTE
        );
    }

    /**
     * Obtiene todas las propuestas de un profesor.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacion> obtenerPropuestasProfesor(
            String profesorId) {

        return propuestaRepository.findBySolicitud_Profesor_Id(profesorId);
    }

    /**
     * Acepta una propuesta de asignación alternativa y genera
     * la asignación definitiva para la solicitud correspondiente.
     *
     * Precondiciones:
     * - La propuesta debe existir y encontrarse pendiente.
     * - El profesor debe ser el propietario de la solicitud.
     * - La solicitud original debe continuar pendiente.
     * - El espacio debe seguir disponible en los días y horarios propuestos.
     *
     * Postcondiciones:
     * - La propuesta queda marcada como aceptada.
     * - La solicitud original se actualiza mediante el proceso de asignación.
     * - Se crea y persiste una nueva asignación definitiva.
     *
     * @param propuestaId Identificador de la propuesta.
     * @param profesorId Identificador del profesor que responde.
     * @throws IllegalArgumentException Si la propuesta no existe o el
     * profesor no tiene permiso para responderla.
     * @throws IllegalStateException Si la propuesta ya fue respondida,
     * la solicitud dejó de estar pendiente o el espacio ya no está disponible.
     */
    @Transactional
    public void aceptarPropuesta(String propuestaId, String profesorId) {

        PropuestaAsignacion propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la propuesta."));

        if (!propuesta.estaPendiente()) {
            throw new IllegalStateException(
                    "Esta propuesta ya fue respondida.");
        }

        Solicitud solicitud = propuesta.getSolicitud();

        if (!solicitud.getProfesor().getId().equals(profesorId)) {
            throw new IllegalArgumentException(
                    "No tenés permiso para responder esta propuesta.");
        }

        if (!solicitud.estaPendiente()) {
            throw new IllegalStateException(
                    "La solicitud original ya no está pendiente.");
        }

        List<Asignacion> asignaciones = asignacionRepository.findAll();
        List<Reserva> reservas = reservaRepository.findAll();

        boolean disponible = prologService.validarDisponibilidadPropuesta(
                solicitud,
                propuesta.getEspacio(),
                propuesta.getDiasPropuestos(),
                propuesta.getHoraInicio(),
                propuesta.getHoraFin(),
                asignaciones,
                reservas
        );

        if (!disponible) {
            throw new IllegalStateException(
                    "El espacio ya no está disponible en el horario propuesto.");
        }

        Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                UUID.randomUUID().toString(),
                solicitud,
                propuesta.getEspacio(),
                propuesta.getDiasPropuestos(),
                propuesta.getHoraInicio(),
                propuesta.getHoraFin()
        );

        propuesta.aceptar();

        solicitudRepository.save(solicitud);
        propuestaRepository.save(propuesta);
        asignacionRepository.save(asignacion);
    }
    /**
     * Rechaza una propuesta alternativa sin modificar la solicitud original.
     *
     * Precondiciones:
     * - La propuesta debe existir y encontrarse pendiente.
     * - El profesor debe ser el propietario de la solicitud.
     *
     * Postcondiciones:
     * - La propuesta queda marcada como rechazada.
     * - La solicitud original conserva sus datos y estado.
     *
     * @param propuestaId Identificador de la propuesta.
     * @param profesorId Identificador del profesor que responde.
     * @throws IllegalArgumentException Si la propuesta no existe o el
     * profesor no tiene permiso para responderla.
     * @throws IllegalStateException Si la propuesta ya fue respondida.
     */
    @Transactional
    public void rechazarPropuesta(String propuestaId, String profesorId) {

        PropuestaAsignacion propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la propuesta."));

        if (!propuesta.estaPendiente()) {
            throw new IllegalStateException(
                    "Esta propuesta ya fue respondida.");
        }

        if (!propuesta.getSolicitud().getProfesor().getId().equals(profesorId)) {
            throw new IllegalArgumentException(
                    "No tenés permiso para responder esta propuesta.");
        }

        propuesta.rechazar();

        propuestaRepository.save(propuesta);
    }

    /**
     * Cancela una propuesta alternativa enviada por el administrador.
     *
     * La cancelación afecta únicamente a la propuesta y conserva
     * la solicitud original pendiente.
     *
     * @param propuestaId Identificador de la propuesta que se cancela.
     * @throws IllegalArgumentException Si no existe la propuesta.
     */
    @Transactional
    public void cancelarPropuesta(String propuestaId) {

        PropuestaAsignacion propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la propuesta."));

        propuesta.cancelar();

        propuestaRepository.save(propuesta);
    }

    /**
     * Modifica los datos de una propuesta que todavía está pendiente
     * de respuesta y vuelve a comprobar su compatibilidad y disponibilidad
     * mediante Prolog.
     *
     * Precondiciones:
     * - La propuesta debe existir y estar pendiente.
     * - El espacio debe cumplir los requisitos de la solicitud.
     * - El horario y los días propuestos deben estar disponibles.
     *
     * Postcondiciones:
     * - La propuesta conserva su identidad y solicitud original.
     * - Se actualizan el espacio, los días y el horario ofrecidos.
     *
     * @param propuestaId Identificador de la propuesta.
     * @param espacioId Identificador del nuevo espacio.
     * @param dias Nuevos días propuestos.
     * @param horaInicio Nuevo horario de inicio.
     * @param horaFin Nuevo horario de finalización.
     * @throws IllegalArgumentException Si no existe la propuesta o el
     * espacio, si el espacio no es compatible o no está disponible.
     * @throws IllegalStateException Si la propuesta ya no está pendiente.
     */
    @Transactional
    public void editarPropuesta(
            String propuestaId,
            String espacioId,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {
        validarPeriodoParaPropuestas();

        PropuestaAsignacion propuesta = propuestaRepository.findById(propuestaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la propuesta."));

        if (!propuesta.estaPendiente()) {
            throw new IllegalStateException(
                    "Solo se pueden editar propuestas pendientes.");
        }

        Solicitud solicitud = propuesta.getSolicitud();

        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el espacio seleccionado."));

        if (!espacio.esCompatible(
                solicitud.getMateria().getTipoEspacioFrecuente(),
                solicitud.getCantidadAlumnos())) {

            throw new IllegalArgumentException(
                    "El espacio no cumple los requisitos de la solicitud.");
        }

        List<Asignacion> asignaciones = asignacionRepository.findAll();
        List<Reserva> reservas = reservaRepository.findAll();

        boolean disponible = prologService.validarDisponibilidadPropuesta(
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin,
                asignaciones,
                reservas
        );

        if (!disponible) {
            throw new IllegalArgumentException(
                    "El espacio no está disponible en los días y horarios propuestos.");
        }

        propuesta.actualizar(
                espacio,
                dias,
                horaInicio,
                horaFin
        );

        propuestaRepository.save(propuesta);
    }
}
