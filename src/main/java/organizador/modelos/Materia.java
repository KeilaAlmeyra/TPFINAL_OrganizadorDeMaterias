package organizador.modelos;

/**
 * Representa una materia de una carrera universitaria.
 */
public class Materia {

    private final String id;
    private final String nombre;
    private final int cargaHoraria;
    private final boolean troncal;
    private final int anioCarrera;
    private final String tipoEspacioFrecuente;

    public Materia(String id, String nombre, int cargaHoraria,
                   boolean troncal, int anioCarrera,
                   String tipoEspacioFrecuente) {

        this.id = id;
        this.nombre = nombre;
        this.cargaHoraria = cargaHoraria;
        this.troncal = troncal;
        this.anioCarrera = anioCarrera;
        this.tipoEspacioFrecuente = tipoEspacioFrecuente;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }

    public boolean esTroncal() {
        return troncal;
    }

    public int getAnioCarrera() {
        return anioCarrera;
    }

    public String getTipoEspacioFrecuente() {
        return tipoEspacioFrecuente;
    }

    /**
     * Comprueba si la materia requiere un aula común.
     */
    public boolean necesitaAula() {
        return tipoEspacioFrecuente.equalsIgnoreCase("aula");
    }

    /**
     * Comprueba si la materia requiere un aula con computadoras.
     */
    public boolean necesitaComputadoras() {
        return tipoEspacioFrecuente.equalsIgnoreCase("computadoras");
    }

    /**
     * Comprueba si la materia requiere un laboratorio.
     */
    public boolean necesitaLaboratorio() {
        return tipoEspacioFrecuente.equalsIgnoreCase("laboratorio");
    }

    @Override
    public String toString() {
        return nombre;
    }
}
