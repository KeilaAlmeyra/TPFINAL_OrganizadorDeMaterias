
package organizador.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import java.time.LocalDate;
import java.time.LocalTime;

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
    @Enumerated(EnumType.STRING)
    private TipoActividad tipoActividad;


    // Nombre que se muestra en pantalla.
    private String reservadoPor;

    // Correo de la cuenta que creó la reserva.
    private String propietarioCorreo;

    // Estado de la solicitud.
    private String estado;

    // Motivo del conflicto o de la decisión del administrador.
    private String motivo;

    protected Reserva() {}


    public Reserva(Espacio espacio, LocalDate fecha,
                   LocalTime horaInicio, LocalTime horaFin,
                   String reservadoPor, String propietarioCorreo) {

        this(espacio, fecha, horaInicio, horaFin,
                reservadoPor, propietarioCorreo, null);
    }

    public Reserva(Espacio espacio, LocalDate fecha,
                   LocalTime horaInicio, LocalTime horaFin,
                   String reservadoPor, String propietarioCorreo,
                   TipoActividad tipoActividad) {

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "La reserva debe tener un espacio.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La reserva debe tener una fecha.");
        }

        if (horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser posterior a la de inicio.");
        }

        if (reservadoPor == null || reservadoPor.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe indicar quién reserva.");
        }

        if (tipoActividad == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de actividad.");
        }

        this.espacio = espacio;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.reservadoPor = reservadoPor.trim();

        this.propietarioCorreo = propietarioCorreo == null
                ? null
                : propietarioCorreo.trim().toLowerCase();

        this.tipoActividad = tipoActividad;
        this.estado = "CONFIRMADA";
    }


    public TipoActividad getTipoActividad() {
        return tipoActividad;
    }



    @PrePersist
    public void establecerEstadoInicial() {
        if (estado == null || estado.isBlank()) {
            estado = "CONFIRMADA";
        }
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

    public String getPropietarioCorreo() {
        return propietarioCorreo;
    }

    public String getEstado() {
        // Los registros anteriores a este cambio se consideran confirmados.
        return estado == null ? "CONFIRMADA" : estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void marcarPendiente(String motivo) {
        this.estado = "PENDIENTE";
        this.motivo = motivo;
    }

    public void confirmar() {
        this.estado = "CONFIRMADA";
        this.motivo = null;
    }

    public void rechazar(String motivo) {
        this.estado = "RECHAZADA";
        this.motivo = motivo;
    }

    public void cancelar() {
        this.estado = "CANCELADA";
        this.motivo = "Cancelada por el usuario o administrador.";
    }

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
