package organizador.modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registra qué ocurrió con una asignación
 * una vez que llega el momento de utilizar el espacio.
 */
public class RegistroCumplimiento {

    public static final String UTILIZADA = "UTILIZADA";
    public static final String CANCELACION_ANTICIPADA = "CANCELACION_ANTICIPADA";
    public static final String CANCELACION_TARDIA = "CANCELACION_TARDIA";
    public static final String INASISTENCIA = "INASISTENCIA";
    public static final String CANCELACION_JUSTIFICADA = "CANCELACION_JUSTIFICADA";

    private final String id;
    private final Asignacion asignacion;
    private final LocalDate fecha;
    private final String resultado;
    private final LocalDateTime fechaRegistro;

    public RegistroCumplimiento(String id, Asignacion asignacion,
                                LocalDate fecha, String resultado) {

        if (asignacion == null) {
            throw new IllegalArgumentException(
                    "El registro debe estar asociado a una asignación."
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

    /**
     * Comprueba que el resultado pertenezca
     * a las situaciones contempladas por el sistema.
     */
    private boolean esResultadoValido(String resultado) {
        return UTILIZADA.equals(resultado)
                || CANCELACION_ANTICIPADA.equals(resultado)
                || CANCELACION_TARDIA.equals(resultado)
                || INASISTENCIA.equals(resultado)
                || CANCELACION_JUSTIFICADA.equals(resultado);
    }

    /**
     * Indica si el registro representa un incumplimiento.
     */
    public boolean esIncumplimiento() {
        return CANCELACION_TARDIA.equals(resultado)
                || INASISTENCIA.equals(resultado);
    }

    /**
     * Indica si el registro debe considerarse
     * para calcular el cumplimiento.
     */
    public boolean esEvaluable() {
        return !CANCELACION_ANTICIPADA.equals(resultado)
                && !CANCELACION_JUSTIFICADA.equals(resultado);
    }
}
