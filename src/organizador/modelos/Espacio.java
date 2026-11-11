package organizador.modelo;

/**
 * Clase que representa un espacio fisico en la facultad.
 */
public class Espacio {
    /**
     * Código único representativo del espacio.
     */
    private final String id;
    private final String nombre;
    private final String tipoEspacio;
    private final int capacidadMaxima;

    /**
     * Constructor del objeto 'Espacio'
     * @param codigoLugar
     * @param nombre
     * @param tipoEspacio
     * @param capacidadMaxima
     */
    public Espacio(String codigoLugar, String nombre, String tipoEspacio, int capacidadMaxima) {
        this.id = codigoLugar;
        this.nombre = nombre;
        this.tipoEspacio = tipoEspacio;
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getId() { return id; }

    public String getNombre() { return nombre; }

    public String getTipoEspacio() { return tipoEspacio; }

    public int getCapacidadMaxima() { return capacidadMaxima; }

    @Override
    public String toString() { return nombre}

     /**
     * Verifica si el espacio tiene capacidad suficiente
     * para una determinada cantidad de alumnos.
     *
     * @param cantidadAlumnos Cantidad de alumnos.
     * @return true si tiene capacidad suficiente.
     */
    public boolean tieneCapacidad(int cantidadAlumnos) {
        return cantidadAlumnos <= capacidadMaxima;
    }

    /**
     * Verifica si el tipo de espacio coincide
     * con el tipo solicitado.
     *
     * @param tipoRequerido Tipo de espacio solicitado.
     * @return true si los tipos coinciden.
     */
    public boolean esDelTipo(String tipoRequerido) {
        return tipoEspacio.equals(tipoRequerido);
    }    
}
