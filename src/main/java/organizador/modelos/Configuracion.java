
package organizador.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class Configuracion {

    @Id
    private String id;

    private LocalDate fechaInicioRecepcion;
    private LocalDate fechaFinRecepcion;

    private LocalDate fechaInicioCursada;
    private LocalDate fechaFinCursada;

    // Constructor requerido por JPA
    protected Configuracion() {
    }

    public Configuracion(String id,
                         LocalDate fechaInicioRecepcion,
                         LocalDate fechaFinRecepcion,
                         LocalDate fechaInicioCursada,
                         LocalDate fechaFinCursada) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID es obligatorio");
        }

        if (fechaInicioRecepcion == null
                || fechaFinRecepcion == null
                || fechaInicioCursada == null
                || fechaFinCursada == null) {
            throw new IllegalArgumentException("Todas las fechas son obligatorias");
        }

        if (fechaFinRecepcion.isBefore(fechaInicioRecepcion)) {
            throw new IllegalArgumentException(
                    "La fecha de cierre no puede ser anterior al inicio de recepción");
        }

        if (fechaFinCursada.isBefore(fechaInicioCursada)) {
            throw new IllegalArgumentException(
                    "La fecha de fin de cursada no puede ser anterior al inicio");
        }

        if (!fechaFinRecepcion.isBefore(fechaInicioCursada)) {
            throw new IllegalArgumentException(
                    "El período de recepción debe finalizar antes del inicio de la cursada");
        }

        this.id = id;
        this.fechaInicioRecepcion = fechaInicioRecepcion;
        this.fechaFinRecepcion = fechaFinRecepcion;
        this.fechaInicioCursada = fechaInicioCursada;
        this.fechaFinCursada = fechaFinCursada;
    }

    public String getId() {
        return id;
    }

    public LocalDate getFechaInicioRecepcion() {
        return fechaInicioRecepcion;
    }

    public LocalDate getFechaFinRecepcion() {
        return fechaFinRecepcion;
    }

    public LocalDate getFechaInicioCursada() {
        return fechaInicioCursada;
    }

    public LocalDate getFechaFinCursada() {
        return fechaFinCursada;
    }

    public boolean estaEnRecepcion(LocalDate fecha) {
        return !fecha.isBefore(fechaInicioRecepcion)
                && !fecha.isAfter(fechaFinRecepcion);
    }

    public boolean estaEnCursada(LocalDate fecha) {
        return !fecha.isBefore(fechaInicioCursada)
                && !fecha.isAfter(fechaFinCursada);
    }
}
