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

/**
 * Representa el servicio principal de orquestación y lógica de negocio para la gestión,
 * evaluación y asignación de espacios físicos a las solicitudes académicas.
 * Coordina las operaciones entre repositorios de datos, reglas de prioridad,
 * servicios auxiliares de gestión y el motor de inferencia de Prolog según el
 * ciclo o modo de funcionamiento del periodo académico.
 */
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

    /**
     * Objetivo:
     * Inicializar el servicio inyectando todas las dependencias requeridas (repositorios y servicios auxiliares).
     *
     * Precondiciones:
     * - Todas las instancias de los componentes pasados por parámetro deben estar disponibles e instanciadas por el contenedor de Spring.
     *
     * Parámetros:
     * @param usuarios Repositorio para la gestión de usuarios.
     * @param materias Repositorio para la gestión de materias.
     * @param solicitudes Repositorio para la gestión de solicitudes.
     * @param asignaciones Repositorio para la gestión de asignaciones confirmadas.
     * @param cumplimientos Repositorio con el historial de cumplimiento de solicitudes.
     * @param reservas Repositorio para la gestión de reservas puntuales de espacios.
     * @param gestorEspacios Servicio auxiliar para el manejo y consulta de espacios físicos.
     * @param gestorSolicitudes Servicio de validación y control de solicitudes.
     * @param calculadorPrioridad Servicio de cálculo de puntajes y ordenamiento de prioridades.
     * @param gestorAsignaciones Servicio de generación de entidades de asignación.
     * @param prologService Servicio de interfaz con el motor lógico Prolog.
     * @param configuracionService Servicio de consulta de configuración y modos del sistema.
     *
     * Retorno:
     * Instancia configurada de {@link AsignadorService}.
     *
     * Postcondiciones:
     * - Las dependencias del servicio quedan asociadas a sus atributos inmutables.
     */
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
     * Obtiene la capacidad máxima de los espacios disponibles
     * compatibles con el tipo requerido por una materia.
     */
    public int obtenerCapacidadMaximaDisponible(String idMateria) {

        Materia materia = materias.findById(idMateria)
                .orElseThrow(() ->
                        new IllegalArgumentException("Materia inexistente."));

        return gestorEspacios.obtenerDisponibles().stream()
                .filter(espacio -> espacio.esDelTipo(
                        materia.getTipoEspacioFrecuente()))
                .mapToInt(Espacio::getCapacidadMaxima)
                .max()
                .orElse(0);
    }

    /**
     * Objetivo:
     * Registrar una nueva solicitud académica en el sistema y determinar su flujo de procesamiento
     * según el modo de funcionamiento actual (guardar como pendiente en recepción o evaluarla inmediatamente en cursada).
     *
     * Precondiciones:
     * - El sistema no debe encontrarse en modo FUERA_DE_PERIODO.
     * - Los identificadores de profesor y materia deben existir en la base de datos.
     * - El usuario indicado debe poseer rol de profesor.
     * - Los datos de la solicitud (horarios, días, capacidad) deben ser válidos conforme a {@link GestorSolicitudes}.
     *
     * Parámetros:
     * @param idProfesor Identificador único (String) del profesor solicitante.
     * @param idMateria Identificador único (String) de la materia asociada.
     * @param cantidadAlumnos Cantidad estimada de alumnos para la actividad.
     * @param dias Lista de días de la semana ({@link List}<{@link DayOfWeek}>) en los que se solicita el espacio.
     * @param horaInicio Horario de inicio ({@link LocalTime}) de la actividad.
     * @param horaFin Horario de finalización ({@link LocalTime}) de la actividad.
     * @param tipoActividad Descripción o clasificación del tipo de actividad académica a realizar.
     *
     * Retorno:
     * @return {@link ModoFuncionamiento} Modo de funcionamiento vigente bajo el cual se recibió la solicitud.
     *
     * Postcondiciones:
     * - La solicitud queda persistida en la base de datos.
     * - Si el modo es CURSADA, se intenta asignar un espacio inmediatamente; en caso de éxito se crea y persiste una asignación.
     * - En cualquier otro modo admisible (ej. recepción), la solicitud permanece persistida en estado pendiente para resolución posterior.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si el sistema está fuera de período, si el profesor o materia no existen,
     *                                  si el usuario no es profesor o si los datos de la solicitud fallan la validación.
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

        int capacidadMaxima = obtenerCapacidadMaximaDisponible(idMateria);

        if (capacidadMaxima == 0) {
            throw new IllegalArgumentException(
                    "No hay espacios disponibles compatibles con esta materia."
            );
        }

        if (cantidadAlumnos > capacidadMaxima) {
            throw new IllegalArgumentException(
                    "La cantidad de alumnos supera la capacidad máxima disponible "
                            + "para esta materia (" + capacidadMaxima + ")."
            );
        }

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

        // Calculamos la prioridad al registrar la solicitud,
        // independientemente del modo de funcionamiento.
        List<RegistroCumplimiento> historial = cumplimientos.findAll();

        solicitud.setPuntajePrioridad(
                calculadorPrioridad.calcularPuntajePrioridad(
                        solicitud,
                        historial
                )
        );

        // Guardamos la solicitud con su prioridad calculada.
        solicitudes.save(solicitud);

        if (modo == ModoFuncionamiento.CURSADA) {
            // Durante la cursada se evalúa solamente esta solicitud.
            procesarSolicitudIndividual(solicitud);
        }

        // En recepción queda pendiente hasta el cierre.
        return modo;
    }

    /**
     * Objetivo:
     * Evaluar de forma aislada una única solicitud durante el período de cursada, asignándole un espacio
     * compatible mediante Prolog si la disponibilidad lo permite.
     *
     * Precondiciones:
     * - La solicitud debe estar instanciada y persistida previamente.
     * - Los servicios auxiliares y la base de datos deben estar disponibles.
     *
     * Parámetros:
     * @param solicitud Entidad {@link Solicitud} individual que se desea evaluar y asignar.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - La solicitud actualiza su puntaje de prioridad secundario.
     * - Si Prolog encuentra un espacio compatible y disponible, se crea y persiste una nueva {@link Asignacion}
     *   y se actualiza el estado de la solicitud.
     * - Si no se encuentra espacio disponible, la solicitud permanece guardada en estado pendiente.
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
     * Objetivo:
     * Procesar por lote la totalidad de solicitudes pendientes acumuladas en el sistema,
     * ordenándolas por prioridad y asignándoles espacios según compatibilidad horaria y física.
     *
     * Precondiciones:
     * - El modo de funcionamiento no debe ser RECEPCION_SOLICITUDES (debe haberse cerrado dicho período).
     * - El modo de funcionamiento no debe ser FUERA_DE_PERIODO.
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link ResultadoProcesamiento} Registro con el balance de solicitudes aprobadas y rechazadas por validación.
     *
     * Postcondiciones:
     * - Las solicitudes no válidas se marcan como RECHAZADA y se persisten.
     * - Para cada solicitud válida que encuentra aula compatible, se crea y persiste una {@link Asignacion},
     *   bloqueando ese espacio para las solicitudes consecutivas dentro del mismo ciclo.
     * - Las solicitudes que no encuentran aula permanecen pendientes para futuros intentos.
     *
     * Excepciones:
     * @throws IllegalStateException Si se invoca mientras aún está abierto el período de recepción
     *                               o si el sistema está fuera del período académico.
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
     * Objetivo:
     * Volver a ejecutar el procesamiento de asignación sobre las solicitudes pendientes
     * exclusivamente durante el período de cursada académica.
     *
     * Precondiciones:
     * - El sistema debe encontrarse estrictamente en modo CURSADA.
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link ResultadoProcesamiento} Registro con el recuento de solicitudes que lograron asignarse (aprobadas)
     *         y aquellas rechazadas formalmente por fallas de validación.
     *
     * Postcondiciones:
     * - Las solicitudes asignadas generan un nuevo registro de {@link Asignacion} y actualizan su estado.
     * - Aquellas que no consiguen espacio libre continúan en estado pendiente.
     *
     * Excepciones:
     * @throws IllegalStateException Si el modo actual del sistema es distinto de CURSADA.
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
     * Modifica una asignación aprobada.
     * Comprueba compatibilidad y disponibilidad antes de guardar.
     */
    @Transactional
    public void modificarAsignacion(
            String idAsignacion,
            String idEspacio,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin) {

        Asignacion asignacion = asignaciones.findById(idAsignacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la asignación."));

        if (!asignacion.estaActiva()
                || !asignacion.getSolicitud().estaAprobada()) {
            throw new IllegalStateException(
                    "Solo se pueden modificar asignaciones aprobadas y activas.");
        }

        if (dias == null || dias.isEmpty()
                || dias.contains(null)
                || horaInicio == null
                || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "Los días y horarios ingresados no son válidos.");
        }

        Espacio espacio = gestorEspacios.buscarPorId(idEspacio);

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "No se encontró el espacio seleccionado.");
        }

        Solicitud solicitud = asignacion.getSolicitud();

        if (!espacio.estaDisponible()) {
            throw new IllegalArgumentException(
                    "El espacio seleccionado no está disponible.");
        }

        if (!espacio.esCompatible(
                solicitud.getMateria().getTipoEspacioFrecuente(),
                solicitud.getCantidadAlumnos())) {
            throw new IllegalArgumentException(
                    "El espacio no cumple con el tipo o la capacidad requerida.");
        }

        boolean disponible = prologService.validarDisponibilidadPropuesta(
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin,
                asignaciones.findAll(),
                reservas.findAll(),
                asignacion.getId()
        );

        if (!disponible) {
            throw new IllegalArgumentException(
                    "El espacio tiene un conflicto de horario con otra asignación o reserva.");
        }

        asignacion.modificar(
                espacio,
                dias,
                horaInicio,
                horaFin
        );

        asignaciones.save(asignacion);
    }

    /**
     * Cancela una asignación aprobada sin eliminar su registro.
     */

    @Transactional
    public void cancelarAsignacion(
            String idAsignacion,
            String idUsuario,
            boolean esAdministrador) {

        Asignacion asignacion = asignaciones.findById(idAsignacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la asignación."));

        if (!esAdministrador
                && !asignacion.getSolicitud().getProfesor()
                .getId().equals(idUsuario)) {

            throw new IllegalArgumentException(
                    "No tenés permiso para cancelar esta asignación.");
        }

        if (!asignacion.estaActiva()
                || !asignacion.getSolicitud().estaAprobada()) {

            throw new IllegalStateException(
                    "Solo se pueden cancelar asignaciones aprobadas y activas.");
        }

        Solicitud solicitud = asignacion.getSolicitud();

        asignacion.cancelar();
        solicitud.cambiarEstado(Solicitud.CANCELADA);

        solicitudes.save(solicitud);
        asignaciones.save(asignacion);
    }


    /**
     * Objetivo:
     * Obtener el listado de usuarios que poseen el rol de profesor, ordenados alfabéticamente por nombre.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible para operaciones de solo lectura.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Usuario}> Lista de usuarios filtrados por rol de profesor y ordenados por nombre.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en la base de datos se mantienen inalterados.
     */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerProfesores() {

        return usuarios.findAll().stream()
                .filter(Usuario::esProfesor)
                .sorted(Comparator.comparing(Usuario::getNombre))
                .toList();
    }

    /**
     * Objetivo:
     * Recuperar la lista completa de materias registradas, ordenadas alfabéticamente por su nombre.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible para operaciones de solo lectura.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Materia}> Lista con todas las materias ordenadas de forma ascendente por nombre.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos no sufre modificaciones.
     */
    @Transactional(readOnly = true)
    public List<Materia> obtenerMaterias() {

        return materias.findAll().stream()
                .sorted(Comparator.comparing(Materia::getNombre))
                .toList();
    }

    /**
     * Objetivo:
     * Obtener el resumen tabular global de todas las solicitudes y sus aulas asignadas para la vista administrativa.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible para operaciones de solo lectura.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link FilaSolicitud}> Lista de DTOs simplificados con todas las solicitudes del sistema.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no genera efectos secundarios en la base de datos.
     */
    @Transactional(readOnly = true)
    public List<FilaSolicitud> obtenerResultados() {
        return obtenerResultados(null, true);
    }

    /**
     * Convierte los días de la semana a nombres en español.
     */
    private String nombreDia(DayOfWeek dia) {

        return switch (dia) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    /**
     * Objetivo:
     * Construir y retornar una lista de representaciones simplificadas (DTO {@link FilaSolicitud})
     * de las solicitudes y sus asignaciones, aplicando filtros según el rol del usuario (administrador o profesor).
     *
     * Precondiciones:
     * - La base de datos debe estar disponible para operaciones de solo lectura.
     *
     * Parámetros:
     * @param idProfesor Identificador único (String) del profesor en sesión, o {@code null} si es administrador.
     * @param esAdministrador Valor booleano que determina si se tienen permisos para visualizar la totalidad de solicitudes.
     *
     * Retorno:
     * @return {@link List}<{@link FilaSolicitud}> Lista de filas ordenadas por identificador de solicitud,
     *         mostrando los datos del curso, horarios, estado y aula asignada (o "-" si no posee).
     *
     * Postcondiciones:
     * - Operación de solo lectura; las entidades y colecciones diferidas son transformadas en objetos inmutables desacoplados de JPA.
     */
    @Transactional(readOnly = true)
    public List<FilaSolicitud> obtenerResultados(
            String idProfesor,
            boolean esAdministrador) {

        Map<String, Asignacion> asignacionesPorSolicitud =
                asignaciones.findAll().stream()
                        .collect(Collectors.toMap(
                                a -> a.getSolicitud().getId(),
                                a -> a,
                                (anterior, nuevo) -> anterior
                        ));

        return solicitudes.findAll().stream()

                .filter(s -> esAdministrador
                        || (idProfesor != null
                        && s.getProfesor().getId().equals(idProfesor)))

                .sorted(Comparator.comparing(Solicitud::getId))

                .map(s -> {
                    Asignacion asignacion =
                            asignacionesPorSolicitud.get(s.getId());

                    String dias = asignacion != null
                            ? asignacion.getDiasAsignados().stream()
                            .map(this::nombreDia)
                            .collect(Collectors.joining(", "))
                            : s.getDias().stream()
                            .map(this::nombreDia)
                            .collect(Collectors.joining(", "));

                    LocalTime inicio = asignacion != null
                            ? asignacion.getHoraInicioAsignada()
                            : s.getHoraInicio();

                    LocalTime fin = asignacion != null
                            ? asignacion.getHoraFinAsignada()
                            : s.getHoraFin();

                    String aula = asignacion != null
                            ? asignacion.getEspacio().getNombre()
                            : "-";

                    return new FilaSolicitud(
                            s.getId(),
                            s.getProfesor().getNombre(),
                            s.getMateria().getNombre(),
                            s.getMateria().getAnioCarrera(),
                            s.getCantidadAlumnos(),
                            s.getMateria().getTipoEspacioFrecuente(),
                            dias,
                            inicio,
                            fin,
                            s.getEstadoSolicitud(),
                            s.getPuntajePrioridad(),
                            aula,
                            asignacion != null ? asignacion.getId() : null,
                            asignacion != null ? asignacion.getEstadoAsignacion() : null
                    );
                })
                .toList();
    }

    /**
     * Representa el balance consolidado del procesamiento por lote de solicitudes.
     *
     * @param aprobadas Cantidad de solicitudes a las que se les logró asignar un espacio.
     * @param rechazadas Cantidad de solicitudes denegadas formalmente por inconsistencias o errores de validación.
     */
    public record ResultadoProcesamiento(
            int aprobadas,
            int rechazadas) {
    }

    /**
     * Busca una asignación activa para preparar su modificación.
     */
    @Transactional(readOnly = true)
    public Asignacion obtenerAsignacionParaEditar(String id) {

        Asignacion asignacion = asignaciones.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la asignación."));

        if (!asignacion.estaActiva()
                || !asignacion.getSolicitud().estaAprobada()) {
            throw new IllegalStateException(
                    "Solo se pueden editar asignaciones aprobadas y activas.");
        }

        return asignacion;
    }
    /**
     * Objetivo:
     * Recuperar las asignaciones registradas y convertirlas a un modelo simplificado (DTO {@link FilaAsignacion}),
     * restringiendo la consulta al profesor solicitante a menos que se trate de un administrador.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible para operaciones de solo lectura.
     * - Si {@code esAdministrador} es {@code false}, {@code idProfesor} debe ser un identificador válido.
     *
     * Parámetros:
     * @param idProfesor Identificador único (String) del profesor para filtrar las asignaciones.
     * @param esAdministrador Valor booleano que indica si se debe omitir el filtro y listar todas las asignaciones existentes.
     *
     * Retorno:
     * @return {@link List}<{@link FilaAsignacion}> Lista de asignaciones procesadas, ordenadas por el nombre de la materia.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en la base de datos permanecen inmutables.
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
                                .map(this::nombreDia)
                                .collect(Collectors.joining(", ")),
                        a.getHoraInicioAsignada(),
                        a.getHoraFinAsignada(),
                        a.getEstadoAsignacion()
                ))
                .toList();
    }

    /**
     * Representa un registro de transferencia de datos (DTO) con los detalles planos de una asignación,
     * optimizado para su renderizado en plantillas de vista (UI).
     *
     * @param id Identificador único de la asignación.
     * @param profesor Nombre del profesor a cargo.
     * @param materia Nombre de la materia asignada.
     * @param aula Nombre del espacio físico o aula asignada.
     * @param dias Cadena descriptiva con los días de cursada separados por comas.
     * @param inicio Horario de inicio del bloque asignado.
     * @param fin Horario de fin del bloque asignado.
     * @param estado Estado formal de la asignación (ej. ACEPTADA, RECHAZADA).
     */
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

    /**
     * Representa un registro de transferencia de datos (DTO) que consolida el estado, datos académicos
     * y el aula resultante de una solicitud para presentación en vistas de usuario o administración.
     *
     * @param id Identificador único de la solicitud.
     * @param profesor Nombre del docente que inició la solicitud.
     * @param materia Nombre de la materia requerida.
     * @param anio Año de la carrera al que pertenece la materia.
     * @param dias Cadena de texto con los días solicitados separados por coma.
     * @param inicio Horario de inicio pedido.
     * @param fin Horario de fin pedido.
     * @param estado Estado actual de la solicitud (PENDIENTE, ASIGNADA, RECHAZADA, etc.).
     * @param puntaje Puntaje numérico de prioridad calculado para la solicitud.
     * @param aula Nombre del aula asignada, o el valor "-" si aún no posee un espacio concedido.
     */
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
            String aula,
            String idAsignacion,
            String estadoAsignacion
    ) {
    }
}