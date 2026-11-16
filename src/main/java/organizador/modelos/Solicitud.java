
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

/**
 * Representa una solicitud realizada por un profesor
 * para utilizar un espacio de la facultad.
 *
 * La solicitud contiene las preferencias del profesor.
 * El espacio otorgado se registra posteriormente
 * mediante la clase Asignacion.
 */
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    // Estados posibles de una solicitud.
    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    // Tipos de actividad académica.
    public static final String EXAMEN_FINAL = "EXAMEN_FINAL";
    public static final String PARCIAL = "PARCIAL";
    public static final String RECUPERATORIO = "RECUPERATORIO";
    public static final String CLASE_OBLIGATORIA = "CLASE_OBLIGATORIA";
    public static final String CLASE_REGULAR = "CLASE_REGULAR";

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Usuario profesor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    private int cantidadAlumnos;

    @ElementCollection
    @CollectionTable(
            name = "solicitud_dias",
            joinColumns = @JoinColumn(name = "solicitud_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "dia")
    private List<DayOfWeek> dias;

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String tipoActividad;

    private String estadoSolicitud;
    private double puntajePrioridad;

    // Constructor vacío requerido por JPA.
    protected Solicitud() {
    }

    public Solicitud(String id, Usuario profesor, Materia materia,
                     int cantidadAlumnos,
                     List<DayOfWeek> dias,
                     LocalTime horaInicio,
                     LocalTime horaFin,
                     String tipoActividad) {

        if (profesor == null || !profesor.esProfesor()) {
            throw new IllegalArgumentException(
                    "La solicitud debe pertenecer a un profesor."
            );
        }

        if (materia == null) {
            throw new IllegalArgumentException(
                    "La solicitud debe tener una materia."
            );
        }

        if (cantidadAlumnos <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de alumnos debe ser mayor a cero."
            );
        }

        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException(
                    "La solicitud debe indicar al menos un día."
            );
        }

        if (!esActividadValida(tipoActividad)) {
            throw new IllegalArgumentException(
                    "El tipo de actividad no es válido."
            );
        }

        this.id = id;
        this.profesor = profesor;
        this.materia = materia;
        this.cantidadAlumnos = cantidadAlumnos;
        this.dias = List.copyOf(dias);
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.tipoActividad = tipoActividad;

        // Toda solicitud comienza pendiente.
        this.estadoSolicitud = PENDIENTE;
        this.puntajePrioridad = 0;
    }

    public String getId() {
        return id;
    }

    public Usuario getProfesor() {
        return profesor;
    }

    public Materia getMateria() {
        return materia;
    }

    public int getCantidadAlumnos() {
        return cantidadAlumnos;
    }

    public List<DayOfWeek> getDias() {
        return dias;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getTipoActividad() {
        return tipoActividad;
    }

    public String getEstadoSolicitud() {
        return estadoSolicitud;
    }

    public double getPuntajePrioridad() {
        return puntajePrioridad;
    }

    /**
     * Comprueba que la hora de finalización
     * sea posterior a la hora de inicio.
     */
    public boolean horarioValido() {
        return horaInicio != null
                && horaFin != null
                && horaFin.isAfter(horaInicio);
    }

    /**
     * Comprueba si la actividad pertenece
     * a los tipos contemplados por el sistema.
     */
    private boolean esActividadValida(String actividad) {
        return EXAMEN_FINAL.equals(actividad)
                || PARCIAL.equals(actividad)
                || RECUPERATORIO.equals(actividad)
                || CLASE_OBLIGATORIA.equals(actividad)
                || CLASE_REGULAR.equals(actividad);
    }

    public boolean estaPendiente() {
        return PENDIENTE.equals(estadoSolicitud);
    }

    public boolean estaAprobada() {
        return APROBADA.equals(estadoSolicitud);
    }

    public boolean estaRechazada() {
        return RECHAZADA.equals(estadoSolicitud);
    }

    /**
     * Permite cambiar el estado de la solicitud.
     */
    public void cambiarEstado(String nuevoEstado) {

        if (PENDIENTE.equals(nuevoEstado)
                || APROBADA.equals(nuevoEstado)
                || RECHAZADA.equals(nuevoEstado)) {

            this.estadoSolicitud = nuevoEstado;

        } else {
            throw new IllegalArgumentException(
                    "El estado de la solicitud no es válido."
            );
        }
    }

    /**
     * Guarda el puntaje calculado por CalculadorPrioridad.
     */
    public void setPuntajePrioridad(double puntajePrioridad) {

        if (puntajePrioridad < 0 || puntajePrioridad > 85) {
            throw new IllegalArgumentException(
                    "El puntaje debe estar entre 0 y 85."
            );
        }

        this.puntajePrioridad = puntajePrioridad;
    }

    @Override
    public String toString() {
        return "Solicitud " + id + " - " + materia.getNombre();
    }
}
