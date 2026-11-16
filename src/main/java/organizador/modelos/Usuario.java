
package organizador.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa a una persona que utiliza el sistema.
 * Puede ser un profesor o un administrador.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nombre;
    private String correo;
    private String rol;

    // Constructor vacío requerido por JPA.
    protected Usuario() {
    }

    public Usuario(String id, String nombre, String correo, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public boolean esProfesor() {
        return rol.equalsIgnoreCase("profesor");
    }

    public boolean esAdministrador() {
        return rol.equalsIgnoreCase("administrador");
    }

    @Override
    public String toString() {
        return nombre;
    }
}
