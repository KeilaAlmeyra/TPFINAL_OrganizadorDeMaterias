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
}
