
package organizador.servicios;

import organizador.modelos.Materia;
import organizador.repositorios.MateriaRepository;
import organizador.repositorios.SolicitudRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class GestorMaterias {

    private final MateriaRepository materias;
    private final SolicitudRepository solicitudes;

    public GestorMaterias(
            MateriaRepository materias,
            SolicitudRepository solicitudes) {

        this.materias = materias;
        this.solicitudes = solicitudes;
    }

    // Registrar una materia nueva.
    public void registrarMateria(Materia materia) {

        validarMateria(materia);

        if (materias.existsById(materia.getId())) {
            throw new IllegalArgumentException(
                    "Ya existe una materia con ese ID."
            );
        }

        materias.save(materia);
    }

    // Obtener todas las materias ordenadas por nombre.
    public List<Materia> obtenerMaterias() {
        return materias.findAll(Sort.by("nombre"));
    }

    // Buscar una materia por su ID.
    public Materia buscarPorId(String id) {

        if (id == null || id.isBlank()) {
            return null;
        }

        return materias.findById(id.trim()).orElse(null);
    }

    // Modificar los datos de una materia existente.
    public void modificarMateria(
            String id,
            String nombre,
            int cargaHoraria,
            boolean troncal,
            int anioCarrera,
            String tipoEspacioFrecuente) {

        Materia materia = buscarPorId(id);

        if (materia == null) {
            throw new IllegalArgumentException(
                    "No se encontró la materia."
            );
        }

        validarDatos(
                nombre,
                cargaHoraria,
                anioCarrera,
                tipoEspacioFrecuente
        );

        materia.actualizar(
                nombre.trim(),
                cargaHoraria,
                troncal,
                anioCarrera,
                tipoEspacioFrecuente.trim().toLowerCase(Locale.ROOT)
        );

        materias.save(materia);
    }

    // Eliminar una materia si no tiene solicitudes asociadas.
    public boolean eliminarMateria(String id) {

        Materia materia = buscarPorId(id);

        if (materia == null) {
            return false;
        }

        if (solicitudes.existsByMateria_Id(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar esta materia porque tiene solicitudes asociadas."
            );
        }

        materias.delete(materia);
        return true;
    }

    // Validar una materia antes de registrarla.
    private void validarMateria(Materia materia) {

        if (materia == null) {
            throw new IllegalArgumentException(
                    "La materia no puede ser nula."
            );
        }

        if (materia.getId() == null || materia.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "El ID de la materia es obligatorio."
            );
        }

        validarDatos(
                materia.getNombre(),
                materia.getCargaHoraria(),
                materia.getAnioCarrera(),
                materia.getTipoEspacioFrecuente()
        );
    }

    // Validar los datos de una materia.
    private void validarDatos(
            String nombre,
            int cargaHoraria,
            int anioCarrera,
            String tipoEspacioFrecuente) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la materia es obligatorio."
            );
        }

        if (cargaHoraria <= 0) {
            throw new IllegalArgumentException(
                    "La carga horaria debe ser mayor que cero."
            );
        }

        if (anioCarrera <= 0) {
            throw new IllegalArgumentException(
                    "El año de carrera debe ser mayor que cero."
            );
        }

        if (tipoEspacioFrecuente == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un tipo de espacio."
            );
        }

        String tipo = tipoEspacioFrecuente.trim()
                .toLowerCase(Locale.ROOT);

        if (!tipo.equals("aula")
                && !tipo.equals("computadoras")
                && !tipo.equals("laboratorio")) {

            throw new IllegalArgumentException(
                    "El tipo de espacio debe ser aula, computadoras o laboratorio."
            );
        }
    }
}
