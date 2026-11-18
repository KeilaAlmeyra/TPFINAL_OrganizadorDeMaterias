
package organizador.servicios;

import organizador.modelos.*;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.AsignacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE = LocalTime.of(22, 0);

    private final ReservaRepository reservaRepositorio;
    private final GestorEspacios gestorEspacios;
    private final AsignacionRepository asignacionRepositorio;
    private final ConfiguracionService configuracionService;

    public ReservaService(
            ReservaRepository reservaRepositorio,
            GestorEspacios gestorEspacios,
            AsignacionRepository asignacionRepositorio,
            ConfiguracionService configuracionService) {

        this.reservaRepositorio = reservaRepositorio;
        this.gestorEspacios = gestorEspacios;
        this.asignacionRepositorio = asignacionRepositorio;
        this.configuracionService = configuracionService;
    }

    private boolean estaVencida(Reserva reserva) {

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        if (reserva.getFecha().isBefore(hoy)) {
            return true;
        }

        return reserva.getFecha().equals(hoy)
                && !reserva.getHoraFin().isAfter(ahora);
    }

    public Reserva reservar(
            String espacioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            String reservadoPor,
            String propietarioCorreo,
            TipoActividad tipoActividad
    ) {

        if (propietarioCorreo == null || propietarioCorreo.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al usuario.");
        }

        Espacio espacio = gestorEspacios.buscarPorId(espacioId);

        if (espacio == null) {
            throw new IllegalArgumentException("Debe elegir un espacio.");
        }

        if (!espacio.estaDisponible()) {
            throw new IllegalArgumentException(
                    "El espacio no está habilitado para utilizarse.");
        }

        if (fecha == null || fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "No se puede reservar en una fecha pasada.");
        }

        if (horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la de inicio.");
        }

        if (horaInicio.isBefore(APERTURA) || horaFin.isAfter(CIERRE)) {
            throw new IllegalArgumentException(
                    "El horario debe estar entre las 08:00 y las 22:00.");
        }

        if (horaInicio.getMinute() % 30 != 0
                || horaFin.getMinute() % 30 != 0) {
            throw new IllegalArgumentException(
                    "Los horarios deben ser en punto o y media.");
        }

        if (fecha.equals(LocalDate.now())
                && !horaInicio.isAfter(LocalTime.now())) {
            throw new IllegalArgumentException(
                    "No se puede reservar un horario que ya comenzó.");
        }

        if (tipoActividad == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de actividad.");
        }

        Reserva reserva = new Reserva(
                espacio,
                fecha,
                horaInicio,
                horaFin,
                reservadoPor,
                propietarioCorreo,
                tipoActividad
        );

        ModoFuncionamiento modo = configuracionService.obtenerModoActual();

// Fuera del período académico no se permiten reservas.
        if (modo == ModoFuncionamiento.FUERA_DE_PERIODO) {
            throw new IllegalArgumentException(
                    "No se pueden realizar reservas fuera del período académico."
            );
        }

