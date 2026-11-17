package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.modelos.Materia;
import organizador.modelos.Usuario;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.MateriaRepository;
import organizador.repositorios.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carga datos opcionales únicamente cuando se pulsa el botón de prueba. */
@Service
public class DatosPruebaAsignacionesService {
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final EspacioRepository espacios;

    public DatosPruebaAsignacionesService(UsuarioRepository usuarios,
                                         MateriaRepository materias,
                                         EspacioRepository espacios) {
        this.usuarios = usuarios;
        this.materias = materias;
        this.espacios = espacios;
    }

    @Transactional
    public void cargar() {
        if (!usuarios.existsById("DEMO-PROF-1")) {
            usuarios.save(new Usuario("DEMO-PROF-1", "Profesora Primer Año",
                    "primero@example.com", "profesor"));
        }
        if (!usuarios.existsById("DEMO-PROF-3")) {
            usuarios.save(new Usuario("DEMO-PROF-3", "Profesor Tercer Año",
                    "tercero@example.com", "profesor"));
        }
        if (!materias.existsById("DEMO-MAT-1")) {
            materias.save(new Materia("DEMO-MAT-1", "Materia Demo Primer Año",
                    4, true, 1, "aula"));
        }
        if (!materias.existsById("DEMO-MAT-3")) {
            materias.save(new Materia("DEMO-MAT-3", "Materia Demo Tercer Año",
                    4, true, 3, "aula"));
        }
        if (!espacios.existsById("DEMO-AULA-1")) {
            espacios.save(new Espacio("DEMO-AULA-1", "Aula Demo (30 lugares)",
                    30, "aula", "disponible"));
        }
    }
}
