
package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.modelos.Reserva;
import organizador.repositorios.ReservaRepository;
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

    public ReservaService(ReservaRepository reservaRepositorio,
                          GestorEspacios gestorEspacios) {
        this.reservaRepositorio = reservaRepositorio;
        this.gestorEspacios = gestorEspacios;
    }

    public Reserva reservar(String espacioId, LocalDate fecha,
                            LocalTime horaInicio, LocalTime horaFin,
                            String reservadoPor, String propietarioCorreo) {

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

        Reserva reserva = new Reserva(
                espacio,
                fecha,
                horaInicio,
                horaFin,
                reservadoPor,
                propietarioCorreo
        );

        long superposiciones = reservaRepositorio.contarSuperposiciones(
                espacioId, fecha, horaInicio, horaFin
        );

        if (superposiciones > 0) {
            reserva.marcarPendiente(
                    "El espacio ya tiene una reserva confirmada "
                            + "en ese horario. Requiere revisión del administrador."
            );
        }

        return reservaRepositorio.save(reserva);
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

        if (!"PENDIENTE".equals(reserva.getEstado())) {
            throw new IllegalArgumentException(
                    "La solicitud ya no está pendiente.");
        }

        long superposiciones = reservaRepositorio.contarSuperposiciones(
                reserva.getEspacio().getId(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );

        if (superposiciones > 0) {
            throw new IllegalArgumentException(
                    "No se puede aprobar: el horario continúa ocupado."
            );
        }

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

                    reserva.cancelar();
                    reservaRepositorio.save(reserva);
                    return true;
                })
                .orElse(false);
    }

}
