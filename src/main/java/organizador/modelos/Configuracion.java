package organizador.modelos;

import java.time.LocalDate;

/**
 * Representa la configuración general del sistema.
 * Permite controlar el período académico y la etapa
 * en la que se encuentra la organización de espacios.
 */
public class Configuracion {

    private final String id;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final boolean enPlanificacion;

    public Configuracion(String id, LocalDate fechaInicio,
                         LocalDate fechaFin, boolean enPlanificacion) {

        this.id = id;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.enPlanificacion = enPlanificacion;
    }

    public String getId() {
        return id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public boolean isEnPlanificacion() {
        return enPlanificacion;
    }

    /**
     * Comprueba si una fecha pertenece al período configurado.
     */
    public boolean estaDentroDelPeriodo(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio)
                && !fecha.isAfter(fechaFin);
    }
}
