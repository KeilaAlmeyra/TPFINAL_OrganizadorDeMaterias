
package organizador.modelos;

import jakarta.persistence.Column;
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

    @Column(name = "contrasena")
    private String contrasena;

    private String rol;

    // Constructor vacío requerido por JPA.
    protected Usuario() {
    }

    // Constructor anterior: se conserva para no romper los datos de prueba.
    public Usuario(String id, String nombre, String correo, String rol) {
        this(id, nombre, correo, null, rol);
    }

    // Constructor nuevo: permite crear usuarios con contraseña.
    public Usuario(String id, String nombre, String correo,
                   String contrasena, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
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

    public String getContrasena() {
        return contrasena;
    }

    public String getRol() {
        return rol;
    }

    public boolean esProfesor() {
        return rol != null && rol.equalsIgnoreCase("profesor");
    }

    public boolean esAdministrador() {
        return rol != null && rol.equalsIgnoreCase("administrador");
    }

    @Override
    public String toString() {
        return nombre;
    }
}
