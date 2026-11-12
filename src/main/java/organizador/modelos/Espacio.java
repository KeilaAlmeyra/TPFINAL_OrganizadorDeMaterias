package organizador.modelos;

/**
 * Representa un espacio físico de la facultad.
 * Puede ser un aula común, un aula con computadoras
 * o un laboratorio.
 */
public class Espacio {

    private final String id;
    private final String nombre;
    private final int capacidadMaxima;
    private final String tipoEspacio;
    private final String ubicacion;
    private final String estado;

    public Espacio(String id, String nombre, int capacidadMaxima,
                   String tipoEspacio, String ubicacion, String estado) {

        this.id = id;
        this.nombre = nombre;
        this.capacidadMaxima = capacidadMaxima;
        this.tipoEspacio = tipoEspacio;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public String getTipoEspacio() {
        return tipoEspacio;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getEstado() {
        return estado;
    }

    /**
     * Comprueba si el espacio tiene capacidad suficiente.
     */
    public boolean tieneCapacidad(int cantidadAlumnos) {
        return cantidadAlumnos > 0
                && cantidadAlumnos <= capacidadMaxima;
    }

    /**
     * Comprueba si el espacio tiene el tipo requerido.
     */
    public boolean esDelTipo(String tipoRequerido) {
        return tipoEspacio.equalsIgnoreCase(tipoRequerido);
    }

    /**
     * Comprueba si el espacio está disponible para ser utilizado.
     */
    public boolean estaDisponible() {
        return estado.equalsIgnoreCase("disponible");
    }

    /**
     * Comprueba si el espacio cumple los requisitos básicos
     * de una materia.
     */
    public boolean esCompatible(String tipoRequerido, int cantidadAlumnos) {
        return estaDisponible()
                && esDelTipo(tipoRequerido)
                && tieneCapacidad(cantidadAlumnos);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
