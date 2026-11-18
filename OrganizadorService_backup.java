package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Solicitud;
import organizador.repositorios.AsignacionRepository;
import organizador.repositorios.SolicitudRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizadorService {

    // Repositorios para acceder a la base de datos.
    private final SolicitudRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;

    // Gestores y servicios que coordinan el procesamiento.
    private final GestorSolicitudes gestorSolicitudes;
    private final CalculadorPrioridad calculadorPrioridad;
    private final GestorEspacios gestorEspacios;
    private final GestorAsignaciones gestorAsignaciones;
    private final GestorCumplimiento gestorCumplimiento;
    private final PrologService prologService;

    // Constructor: recibe las dependencias de Spring.
    public OrganizadorService(
            SolicitudRepository solicitudRepository,
            AsignacionRepository asignacionRepository,
            GestorSolicitudes gestorSolicitudes,
            CalculadorPrioridad calculadorPrioridad,
            GestorEspacios gestorEspacios,
            GestorAsignaciones gestorAsignaciones,
            GestorCumplimiento gestorCumplimiento,
            PrologService prologService) {

        this.solicitudRepository = solicitudRepository;
        this.asignacionRepository = asignacionRepository;
        this.gestorSolicitudes = gestorSolicitudes;
        this.calculadorPrioridad = calculadorPrioridad;
        this.gestorEspacios = gestorEspacios;
        this.gestorAsignaciones = gestorAsignaciones;
        this.gestorCumplimiento = gestorCumplimiento;
        this.prologService = prologService;
    }

    /**
     * Obtiene todas las solicitudes que todavía están pendientes.
     *
     * @return lista de solicitudes pendientes.
     */
    public List<Solicitud> obtenerSolicitudesPendientes() {

        return solicitudRepository.findAll().stream()
                .filter(Solicitud::estaPendiente)
                .toList();
    }

    /**
     * Valida las solicitudes que están pendientes.
     *
     * @return lista de solicitudes que cumplen los requisitos.
     */
    public List<Solicitud> validarSolicitudesPendientes() {

        return obtenerSolicitudesPendientes().stream()
                .filter(solicitud ->
                        gestorSolicitudes.validarSolicitud(solicitud) == null)
                .toList();
    }

    /**
     * Ordena las solicitudes válidas según su prioridad.
     *
     * También guarda el puntaje secundario calculado
     * para cada solicitud.
     *
     * @return lista de solicitudes ordenadas por prioridad.
     */
    public List<Solicitud> ordenarSolicitudesPendientes() {

        // Obtener las solicitudes que cumplen los requisitos.
        List<Solicitud> solicitudesValidas =
                validarSolicitudesPendientes();

        // Obtener el historial de cumplimiento de los profesores.
        var historial = gestorCumplimiento.obtenerRegistros();

        // Ordenar por año de carrera y puntaje secundario.
        List<Solicitud> solicitudesOrdenadas =
                calculadorPrioridad.ordenarSolicitudes(
                        solicitudesValidas,
                        historial
                );

        // Guardar el puntaje secundario de cada solicitud.
        for (Solicitud solicitud : solicitudesOrdenadas) {

            double puntaje =
                    calculadorPrioridad.calcularPuntajeSecundario(
                            solicitud,
                            historial
                    );

            solicitud.setPuntajePrioridad(puntaje);

            solicitudRepository.save(solicitud);
        }

        return solicitudesOrdenadas;
    }

    /**
     * Procesa las solicitudes pendientes según su prioridad.
     *
     * Busca un espacio compatible para cada solicitud.
     * Si encuentra uno, crea y guarda la asignación.
     * Si no encuentra ninguno, la solicitud permanece pendiente.
     *
     * @return lista de asignaciones creadas.
     */
    public List<Asignacion> procesarSolicitudes() {

        // Obtener las solicitudes válidas y ordenadas.
        List<Solicitud> solicitudesOrdenadas =
                ordenarSolicitudesPendientes();

        // Prolog evalua el estado, tipo, capacidad y ocupacion de cada espacio.
        var espaciosDisponibles = gestorEspacios.obtenerEspacios();

        // Obtener las asignaciones que ya existen.
        List<Asignacion> asignacionesActuales =
                new java.util.ArrayList<>(asignacionRepository.findAll());

        // Guardar las nuevas asignaciones.
        List<Asignacion> nuevasAsignaciones =
                new java.util.ArrayList<>();

        // Procesar las solicitudes en orden de prioridad.
        for (Solicitud solicitud : solicitudesOrdenadas) {

            // Buscar un espacio compatible y libre.
            var espacio = prologService.buscarEspacioCompatible(
                    solicitud,
                    espaciosDisponibles,
                    asignacionesActuales,
                    List.of()
            );

            // Si no hay espacio, la solicitud continúa pendiente.
            if (espacio == null) {
                continue;
            }

            // Crear la asignación.
            Asignacion asignacion = gestorAsignaciones.crearAsignacion(
                    java.util.UUID.randomUUID().toString(),
                    solicitud,
                    espacio
            );

            // Guardar los cambios en la base de datos.
            solicitudRepository.save(solicitud);
            asignacionRepository.save(asignacion);

            // Actualizar las listas para considerar esta asignación
            // al procesar las siguientes solicitudes.
            asignacionesActuales.add(asignacion);
            nuevasAsignaciones.add(asignacion);
        }

        return nuevasAsignaciones;
    }

}