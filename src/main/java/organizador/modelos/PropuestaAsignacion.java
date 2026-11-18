
package organizador.modelos;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "propuestas_asignacion")
public class PropuestaAsignacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";

    public static final String CANCELADA = "CANCELADA";

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    @ElementCollection
    @CollectionTable(
            name = "propuesta_asignacion_dias",
            joinColumns = @JoinColumn(name = "propuesta_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "dia")
    private List<DayOfWeek> diasPropuestos;

    private LocalTime horaInicio;
    private LocalTime horaFin;

    private String estado;

    // Constructor vacío requerido por JPA.
    protected PropuestaAsignacion() {
    }

    public PropuestaAsignacion(
            String id,
            Solicitud solicitud,
            Espacio espacio,
            List<DayOfWeek> diasPropuestos,
            LocalTime horaInicio,
            LocalTime horaFin) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "La propuesta debe tener un identificador.");
        }

        if (solicitud == null || !solicitud.estaPendiente()) {
            throw new IllegalArgumentException(
                    "La propuesta debe pertenecer a una solicitud pendiente.");
        }

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "La propuesta debe indicar un espacio.");
        }

        if (diasPropuestos == null || diasPropuestos.isEmpty()) {
            throw new IllegalArgumentException(
                    "La propuesta debe tener al menos un día.");
        }

        if (horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "El horario propuesto no es válido.");
        }

        this.id = id;
        this.solicitud = solicitud;
        this.espacio = espacio;
        this.diasPropuestos = new ArrayList<>(diasPropuestos);
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = PENDIENTE;
    }

    public String getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Espacio getEspacio() {
        return espacio;
    }

    public List<DayOfWeek> getDiasPropuestos() {
        return diasPropuestos;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getEstado() {
        return estado;
    }

    public boolean estaPendiente() {
        return PENDIENTE.equals(estado);
    }

    public boolean estaAceptada() {
        return ACEPTADA.equals(estado);
    }

    public boolean estaRechazada() {
        return RECHAZADA.equals(estado);
    }

    public boolean estaCancelada() {
        return CANCELADA.equals(estado);
    }

    public void aceptar() {
        verificarPendiente();
        estado = ACEPTADA;
    }

    public void rechazar() {
        verificarPendiente();
        estado = RECHAZADA;
    }

    public void cancelar() {
        verificarPendiente();
        estado = CANCELADA;
    }

    public void actualizar(
            Espacio espacio,
            List<DayOfWeek> diasPropuestos,
            LocalTime horaInicio,
            LocalTime horaFin) {

        verificarPendiente();

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un espacio.");
        }

        if (diasPropuestos == null || diasPropuestos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un día.");
        }

        if (horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                    "El horario propuesto no es válido.");
        }

        this.espacio = espacio;

        List<DayOfWeek> nuevosDias = new ArrayList<>(diasPropuestos);

        this.diasPropuestos.clear();
        this.diasPropuestos.addAll(nuevosDias);

        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    private void verificarPendiente() {
        if (!estaPendiente()) {
            throw new IllegalStateException(
                    "La propuesta ya fue respondida.");
        }
    }
}
