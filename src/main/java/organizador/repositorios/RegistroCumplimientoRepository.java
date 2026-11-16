package organizador.repositorios;

import organizador.modelos.RegistroCumplimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroCumplimientoRepository
        extends JpaRepository<RegistroCumplimiento, String> {
}