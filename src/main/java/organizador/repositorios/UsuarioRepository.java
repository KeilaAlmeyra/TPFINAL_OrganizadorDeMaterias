package organizador.repositorios;

import organizador.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Representa el repositorio de acceso a datos para la entidad {@link Usuario}.
 * Extiende de {@link JpaRepository} para proveer operaciones CRUD estándar,
 * gestión de persistencia y consultas específicas para la autenticación y validación
 * de cuentas de usuario en el sistema.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    /**
     * Objetivo:
     * Buscar un usuario a partir de su dirección de correo electrónico,
     * ignorando diferencias entre mayúsculas y minúsculas.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code correo} no debe ser nulo.
     *
     * Parámetros:
     * @param correo Dirección de correo electrónico (String) asociada al usuario a consultar.
     *
     * Retorno:
     * @return {@link Optional}<{@link Usuario}> Contenedor con la entidad {@link Usuario} correspondiente
     *         si existe una coincidencia; {@link Optional#empty()} en caso contrario.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos permanece inmutable.
     */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /**
     * Objetivo:
     * Verificar si existe al menos un usuario registrado con una dirección de correo
     * electrónico específica, ignorando diferencias entre mayúsculas y minúsculas.
     *
     * Precondiciones:
     * - La base de datos debe estar disponible y accesible.
     * - El parámetro {@code correo} no debe ser nulo.
     *
     * Parámetros:
     * @param correo Dirección de correo electrónico (String) a comprobar.
     *
     * Retorno:
     * @return {@code true} si ya existe un usuario registrado con dicho correo;
     *         {@code false} en caso contrario.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de la base de datos permanece inalterado.
     */
    boolean existsByCorreoIgnoreCase(String correo);
}