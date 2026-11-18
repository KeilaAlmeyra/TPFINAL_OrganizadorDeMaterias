
package organizador.modelos;

public enum TipoActividad {
    EXAMEN_FINAL("Examen final"),
    PARCIAL("Parcial"),
    RECUPERATORIO("Recuperatorio"),
    ACTIVIDAD_ACADEMICA("Actividad académica"),
    OTRA("Otra actividad");

    private final String descripcion;

    TipoActividad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
