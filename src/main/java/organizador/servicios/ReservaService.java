package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.modelos.Reserva;
import organizador.repositorios.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de las reservas: valida los datos
 * y evita que dos reservas se pisen.
 */
@Service
public class ReservaService {

    // Horario en el que funciona la facultad.
    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE = LocalTime.of(22, 0);

    private final ReservaRepository reservaRepositorio;
    private final GestorEspacios gestorEspacios;

    public ReservaService(ReservaRepository reservaRepositorio,
                          GestorEspacios gestorEspacios) {
        this.reservaRepositorio = reservaRepositorio;
        this.gestorEspacios = gestorEspacios;
    }

    /**
     * Crea una reserva y la guarda en la base de datos.
     * Si algo está mal lanza IllegalArgumentException con el motivo.
     */
    public Reserva reservar(String espacioId, LocalDate fecha,
                            LocalTime horaInicio, LocalTime horaFin,
                            String reservadoPor) {

        Espacio espacio = gestorEspacios.buscarPorId(espacioId);

        if (espacio == null) {
            throw new IllegalArgumentException("Debe elegir un espacio.");
        }

        if (!espacio.estaDisponible()) {
            throw new IllegalArgumentException(
                    "El espacio no está disponible.");
        }

        // El constructor de Reserva valida fecha, horas y nombre.
        Reserva reserva = new Reserva(
                espacio, fecha, horaInicio, horaFin, reservadoPor);

        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "No se puede reservar en una fecha pasada.");
        }

        if (horaInicio.isBefore(APERTURA) || horaFin.isAfter(CIERRE)) {
            throw new IllegalArgumentException(
                    "El horario debe estar entre las 08:00 y las 22:00.");
        }

        if (reservaRepositorio.contarSuperposiciones(
                espacioId, fecha, horaInicio, horaFin) > 0) {
            throw new IllegalArgumentException(
                    "Ese espacio ya está reservado en ese horario.");
        }

        return reservaRepositorio.save(reserva);
    }

    /**
     * Lista las reservas. Si se indica un nombre,
     * muestra solo las de esa persona.
     */
    public List<Reserva> listar(String reservadoPor) {

        if (reservadoPor == null || reservadoPor.isBlank()) {
            return reservaRepositorio.findAllByOrderByFechaAscHoraInicioAsc();
        }

        return reservaRepositorio
                .findByReservadoPorContainingIgnoreCaseOrderByFechaAscHoraInicioAsc(
                        reservadoPor.trim());
    }

    /**
     * Cancela (borra) una reserva. Devuelve false si no existía.
     */
    public boolean cancelar(Long id) {

        if (id == null || !reservaRepositorio.existsById(id)) {
            return false;
        }

        reservaRepositorio.deleteById(id);
        return true;
    }
}