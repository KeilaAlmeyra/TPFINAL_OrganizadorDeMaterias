package organizador.repositorios;

import organizador.modelos.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla ESPACIO.
 *
 * JpaRepository<Espacio, String>:
 *   - Espacio: la entidad (la tabla) con la que trabaja.
 *   - String: el tipo del @Id de Espacio
 *
 * Es una interfaz vacía a propósito: Spring crea la implementación solo
 * y ya trae métodos listos, como save, findById, findAll, existsById
 * y deleteById. Los usa GestorEspacios.
 */
public interface EspacioRepository extends JpaRepository<Espacio, String> {
}