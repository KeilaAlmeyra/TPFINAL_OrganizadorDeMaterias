
package organizador.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa una materia de una carrera universitaria.
 */
@Entity
@Table(name = "materias")
public class Materia {

    @Id
    private String id;

    private String nombre;
    private int cargaHoraria;
    private boolean troncal;
    private int anioCarrera;
    private String tipoEspacioFrecuente;

    // Constructor vacío requerido por JPA.
    protected Materia() {
    }

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

    public boolean isTroncal() {
        return troncal;
    }
    public int getAnioCarrera() {
        return anioCarrera;
    }

    public String getTipoEspacioFrecuente() {
        return tipoEspacioFrecuente;
    }


    public void actualizar(
            String nombre,
            int cargaHoraria,
            boolean troncal,
            int anioCarrera,
            String tipoEspacioFrecuente) {

        this.nombre = nombre;
        this.cargaHoraria = cargaHoraria;
        this.troncal = troncal;
        this.anioCarrera = anioCarrera;
        this.tipoEspacioFrecuente = tipoEspacioFrecuente;
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
