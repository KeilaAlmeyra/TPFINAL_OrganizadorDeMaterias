
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.ArrayList;

@Service
public class GestorPropuestas {

    private final SolicitudRepository solicitudRepository;
    private final EspacioRepository espacioRepository;
    private final PropuestaAsignacionRepository propuestaRepository;
    private final AsignacionRepository asignacionRepository;
    private final ReservaRepository reservaRepository;
    private final PrologService prologService;

    private final GestorAsignaciones gestorAsignaciones;

    public GestorPropuestas(
            SolicitudRepository solicitudRepository,
            EspacioRepository espacioRepository,
            PropuestaAsignacionRepository propuestaRepository,
            AsignacionRepository asignacionRepository,
            ReservaRepository reservaRepository,
            PrologService prologService,
            GestorAsignaciones gestorAsignaciones) {

        this.solicitudRepository = solicitudRepository;
        this.espacioRepository = espacioRepository;
        this.propuestaRepository = propuestaRepository;
        this.asignacionRepository = asignacionRepository;
        this.reservaRepository = reservaRepository;
        this.prologService = prologService;
        this.gestorAsignaciones = gestorAsignaciones;
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
     * Crea una propuesta alternativa para una solicitud pendiente.
     */
    @Transactional
    public PropuestaAsignacion crearPropuesta(
            String solicitudId,
            String espacioId,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {

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
     * Obtiene las propuestas alternativas correspondientes a un profesor.
     */
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
     * Acepta una propuesta y genera la asignación definitiva.
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
     * Rechaza una propuesta sin modificar la solicitud original.
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
     * Cancela una propuesta enviada por el administrador.
     * La solicitud original permanece pendiente.
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
     * Modifica una propuesta pendiente y vuelve a validar
     * su compatibilidad y disponibilidad mediante Prolog.
     */
    @Transactional
    public void editarPropuesta(
            String propuestaId,
            String espacioId,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {

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
