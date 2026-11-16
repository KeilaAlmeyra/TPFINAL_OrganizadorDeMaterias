package organizador.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * Representa la reserva de un espacio en una fecha y horario.
 */
@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Espacio espacio;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String reservadoPor;

    protected Reserva() {}

    public Reserva(Espacio espacio, LocalDate fecha,
                   LocalTime horaInicio, LocalTime horaFin,
                   String reservadoPor) {

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "La reserva debe tener un espacio."
            );
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La reserva debe tener una fecha."
            );
        }

        if (horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la de inicio."
            );
        }

        if (reservadoPor == null || reservadoPor.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe indicar quién reserva."
            );
        }

        this.espacio = espacio;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.reservadoPor = reservadoPor.trim();
    }

    public Long getId() {
        return id;
    }

    public Espacio getEspacio() {
        return espacio;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getReservadoPor() {
        return reservadoPor;
    }

    /**
     * Comprueba si dos reservas usan el mismo espacio,
     * en la misma fecha y con horarios que se pisan.
     */
    public boolean seSuperponeCon(Reserva otra) {

        if (otra == null) {
            return false;
        }

        boolean mismoEspacio =
                espacio.getId().equals(otra.espacio.getId());

        boolean mismaFecha = fecha.equals(otra.fecha);

        boolean horarioSePisa =
                horaInicio.isBefore(otra.horaFin)
                        && otra.horaInicio.isBefore(horaFin);

        return mismoEspacio && mismaFecha && horarioSePisa;
    }

    @Override
    public String toString() {
        return "Reserva de " + espacio.getNombre() + " el " + fecha
                + " de " + horaInicio + " a " + horaFin;
    }
}