package organizador.modelos;

import java.time.LocalDate;
import java.time.LocalTime;

public class Reserva {

    private final String id;
    private final Espacio espacio;
    private final String usuario;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;

    public Reserva(String id, Espacio espacio, String usuario, LocalDate fecha, LocalTime horaInicio) {
        this.id = id;
        this.espacio = espacio;
        this.usuario = usuario;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaInicio;
    }

    public String getId() { return this.id;}
    public Espacio getEspacio()  { return this.espacio; }
    public String getUsuario() { return this.usuario; }
    public LocalDate getFecha() { return this.fecha; }
    public LocalTime getHoraInicio() { return this.horaInicio; }
    public LocalTime getHoraFin() { return this.horaFin; }

    /**
     * Compara con otro pedido para ver si se superponen.
     * @param otroEspacio
     * @param otraFecha
     * @param otroInicio
     * @param otroFin
     * @return
     */
    public boolean choca(Espacio otroEspacio, LocalDate otraFecha, LocalTime otroInicio, LocalTime otroFin){
        return espacio.getId().equals(otroEspacio.getId())
                && fecha.equals(otraFecha)
                && horaInicio.isBefore(otroFin)
                && otroInicio.isBefore(horaFin);
    }

    
    /**
     * Verifica que el horario de finalizacion
     * sea posterior al horario de inicio.
     *
     * @return true si el horario es valido.
     */
    public boolean horarioValido() {
        return horaFin.isAfter(horaInicio);
    }

    /**
     * Compara esta reserva con otra reserva
     * para determinar si existe superposicion.
     *
     * @param otraReserva Reserva que se desea comparar.
     * @return true si ambas reservas se superponen.
     */
    public boolean choca(Reserva otraReserva) {
        return choca(
                otraReserva.getEspacio(),
                otraReserva.getFecha(),
                otraReserva.getHoraInicio(),
                otraReserva.getHoraFin()
        );
    }

    /**
     * Verifica si la reserva pertenece
     * a un usuario determinado.
     *
     * @param otroUsuario Usuario que se desea consultar.
     * @return true si la reserva pertenece al usuario.
     */
    public boolean perteneceAUsuario(String otroUsuario) {
        return usuario.equals(otroUsuario);
    }

    /**
     * Verifica si la reserva corresponde
     * a una fecha determinada.
     *
     * @param otraFecha Fecha que se desea consultar.
     * @return true si las fechas coinciden.
     */
    public boolean esEnFecha(LocalDate otraFecha) {
        return fecha.equals(otraFecha);
    }

    /**
     * Calcula la duracion de una reserva en minutos.
     *
     * @return Duracion de la reserva en minutos.
     */
    public long getDuracionMinutos() {
        return java.time.Duration.between(horaInicio, horaFin).toMinutes();
    }

    /**
     * Verifica si una hora se encuentra dentro
     * del horario de la reserva.
     *
     * @param otraHora Hora que se desea consultar.
     * @return true si la hora esta dentro de la reserva.
     */
    public boolean ocupaHorario(LocalTime otraHora) {
        return !otraHora.isBefore(horaInicio)
                && otraHora.isBefore(horaFin);
    }

    /**
     * Verifica si un espacio se encuentra reservado
     * en una fecha y hora determinadas.
     *
     * @param otroEspacio Espacio que se desea consultar.
     * @param otraFecha Fecha que se desea consultar.
     * @param otraHora Hora que se desea consultar.
     * @return true si esta reserva ocupa el espacio
     *         en la fecha y hora indicadas.
     */
    public boolean estaReservado(Espacio otroEspacio,
                                 LocalDate otraFecha,
                                 LocalTime otraHora) {

        return espacio.getId().equals(otroEspacio.getId())
                && esEnFecha(otraFecha)
                && ocupaHorario(otraHora);
    }

    /**
     * Devuelve la informacion de una reserva.
     *
     * @return Descripcion de la reserva.
     */
    public String getDescripcion() {
        return "Reserva: " + id
                + "\nEspacio: " + espacio.getNombre()
                + "\nUsuario: " + usuario
                + "\nFecha: " + fecha
                + "\nHora de inicio: " + horaInicio
                + "\nHora de fin: " + horaFin;
    }
}
