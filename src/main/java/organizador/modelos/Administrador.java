package organizador.modelos;

/**
 * Representa a un administrador del sistema.
 * Hereda los datos y comportamientos de Usuario.
 */
public class Administrador extends Usuario {

    public Administrador(String id, String nombre, String correo) {
        super(id, nombre, correo, "administrador");
    }
}
