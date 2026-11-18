package organizador.servicios;

import organizador.modelos.Espacio;
import organizador.repositorios.EspacioRepository;
import organizador.repositorios.ReservaRepository;
import organizador.repositorios.AsignacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Representa el servicio encargado de la administración y gestión del ciclo de vida
 * de los espacios físicos (aulas, laboratorios, salas de computación).
 * Centraliza las operaciones de registro, consulta, modificación, validación y eliminación
 * de espacios, asegurando la consistencia e integridad referencial respecto a reservas y asignaciones.
 */
@Service
public class GestorEspacios {

    private final EspacioRepository repositorio;
    private final ReservaRepository reservas;
    private final AsignacionRepository asignaciones;

    /**
     * Objetivo:
     * Inicializar el servicio inyectando los repositorios necesarios para la persistencia
     * y comprobación de integridad de los espacios físicos.
     *
     * Precondiciones:
     * - Las dependencias de repositorios deben estar instanciadas y disponibles en el contexto de Spring.
     *
     * Parámetros:
     * @param repositorio Repositorio principal de datos para la entidad {@link Espacio}.
     * @param reservas Repositorio de acceso a las reservas vinculadas a espacios.
     * @param asignaciones Repositorio de acceso a las asignaciones académicas vigentes.
     *
     * Retorno:
     * Instancia configurada de {@link GestorEspacios}.
     *
     * Postcondiciones:
     * - Las referencias a los repositorios quedan establecidas de forma inmutable.
     */
    public GestorEspacios(
            EspacioRepository repositorio,
            ReservaRepository reservas,
            AsignacionRepository asignaciones) {

        this.repositorio = repositorio;
        this.reservas = reservas;
        this.asignaciones = asignaciones;
    }

    /**
     * Objetivo:
     * Registrar un nuevo espacio físico en el sistema tras validar su formato, unicidad de identificador
     * y normalizar sus valores de tipo y estado.
     *
     * Precondiciones:
     * - {@code espacio} no debe ser nulo.
     * - El identificador del espacio debe ser no nulo, no vacío y no existir previamente en la base de datos.
     * - Los datos de nombre, capacidad (mayor a cero), tipo y estado deben satisfacer las reglas de validación.
     *
     * Parámetros:
     * @param espacio Entidad {@link Espacio} a registrar.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - El espacio queda normalizado (textos limpios, tipo y estado en minúsculas) y persistido en la base de datos.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code espacio} es nulo, si su ID está vacío, si el ID ya existe,
     *                                  o si los datos de capacidad, tipo o estado no cumplen las validaciones de negocio.
     */
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

    /**
     * Objetivo:
     * Obtener el listado completo de todos los espacios físicos registrados, ordenados ascendentemente por su identificador.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Espacio}> Lista de todos los espacios ordenados por ID. Retorna lista vacía si no hay registros.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en el repositorio no son modificados.
     */
    // Obtener todos los espacios.
    public List<Espacio> obtenerEspacios() {
        return repositorio.findAll(Sort.by("id"));
    }

    /**
     * Objetivo:
     * Localizar un espacio físico específico a partir de su código identificador.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * @param id Cadena de texto (String) con el identificador del espacio.
     *
     * Retorno:
     * @return La entidad {@link Espacio} correspondiente si existe en la base de datos;
     *         o {@code null} si {@code id} es nulo, está en blanco o no existe el registro.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no genera efectos secundarios.
     */
    // Buscar un espacio por su ID.
    public Espacio buscarPorId(String id) {

        if (id == null || id.isBlank()) {
            return null;
        }

        return repositorio.findById(id.trim()).orElse(null);
    }

    /**
     * Objetivo:
     * Recuperar únicamente los espacios físicos cuyo estado operativo actual sea disponible.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * Ninguno.
     *
     * Retorno:
     * @return {@link List}<{@link Espacio}> Lista con los espacios disponibles. Retorna lista vacía si ninguno está disponible.
     *
     * Postcondiciones:
     * - Operación de solo lectura; el estado de los registros permanece inmutable.
     */
    // Obtener solamente los espacios disponibles.
    public List<Espacio> obtenerDisponibles() {
        return obtenerEspacios().stream()
                .filter(Espacio::estaDisponible)
                .toList();
    }

    /**
     * Objetivo:
     * Filtrar y obtener los espacios físicos que correspondan a una categoría o tipo determinado.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     *
     * Parámetros:
     * @param tipo Cadena de texto (String) indicando el tipo de espacio a buscar (ej. "aula", "computadoras", "laboratorio").
     *
     * Retorno:
     * @return {@link List}<{@link Espacio}> Lista de espacios que coinciden con el tipo buscado;
     *         o una lista vacía si {@code tipo} es nulo, está en blanco o no hay coincidencias.
     *
     * Postcondiciones:
     * - Operación de solo lectura; no altera el estado de la base de datos.
     */
    // Buscar espacios de un tipo determinado.
    public List<Espacio> buscarPorTipo(String tipo) {

        if (tipo == null || tipo.isBlank()) {
            return List.of();
        }

        return obtenerEspacios().stream()
                .filter(espacio -> espacio.esDelTipo(tipo.trim()))
                .toList();
    }

