package organizador.repositorios;

import organizador.modelos.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacioRepository extends JpaRepository<Espacio, String> {
}