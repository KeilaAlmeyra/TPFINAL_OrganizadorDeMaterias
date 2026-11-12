package organizador.modelos;

/**
 * Representa a una persona que utiliza el sistema.
 * Puede ser un profesor o un administrador.
 */
public class Usuario {

    private final String id;
    private final String nombre;
    private final String correo;
    private final String rol;

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
