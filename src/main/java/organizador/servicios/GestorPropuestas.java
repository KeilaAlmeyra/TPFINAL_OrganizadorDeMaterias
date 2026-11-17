
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
}
