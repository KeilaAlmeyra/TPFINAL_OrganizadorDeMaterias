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

/**
 * Representa el servicio encargado de la administración y ciclo de vida de las propuestas alternativas de asignación.
 * Permite formular nuevas ofertas de espacios y horarios para solicitudes no resueltas,
 * validar su factibilidad mediante el motor lógico Prolog y gestionar la aceptación o rechazo
 * por parte del docente responsable.
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

    /**
     * Objetivo:
     * Inicializar el servicio inyectando los repositorios de persistencia y los servicios colaboradores requeridos.
     *
     * Precondiciones:
     * - Todas las dependencias deben ser gestionadas e instanciadas correctamente por el contexto de Spring.
     *
     * Parámetros:
     * @param solicitudRepository Repositorio de acceso y persistencia para entidades {@link Solicitud}.
     * @param espacioRepository Repositorio de acceso a datos para entidades {@link Espacio}.
     * @param propuestaRepository Repositorio para la gestión de entidades {@link PropuestaAsignacion}.
     * @param asignacionRepository Repositorio de asignaciones confirmadas en el sistema.
     * @param reservaRepository Repositorio de reservas puntuales de espacios.
     * @param prologService Servicio de consulta de reglas lógicas y disponibilidad en Prolog.
     * @param gestorAsignaciones Servicio auxiliar para la formalización y creación de asignaciones.
     *
     * Retorno:
     * Instancia configurada de {@link GestorPropuestas}.
     *
     * Postcondiciones:
     * - Las dependencias quedan asociadas a los campos inmutables del servicio.
     */
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
     * Objetivo:
     * Crear y persistir una propuesta de asignación alternativa (espacio, días u horarios diferentes)
     * para una solicitud académica pendiente, comprobando su compatibilidad y disponibilidad contra las
     * reservas y asignaciones vigentes mediante Prolog.
     *
     * Precondiciones:
     * - {@code solicitudId} y {@code espacioId} deben corresponder a entidades existentes en la base de datos.
     * - La solicitud debe encontrarse en estado pendiente ({@code solicitud.estaPendiente() == true}).
     * - {@code dias}, {@code horaInicio} y {@code horaFin} deben definir un rango temporal válido.
     * - El espacio debe ser compatible y estar libre de superposiciones para los días y horarios indicados.
     *
     * Parámetros:
     * @param solicitudId Identificador único (String) de la solicitud para la cual se plantea la alternativa.
     * @param espacioId Identificador único (String) del espacio físico ofrecido.
     * @param dias Lista de días de la semana ({@link List}<{@link DayOfWeek}>) propuestos.
     * @param horaInicio Horario de inicio ({@link LocalTime}) propuesto.
     * @param horaFin Horario de finalización ({@link LocalTime}) propuesto.
     *
     * Retorno:
     * @return {@link PropuestaAsignacion} Entidad de la propuesta creada y persistida en la base de datos.
     *
     * Postcondiciones:
     * - Se almacena un nuevo registro de {@link PropuestaAsignacion} en la base de datos en estado pendiente.
     * - La solicitud original permanece en estado pendiente sin mutar sus datos propios.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si no se encuentra la solicitud o el espacio, si la solicitud no está pendiente,
     *                                  o si el espacio no resulta compatible/disponible según la validación de Prolog.
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
     * Objetivo:
     * Recuperar el listado de propuestas de asignación asociadas a las solicitudes de un profesor específico.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible para operaciones de lectura.
     * - {@code profesorId} no debe ser nulo.
     *
     * Parámetros:
     * @param profesorId Identificador único (String) del profesor cuyas propuestas se desean consultar.
     *
     * Retorno:
     * @return {@link List}<{@link PropuestaAsignacion}> Lista de propuestas vinculadas al docente. Retorna una lista vacía si no existen registros.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en la base de datos permanecen inmutables.
     */
    @Transactional(readOnly = true)
    public List<PropuestaAsignacion> obtenerPropuestasProfesor(
            String profesorId) {

        return propuestaRepository.findBySolicitud_Profesor_Id(profesorId);
    }

    /**
     * Objetivo:
     * Aceptar una propuesta de asignación pendiente por parte del profesor correspondiente, validando nuevamente
     * la disponibilidad del espacio y formalizando la asignación definitiva.
     *
     * Precondiciones:
     * - Debe existir una propuesta con identificador {@code propuestaId} y debe encontrarse en estado pendiente.
     * - {@code profesorId} debe coincidir con el identificador del profesor titular de la solicitud.
     * - La solicitud vinculada debe encontrarse aún en estado pendiente.
     * - El espacio físico debe continuar disponible en los días y horarios estipulados por la propuesta.
     *
     * Parámetros:
     * @param propuestaId Identificador único (String) de la propuesta a aceptar.
     * @param profesorId Identificador único (String) del docente que intenta aceptar la propuesta.
     *
     * Sin retorno.
     *
     * Postcondiciones:
     * - La propuesta cambia su estado a aceptada y se actualiza en base de datos.
     * - La solicitud cambia su estado a aprobada y se persiste.
     * - Se genera y persiste un nuevo registro de {@link Asignacion} con el espacio y cronograma de la propuesta.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si la propuesta no existe o si el usuario no tiene permisos para responderla.
     * @throws IllegalStateException Si la propuesta o la solicitud ya no están pendientes, o si el espacio dejó de estar disponible.
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
     * Objetivo:
     * Rechazar formalmente una propuesta de asignación por parte del profesor, manteniendo inalterada
     * la solicitud original para que pueda recibir otras propuestas o asignaciones futuras.
     *
     * Precondiciones:
     * - Debe existir una propuesta con identificador {@code propuestaId} y debe encontrarse en estado pendiente.
     * - {@code profesorId} debe coincidir con el identificador del profesor titular de la solicitud vinculada.
     *
     * Parámetros:
     * @param propuestaId Identificador único (String) de la propuesta a desestimar.
     * @param profesorId Identificador único (String) del docente que efectúa el rechazo.
     *
     * Sin retorno
     *
     * Postcondiciones:
     * - La propuesta cambia su estado a rechazada y se persiste en la base de datos.
     * - La solicitud original conserva su estado actual intacto.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si no existe la propuesta o si el profesor no posee permisos sobre ella.
     * @throws IllegalStateException Si la propuesta ya había sido respondida previamente.
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