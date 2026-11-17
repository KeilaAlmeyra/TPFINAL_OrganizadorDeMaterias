
package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.AsignacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class GestorEspacios {

    private final EspacioRepository repositorio;
    private final ReservaRepository reservas;
    private final AsignacionRepository asignaciones;

    public GestorEspacios(
            EspacioRepository repositorio,
            ReservaRepository reservas,
            AsignacionRepository asignaciones) {

        this.repositorio = repositorio;
        this.reservas = reservas;
        this.asignaciones = asignaciones;
    }

    // Registrar un nuevo espacio.
    public void registrarEspacio(Espacio espacio) {

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "El espacio no puede ser nulo.");
        }

        if (espacio.getId() == null || espacio.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Debe ingresar un código identificador.");
        }

        if (repositorio.existsById(espacio.getId().trim())) {
            throw new IllegalArgumentException(
                    "Ya existe un espacio con ese ID.");
        }

        validarDatos(
                espacio.getNombre(),
                espacio.getCapacidadMaxima(),
                espacio.getTipoEspacio(),
                espacio.getEstado()
        );

        espacio.actualizar(
                espacio.getNombre().trim(),
                espacio.getCapacidadMaxima(),
                normalizarTipo(espacio.getTipoEspacio()),
                normalizarEstado(espacio.getEstado())
        );

        repositorio.save(espacio);
    }

    // Obtener todos los espacios.
    public List<Espacio> obtenerEspacios() {
        return repositorio.findAll(Sort.by("id"));
    }

    // Buscar un espacio por su ID.
    public Espacio buscarPorId(String id) {

        if (id == null || id.isBlank()) {
            return null;
        }

        return repositorio.findById(id.trim()).orElse(null);
    }

    // Obtener solamente los espacios disponibles.
    public List<Espacio> obtenerDisponibles() {
        return obtenerEspacios().stream()
                .filter(Espacio::estaDisponible)
                .toList();
    }

    // Buscar espacios de un tipo determinado.
    public List<Espacio> buscarPorTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            return List.of();
        }

        return obtenerEspacios().stream()
                .filter(espacio -> espacio.esDelTipo(tipo.trim()))
                .toList();
    }

    // Modificar los datos de un espacio existente.
    public void modificarEspacio(
            String id,
            String nombre,
            int capacidadMaxima,
            String tipoEspacio,
            String estado) {

        Espacio espacio = buscarPorId(id);

        if (espacio == null) {
            throw new IllegalArgumentException(
                    "No se encontró el espacio que desea modificar.");
        }

        validarDatos(nombre, capacidadMaxima, tipoEspacio, estado);

        espacio.actualizar(
                nombre.trim(),
                capacidadMaxima,
                normalizarTipo(tipoEspacio),
                normalizarEstado(estado)
        );

        repositorio.save(espacio);
    }

    // Eliminar un espacio únicamente si no tiene registros asociados.
    public boolean eliminarEspacio(String id) {

        if (id == null || id.isBlank()
                || !repositorio.existsById(id.trim())) {
            return false;
        }

        String espacioId = id.trim();

        if (reservas.existsByEspacio_Id(espacioId)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el espacio porque tiene reservas asociadas.");
        }

        if (asignaciones.existsByEspacio_Id(espacioId)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el espacio porque tiene asignaciones asociadas.");
        }

        repositorio.deleteById(espacioId);
        return true;
    }

    // Validar los datos comunes al registrar y modificar.
    private void validarDatos(
            String nombre,
            int capacidadMaxima,
            String tipoEspacio,
            String estado) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe ingresar el nombre del espacio.");
        }

        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException(
                    "La capacidad debe ser mayor que cero.");
        }

        String tipo = normalizarTipo(tipoEspacio);

        if (!tipo.equals("aula")
                && !tipo.equals("computadoras")
                && !tipo.equals("laboratorio")) {

            throw new IllegalArgumentException(
                    "El tipo debe ser aula, computadoras o laboratorio.");
        }

        String estadoNormalizado = normalizarEstado(estado);

        if (!estadoNormalizado.equals("disponible")
                && !estadoNormalizado.equals("mantenimiento")) {

            throw new IllegalArgumentException(
                    "El estado debe ser disponible o mantenimiento.");
        }
    }

    private String normalizarTipo(String tipo) {
        return tipo == null
                ? ""
                : tipo.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarEstado(String estado) {
        return estado == null
                ? ""
                : estado.trim().toLowerCase(Locale.ROOT);
    }
}
