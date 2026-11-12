package organizador.modelos;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Representa el espacio y horario otorgados
 * a una solicitud que fue aprobada.
 */
public class Asignacion {

    public static final String ACTIVA = "ACTIVA";
    public static final String CANCELADA = "CANCELADA";

    private final String id;
    private final Solicitud solicitud;
    private final Espacio espacio;
    private final List<DayOfWeek> diasAsignados;
    private final LocalTime horaInicioAsignada;
    private final LocalTime horaFinAsignada;

    private String estadoAsignacion;

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

    /**
     * Comprueba si la asignación está activa.
     */
    public boolean estaActiva() {
        return estadoAsignacion.equals(ACTIVA);
    }

    /**
     * Cancela una asignación.
     */
    public void cancelar() {
        estadoAsignacion = CANCELADA;
    }

    /**
     * Comprueba si dos asignaciones utilizan
     * el mismo espacio, día y horario superpuesto.
     */
    public boolean seSuperponeCon(Asignacion otra) {

        if (otra == null || !estaActiva() || !otra.estaActiva()) {
            return false;
        }

        boolean mismoEspacio =
                espacio.getId().equals(otra.espacio.getId());

        boolean mismoDia =
                !java.util.Collections.disjoint(
                        diasAsignados, otra.diasAsignados
                );

        boolean mismoHorario =
                horaInicioAsignada.isBefore(otra.horaFinAsignada)
                        && otra.horaInicioAsignada.isBefore(horaFinAsignada);

        return mismoEspacio && mismoDia && mismoHorario;
    }

    @Override
    public String toString() {
        return "Asignación " + id + " - " + espacio.getNombre();
    }
}
