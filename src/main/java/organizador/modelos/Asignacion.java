
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "asignaciones")
public class Asignacion {

    public static final String ACTIVA = "ACTIVA";
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
            name = "asignacion_dias",
            joinColumns = @JoinColumn(name = "asignacion_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "dia")
    private List<DayOfWeek> diasAsignados;

    private LocalTime horaInicioAsignada;
    private LocalTime horaFinAsignada;

    private String estadoAsignacion;

    // Constructor vacío requerido por JPA.
    protected Asignacion() {
    }

    public Asignacion(String id, Solicitud solicitud, Espacio espacio,
                      List<DayOfWeek> diasAsignados,
                      LocalTime horaInicioAsignada,
                      LocalTime horaFinAsignada) {

        if (solicitud == null || !solicitud.estaAprobada()) {
            throw new IllegalArgumentException(
                    "Solo se pueden generar asignaciones de solicitudes aprobadas."
            );
        }

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "La asignación debe tener un espacio."
            );
        }

        this.id = id;
        this.solicitud = solicitud;
        this.espacio = espacio;
        this.diasAsignados = List.copyOf(diasAsignados);
        this.horaInicioAsignada = horaInicioAsignada;
        this.horaFinAsignada = horaFinAsignada;
        this.estadoAsignacion = ACTIVA;
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

    public List<DayOfWeek> getDiasAsignados() {
        return diasAsignados;
    }

    public LocalTime getHoraInicioAsignada() {
        return horaInicioAsignada;
    }

    public LocalTime getHoraFinAsignada() {
        return horaFinAsignada;
    }

    public String getEstadoAsignacion() {
        return estadoAsignacion;
    }

    public boolean estaActiva() {
        return estadoAsignacion.equals(ACTIVA);
    }

    public void cancelar() {
        estadoAsignacion = CANCELADA;
    }

    @Override
    public String toString() {
        return "Asignación " + id + " - " + espacio.getNombre();
    }
}
