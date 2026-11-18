package organizador.servicios;

import organizador.modelos.Materia;
import organizador.repositorios.MateriaRepository;
import organizador.repositorios.SolicitudRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Representa el servicio encargado de la gestión del catálogo de materias académicas.
 * Centraliza las operaciones CRUD, las validaciones de los datos curriculares
 * y el control de integridad referencial respecto a las solicitudes existentes en el sistema.
 */
@Service
public class GestorMaterias {

    private final MateriaRepository materias;
    private final SolicitudRepository solicitudes;

    /**
     * Objetivo:
     * Inicializar el servicio inyectando los repositorios necesarios para el acceso a datos
     * de materias y la comprobación de dependencias con solicitudes.
     *
     * Precondiciones:
     * - Las dependencias de repositorios deben estar instanciadas y disponibles en el contexto de Spring.
     *
     * Parámetros:
     * @param materias Repositorio de persistencia y consulta para entidades {@link Materia}.
     * @param solicitudes Repositorio de consulta para verificar solicitudes asociadas a materias.
     *
     * Retorno:
     * Instancia configurada de {@link GestorMaterias}.
     *
     * Postcondiciones:
     * - Las referencias a los repositorios quedan establecidas de forma inmutable.
     */
    public GestorMaterias(
            MateriaRepository materias,
            SolicitudRepository solicitudes) {

        this.materias = materias;
        this.solicitudes = solicitudes;
    }

    /**
     * Objetivo:
     * Registrar una nueva materia en el catálogo del sistema previa validación de sus atributos
     * y verificación de no duplicidad de su identificador.
     *
     * Precondiciones:
     * - {@code materia} no debe ser nula.
     * - El identificador de la materia no debe ser nulo ni estar en blanco.
     * - No debe existir previamente una materia con el mismo identificador en la base de datos.
     * - Nombre, carga horaria (> 0), año de carrera (> 0) y tipo de espacio frecuente deben ser válidos.
     *
     * Parámetros:
     * @param materia Entidad {@link Materia} a registrar.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - La materia queda persistida en la base de datos.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code materia} es nula, si el ID es nulo/en blanco,
     *                                  si ya existe una materia con ese ID o si los atributos no cumplen las reglas de negocio.
     */
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

    /**
     * Objetivo:
     * Obtener el catálogo completo de materias registradas, ordenadas alfabéticamente por su nombre.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Materia}> Lista con todas las materias ordenadas de forma ascendente por nombre.
     *         Retorna una lista vacía si no existen materias registradas.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en la base de datos permanecen inmutables.
     */
    // Obtener todas las materias ordenadas por nombre.
    public List<Materia> obtenerMaterias() {
        return materias.findAll(Sort.by("nombre"));
    }

    /**
     * Objetivo:
     * Buscar una materia específica a partir de su identificador único.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * @param id Identificador único (String) de la materia a localizar.
     *
     * Retorno:
     * @return La entidad {@link Materia} correspondiente si existe en la base de datos;
     *         o {@code null} si {@code id} es nulo, está en blanco o no existe el registro.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no genera efectos secundarios.
     */
    // Buscar una materia por su ID.
    public Materia buscarPorId(String id) {

        if (id == null || id.isBlank()) {
            return null;
        }

        return materias.findById(id.trim()).orElse(null);
    }

    /**
     * Objetivo:
     * Actualizar los atributos curriculares de una materia existente previa validación
     * de sus nuevos valores y normalización del tipo de espacio.
     *
     * Precondiciones:
     * - Debe existir previamente en la base de datos una materia con el {@code id} provisto.
     * - {@code nombre} no debe ser nulo ni estar en blanco.
     * - {@code cargaHoraria} y {@code anioCarrera} deben ser enteros mayores que cero.
     * - {@code tipoEspacioFrecuente} debe ser no nulo y corresponder a un tipo admitido ("aula", "computadoras", "laboratorio").
     *
     * Parámetros:
     * @param id Identificador único (String) de la materia a modificar.
     * @param nombre Nuevo nombre curricular de la materia.
     * @param cargaHoraria Cantidad de horas asignadas a la asignatura (entero positivo).
     * @param troncal Booleano que determina si la materia es troncal en el plan de estudios.
     * @param anioCarrera Año académico de la carrera al que pertenece la materia (entero positivo).
     * @param tipoEspacioFrecuente Tipo de aula o espacio físico requerido con mayor frecuencia.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - Los datos de la materia son actualizados y persistidos en la base de datos.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si no se encuentra la materia, si el nombre está en blanco,
     *                                  si la carga horaria o el año son menores o iguales a cero, o si el tipo de espacio es inválido.
     */
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

    /**
     * Objetivo:
     * Dar de baja una materia del sistema asegurando la integridad referencial,
     * permitiendo la eliminación únicamente si no existen solicitudes asociadas a ella.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible para operaciones de lectura y borrado.
     *
     * Parámetros:
     * @param id Identificador único (String) de la materia a eliminar.
     *
     * Retorno:
     * @return {@code true} si la materia existía y fue eliminada exitosamente;
     *         {@code false} si la materia no fue encontrada o si el identificador no es válido.
     *
     * Postcondiciones:
     * - Si se elimina, la entidad {@link Materia} deja de existir en la base de datos.
     * - Si posee solicitudes asociadas, no se altera ningún registro.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si la materia posee solicitudes vinculadas en el sistema.
     */
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

    /**
     * Objetivo:
     * Validar de forma exhaustiva una entidad de tipo {@link Materia} antes de persistirla en el registro.
     *
     * Precondiciones:
     * - Ninguna específica.
     *
     * Parámetros:
     * @param materia Instancia de {@link Materia} a verificar.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - Finaliza de forma limpia si la entidad y sus campos obligatorios son válidos; no muta el estado del objeto.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code materia} es {@code null}, si su identificador es nulo/en blanco,
     *                                  o si cualquiera de sus atributos incumple las validaciones de negocio delegadas.
     */
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

    /**
     * Objetivo:
     * Validar individualmente los campos curriculares y de infraestructura requeridos para una materia.
     *
     * Precondiciones:
     * - Ninguna específica.
     *
     * Parámetros:
     * @param nombre Denominación curricular de la materia.
     * @param cargaHoraria Cantidad de horas asignadas.
     * @param anioCarrera Año de la carrera al que corresponde la materia.
     * @param tipoEspacioFrecuente Cadena con el tipo de espacio físico requerido.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - Concluye sin efectos secundarios si todos los datos cumplen los criterios establecidos.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si el nombre es nulo/en blanco, si la carga horaria o el año de carrera
     *                                  son menores o iguales a cero, o si el tipo de espacio es nulo o distinto de "aula", "computadoras" o "laboratorio".
     */
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