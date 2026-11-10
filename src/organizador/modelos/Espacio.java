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
}