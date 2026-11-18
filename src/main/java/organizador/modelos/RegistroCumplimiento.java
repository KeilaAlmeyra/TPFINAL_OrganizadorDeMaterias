package organizador.modelos;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_cumplimiento")
public class RegistroCumplimiento {

    public static final String UTILIZADA = "UTILIZADA";
    public static final String CANCELACION_ANTICIPADA = "CANCELACION_ANTICIPADA";
    public static final String CANCELACION_TARDIA = "CANCELACION_TARDIA";
    public static final String INASISTENCIA = "INASISTENCIA";
    public static final String CANCELACION_JUSTIFICADA = "CANCELACION_JUSTIFICADA";

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private Asignacion asignacion;

    private LocalDate fecha;

    private String resultado;

    private LocalDateTime fechaRegistro;

    protected RegistroCumplimiento() {
    }

    public RegistroCumplimiento(String id, Asignacion asignacion,
                                LocalDate fecha, String resultado) {

        if (asignacion == null) {
            throw new IllegalArgumentException(
                    "El registro debe estar asociado a una asignación."
            );
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "El registro debe tener una fecha."
            );
        }

        if (!esResultadoValido(resultado)) {
            throw new IllegalArgumentException(
                    "El resultado del registro no es válido."
            );
        }

        this.id = id;
        this.asignacion = asignacion;
        this.fecha = fecha;
        this.resultado = resultado;
        this.fechaRegistro = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Asignacion getAsignacion() {
        return asignacion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getResultado() {
        return resultado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    private boolean esResultadoValido(String resultado) {
        return UTILIZADA.equals(resultado)
                || CANCELACION_ANTICIPADA.equals(resultado)
                || CANCELACION_TARDIA.equals(resultado)
                || INASISTENCIA.equals(resultado)
                || CANCELACION_JUSTIFICADA.equals(resultado);
    }

    public boolean esIncumplimiento() {
        return CANCELACION_TARDIA.equals(resultado)
                || INASISTENCIA.equals(resultado);
    }

    public boolean esEvaluable() {
        return !CANCELACION_ANTICIPADA.equals(resultado)
                && !CANCELACION_JUSTIFICADA.equals(resultado);
    }
}