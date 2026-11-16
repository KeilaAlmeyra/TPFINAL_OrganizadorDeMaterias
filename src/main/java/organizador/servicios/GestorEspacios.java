package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.repositorios.EspacioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestorEspacios {

    private final EspacioRepository repositorio;

    public GestorEspacios(EspacioRepository repositorio) {
        this.repositorio = repositorio;
    }

    // Registrar un nuevo espacio.
    public void registrarEspacio(Espacio espacio) {

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "El espacio no puede ser nulo."
            );
        }

        if (repositorio.existsById(espacio.getId())) {
            throw new IllegalArgumentException(
                    "Ya existe un espacio con ese ID."
            );
        }

        repositorio.save(espacio);
    }

    // Obtener todos los espacios.
    public List<Espacio> obtenerEspacios() {
        return repositorio.findAll(Sort.by("id"));
    }

    // Buscar un espacio por su ID (null si no existe).
    public Espacio buscarPorId(String id) {

        if (id == null) {
            return null;
        }

        return repositorio.findById(id).orElse(null);
    }

    // Obtener solamente los espacios disponibles.
    public List<Espacio> obtenerDisponibles() {

        return obtenerEspacios().stream()
                .filter(Espacio::estaDisponible)
                .toList();
    }

    // Buscar espacios de un tipo determinado.
    public List<Espacio> buscarPorTipo(String tipo) {

        return obtenerEspacios().stream()
                .filter(espacio -> espacio.esDelTipo(tipo))
                .toList();
    }

    // Eliminar un espacio.
    public boolean eliminarEspacio(String id) {

        if (id == null || !repositorio.existsById(id)) {
            return false;
        }

        repositorio.deleteById(id);
        return true;
    }
}