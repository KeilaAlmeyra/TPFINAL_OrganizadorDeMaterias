
package organizador.repositorios;

import organizador.modelos.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, String> {
}