    /**
     * Objetivo:
     * Actualizar los atributos de un espacio existente en el sistema tras validar y normalizar los nuevos valores.
     *
     * Precondiciones:
     * - El espacio identificado por {@code id} debe existir previamente en la base de datos.
     * - {@code nombre} no debe ser nulo ni estar en blanco.
     * - {@code capacidadMaxima} debe ser un entero estrictamente mayor que cero.
     * - {@code tipoEspacio} debe corresponder a un tipo admitido ("aula", "computadoras", "laboratorio").
     * - {@code estado} debe corresponder a un estado admisible ("disponible", "mantenimiento").
     *
     * Parámetros:
     * @param id Identificador único (String) del espacio a modificar.
     * @param nombre Nuevo nombre descriptivo del espacio.
     * @param capacidadMaxima Nueva capacidad máxima de ocupantes (entero positivo).
     * @param tipoEspacio Tipo o categoría asignada al espacio.
     * @param estado Estado operativo del espacio.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - Los datos del espacio son actualizados y persistidos en la base de datos con formato normalizado.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si no se encuentra el espacio con dicho ID, si el nombre está en blanco,
     *                                  si la capacidad es menor o igual a cero, o si el tipo o estado son inválidos.
     */
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

    /**
     * Objetivo:
     * Eliminar un espacio del sistema comprobando previamente que no posea dependencias activas
     * (reservas o asignaciones académicas vinculadas).
     *
     * Precondiciones:
     * - La base de datos debe ser accesible para operaciones de lectura y borrado.
     *
     * Parámetros:
     * @param id Identificador único (String) del espacio a dar de baja.
     *
     * Retorno:
     * @return {@code true} si el espacio existía y fue eliminado exitosamente;
     *         {@code false} si el ID provisto es nulo, está en blanco o no existe en la base de datos.
     *
     * Postcondiciones:
     * - Si la eliminación se efectúa, el registro del espacio deja de existir en la base de datos.
     * - Si posee dependencias, no se elimina ningún registro.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si el espacio tiene reservas o asignaciones asociadas en el sistema.
     */
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

    /**
     * Objetivo:
     * Verificar que los atributos base de un espacio cumplan con los formatos y valores permitidos por el sistema.
     *
     * Precondiciones:
     * - Ninguna específica.
     *
     * Parámetros:
     * @param nombre Nombre o denominación a validar.
     * @param capacidadMaxima Capacidad numérica a comprobar.
     * @param tipoEspacio Cadena descriptiva del tipo de espacio.
     * @param estado Cadena descriptiva del estado operativo.
     *
     * Retorno:
     * Ninguno (método {@code void}).
     *
     * Postcondiciones:
     * - Finaliza de forma limpia si los datos son correctos; no modifica el estado del sistema.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si el nombre es nulo/en blanco, la capacidad es menor o igual a cero,
     *                                  o si el tipo o estado no pertenecen a los valores predefinidos.
     */
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

    /**
     * Objetivo:
     * Normalizar una cadena de texto representativa del tipo de espacio, eliminando espacios en blanco
     * iniciales/finales y convirtiéndola a minúsculas.
     *
     * Precondiciones:
     * - Ninguna.
     *
     * Parámetros:
     * @param tipo Cadena de texto con el tipo de espacio, o {@code null}.
     *
     * Retorno:
     * @return Cadena normalizada en minúsculas y sin espacios residuales; o cadena vacía {@code ""} si el parámetro es nulo.
     *
     * Postcondiciones:
     * - No muta parámetros ni el estado de la clase.
     */
    private String normalizarTipo(String tipo) {
        return tipo == null
                ? ""
                : tipo.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Objetivo:
     * Normalizar una cadena de texto representativa del estado de un espacio, eliminando espacios en blanco
     * iniciales/finales y convirtiéndola a minúsculas.
     *
     * Precondiciones:
     * - Ninguna.
     *
     * Parámetros:
     * @param estado Cadena de texto con el estado del espacio, o {@code null}.
     *
     * Retorno:
     * @return Cadena normalizada en minúsculas y sin espacios residuales; o cadena vacía {@code ""} si el parámetro es nulo.
     *
     * Postcondiciones:
     * - No muta parámetros ni el estado de la clase.
     */
    private String normalizarEstado(String estado) {
        return estado == null
                ? ""
                : estado.trim().toLowerCase(Locale.ROOT);
    }
}