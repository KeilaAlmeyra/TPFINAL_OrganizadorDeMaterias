package organizador.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa la entidad de un usuario dentro del sistema organizador.
 * Encapsula la información de identidad (nombre, correo), credenciales de acceso
 * y el rol asignado (profesor, alumno o administrador) para la gestión de permisos
 * y solicitudes académicas. Mapea contra la tabla {@code usuarios} en la base de datos.
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

    /**
     * Objetivo:
     * Constructor por defecto sin argumentos requerido por el estándar JPA/Hibernate
     * para la instanciación reflexiva de la entidad.
     * Retorno:
     * Instancia vacía de {@link Usuario}.
     * Postcondiciones:
     * - Crea un objeto con sus atributos inicializados en sus valores por defecto (null).
     */
    // Constructor vacío requerido por JPA.
    protected Usuario() {
    }

    /**
     * Objetivo:
     * Instanciar un usuario sin credencial de contraseña explícita (útil para pruebas
     * o compatibilidad con datos precargados).

     * Parámetros:
     * @param id Identificador único (String) del usuario.
     * @param nombre Nombre y apellido o denominación del usuario.
     * @param correo Dirección de correo electrónico del usuario.
     * @param rol Rol que desempeña en el sistema (ej. "profesor", "administrador").
     *
     * Retorno:
     * Instancia configurada de {@link Usuario} con contraseña en null.
     *
     * Postcondiciones:
     * - Se inicializan los atributos del usuario y el campo {@code contrasena} queda como {@code null}.
     */
    // Constructor anterior: se conserva para no romper los datos de prueba.
    public Usuario(String id, String nombre, String correo, String rol) {
        this(id, nombre, correo, null, rol);
    }

    /**
     * Objetivo:
     * Crear una instancia completa de usuario inicializando su identidad, credencial de acceso y rol.
     * Parámetros:
     * @param id Identificador único (String) del usuario.
     * @param nombre Nombre y apellido o denominación del usuario.
     * @param correo Dirección de correo electrónico del usuario.
     * @param contrasena Contraseña o hash de autenticación del usuario.
     * @param rol Rol asignado en la plataforma (ej. "profesor", "administrador").
     *
     * Retorno:
     * Instancia completamente inicializada de {@link Usuario}.
     *
     * Postcondiciones:
     * - Todos los campos internos de la entidad quedan seteados con los valores provistos.
     */
    // Constructor nuevo: permite crear usuarios con contraseña.
    public Usuario(String id, String nombre, String correo,
                   String contrasena, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    /**
     * Objetivo:
     * Obtener el identificador único del usuario.
     *
     * Retorno:
     * @return {@link String} Identificador único del usuario.
     *
     * Postcondiciones:
     * - No altera el estado interno de la entidad.
     */
    public String getId() {
        return id;
    }

    /**
     * Objetivo:
     * Obtener el nombre registrado del usuario.
     * Retorno:
     * @return {@link String} Nombre del usuario.
     *
     * Postcondiciones:
     * - No altera el estado interno de la entidad.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Objetivo:
     * Obtener la dirección de correo electrónico del usuario.
     * Retorno:
     * @return {@link String} Correo electrónico registrado.
     *
     * Postcondiciones:
     * - No altera el estado interno de la entidad.
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Objetivo:
     * Obtener la contraseña o credencial almacenada para el usuario.
     * Retorno:
     * @return {@link String} Contraseña almacenada, o {@code null} si no fue establecida.
     *
     * Postcondiciones:
     * - No altera el estado interno de la entidad.
     */
    public String getContrasena() {
        return contrasena;
    }

    /**
     * Objetivo:
     * Obtener el rol del usuario en la plataforma.
     * Retorno:
     * @return {@link String} Cadena con el rol asignado.
     *
     * Postcondiciones:
     * - No altera el estado interno de la entidad.
     */
    public String getRol() {
        return rol;
    }

    /**
     * Objetivo:
     * Comprobar si el usuario posee asignado el rol de profesor.
     * Retorno:
     * @return {@code true} si el atributo {@code rol} no es nulo y coincide (ignorando mayúsculas/minúsculas) con "profesor";
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - No modifica el estado de la entidad.
     */
    public boolean esProfesor() {
        return rol != null && rol.equalsIgnoreCase("profesor");
    }

    /**
     * Objetivo:
     * Comprobar si el usuario posee asignado el rol de administrador.
     * Retorno:
     * @return {@code true} si el atributo {@code rol} no es nulo y coincide (ignorando mayúsculas/minúsculas) con "administrador";
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - No modifica el estado de la entidad.
     */
    public boolean esAdministrador() {
        return rol != null && rol.equalsIgnoreCase("administrador");
    }

    /**
     * Objetivo:
     * Proveer una representación textual concisa del usuario basada en su nombre.
     * Retorno:
     * @return {@link String} Nombre del usuario.
     *
     * Postcondiciones:
     * - No modifica el estado de la entidad.
     */
    @Override
    public String toString() {
        return nombre;
    }
}