// Durante la recepción, las reservas quedan pendientes.
// Se procesarán después de las solicitudes de materias.
        if (modo == ModoFuncionamiento.RECEPCION_SOLICITUDES) {

            reserva.marcarPendiente(
                    "Pendiente de procesamiento al comenzar el período de cursada."
            );

        } else {

            // Durante la cursada, comprobar conflictos con asignaciones activas.
            List<Asignacion> asignaciones = asignacionRepositorio.findAll();

            if (tieneConflictoConAsignacion(reserva, asignaciones)) {

                reserva.marcarPendiente(
                        "El horario coincide con una asignación de materia. "
                                + "Requiere revisión del administrador."
                );

            } else {

                long superposiciones = reservaRepositorio.contarSuperposiciones(
                        espacioId, fecha, horaInicio, horaFin
                );

                if (superposiciones > 0) {
                    reserva.marcarPendiente(
                            "El espacio ya tiene una reserva confirmada "
                                    + "en ese horario. Requiere revisión del administrador."
                    );
                }
            }
        }

        return reservaRepositorio.save(reserva);
    }

    /**
     * Procesa las reservas pendientes al comenzar la cursada.
     * Las reservas se confirman solamente si no entran en conflicto
     * con asignaciones activas ni con otras reservas confirmadas.
     */
    public ResultadoReservas procesarPendientes() {

        if (configuracionService.obtenerModoActual()
                != ModoFuncionamiento.CURSADA) {
            throw new IllegalStateException(
                    "Las reservas solo pueden procesarse durante el período de cursada."
            );
        }

        List<Reserva> reservas = reservaRepositorio
                .findAllByOrderByFechaAscHoraInicioAsc();

        List<Asignacion> asignaciones = asignacionRepositorio.findAll();

        int confirmadas = 0;
        int pendientes = 0;

        for (Reserva reserva : reservas) {

            if (!"PENDIENTE".equals(reserva.getEstado())) {
                continue;
            }

            // No procesar automáticamente reservas vencidas.
            if (estaVencida(reserva)) {
                pendientes++;
                continue;
            }

            if (tieneConflictoConAsignacion(reserva, asignaciones)) {
                reserva.marcarPendiente(
                        "El horario coincide con una asignación de materia. "
                                + "Requiere revisión del administrador."
                );
                reservaRepositorio.save(reserva);
                pendientes++;
                continue;
            }

            long superposiciones = reservaRepositorio.contarSuperposiciones(
                    reserva.getEspacio().getId(),
                    reserva.getFecha(),
                    reserva.getHoraInicio(),
                    reserva.getHoraFin()
            );

            if (superposiciones > 0) {
                reserva.marcarPendiente(
                        "El horario coincide con otra reserva confirmada. "
                                + "Requiere revisión del administrador."
                );
                reservaRepositorio.save(reserva);
                pendientes++;
                continue;
            }

            reserva.confirmar();
            reservaRepositorio.save(reserva);
            confirmadas++;
        }

        return new ResultadoReservas(confirmadas, pendientes);
    }

    /**
     * Comprueba si una reserva puntual coincide con una asignación semanal activa.
     */
    private boolean tieneConflictoConAsignacion(
            Reserva reserva,
            List<Asignacion> asignaciones) {

        for (Asignacion asignacion : asignaciones) {

            if (asignacion == null || !asignacion.estaActiva()) {
                continue;
            }

            if (!asignacion.getEspacio().getId()
                    .equals(reserva.getEspacio().getId())) {
                continue;
            }

            if (!asignacion.getDiasAsignados()
                    .contains(reserva.getFecha().getDayOfWeek())) {
                continue;
            }

            boolean seSuperpone =
                    asignacion.getHoraInicioAsignada()
                            .isBefore(reserva.getHoraFin())
                            && asignacion.getHoraFinAsignada()
                            .isAfter(reserva.getHoraInicio());

            if (seSuperpone) {
                return true;
            }
        }

        return false;
    }

    public record ResultadoReservas(
            int confirmadas,
            int pendientes) {
    }

    // El administrador ve todas; el profesor, solamente las propias.
    public List<Reserva> listar(String correo, boolean esAdministrador) {

        if (esAdministrador) {
            return reservaRepositorio.findAllByOrderByFechaAscHoraInicioAsc();
        }

        if (correo == null || correo.isBlank()) {
            return List.of();
        }

        return reservaRepositorio
                .findByPropietarioCorreoIgnoreCaseOrderByFechaAscHoraInicioAsc(
                        correo);
    }

    // El administrador consulta las solicitudes pendientes.
    public List<Reserva> listarPendientes() {
        return reservaRepositorio
                .findByEstadoOrderByFechaAscHoraInicioAsc("PENDIENTE");
    }


    // El administrador aprueba una solicitud pendiente.
    public Reserva aprobar(Long id) {

        Reserva reserva = reservaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        if (estaVencida(reserva)) {
            throw new IllegalArgumentException(
                    "No se puede aprobar una reserva cuya fecha u horario ya pasó.");
        }

        // 1. Comprobar conflictos con asignaciones de materias.
        List<Asignacion> asignaciones = asignacionRepositorio.findAll();

        if (tieneConflictoConAsignacion(reserva, asignaciones)) {
            throw new IllegalArgumentException(
                    "No se puede aprobar: el espacio está ocupado por una asignación de materia en ese horario.");
        }

        // 2. Comprobar conflictos con otras reservas confirmadas.
        long superposiciones = reservaRepositorio.contarSuperposiciones(
                reserva.getEspacio().getId(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );

        if (superposiciones > 0) {
            throw new IllegalArgumentException(
                    "No se puede aprobar: el horario continúa ocupado por otra reserva.");
        }

        // 3. Confirmar solamente si pasó ambas verificaciones.
        reserva.confirmar();

        return reservaRepositorio.save(reserva);
    }

    // El administrador rechaza una solicitud pendiente.
    public Reserva rechazar(Long id, String motivo) {

        Reserva reserva = reservaRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la solicitud."));

        if (!"PENDIENTE".equals(reserva.getEstado())) {
            throw new IllegalArgumentException(
                    "La solicitud ya no está pendiente.");
        }

        if (estaVencida(reserva)) {
            throw new IllegalArgumentException(
                    "No se puede modificar el estado de una reserva vencida.");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe indicar el motivo del rechazo.");
        }

        reserva.rechazar(motivo.trim());

        return reservaRepositorio.save(reserva);
    }

    // Cancela conservando el registro histórico.

    public boolean cancelar(Long id, String correo, boolean esAdministrador) {

        if (id == null) {
            return false;
        }

        if (esAdministrador) {
            return reservaRepositorio.findById(id)
                    .map(reserva -> {

                        if ("CANCELADA".equals(reserva.getEstado())
                                || "RECHAZADA".equals(reserva.getEstado())) {
                            return false;
                        }

                        if (estaVencida(reserva)) {
                            return false;
                        }

                        reserva.cancelar();
                        reservaRepositorio.save(reserva);
                        return true;
                    })
                    .orElse(false);
        }

        if (correo == null || correo.isBlank()) {
            return false;
        }

        return reservaRepositorio
                .findByIdAndPropietarioCorreoIgnoreCase(id, correo)
                .map(reserva -> {

                    if ("CANCELADA".equals(reserva.getEstado())
                            || "RECHAZADA".equals(reserva.getEstado())) {
                        return false;
                    }

                    if (estaVencida(reserva)) {
                        return false;
                    }

                    reserva.cancelar();
                    reservaRepositorio.save(reserva);
                    return true;
                })
                .orElse(false);
    }

}
