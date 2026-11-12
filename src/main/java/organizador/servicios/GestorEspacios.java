package organizador.servicios;

import organizador.modelos.Espacio;

import java.util.ArrayList;
import java.util.List;

public class GestorEspacios {

    private final List<Espacio> espacios;

    public GestorEspacios() {
        this.espacios = new ArrayList<>();
    }

    // Registrar un nuevo espacio.
    public void registrarEspacio(Espacio espacio) {

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "El espacio no puede ser nulo."
            );
        }

        if (buscarPorId(espacio.getId()) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un espacio con ese ID."
            );
        }

        espacios.add(espacio);
    }

    // Obtener todos los espacios.
    public List<Espacio> obtenerEspacios() {
        return List.copyOf(espacios);
    }

    // Buscar un espacio por su ID.
    public Espacio buscarPorId(String id) {

        if (id == null) {
            return null;
        }

        return espacios.stream()
                .filter(espacio -> espacio.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Obtener solamente los espacios disponibles.
    public List<Espacio> obtenerDisponibles() {

        return espacios.stream()
                .filter(Espacio::estaDisponible)
                .toList();
    }

    // Buscar espacios de un tipo determinado.
    public List<Espacio> buscarPorTipo(String tipo) {

        return espacios.stream()
                .filter(espacio -> espacio.esDelTipo(tipo))
                .toList();
    }

    // Eliminar un espacio.
    public boolean eliminarEspacio(String id) {

        Espacio espacio = buscarPorId(id);

        if (espacio == null) {
            return false;
        }

        espacios.remove(espacio);
        return true;
    }
}

