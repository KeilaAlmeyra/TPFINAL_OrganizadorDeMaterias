package organizador.servicios;

import organizador.modelos.Configuracion;
import organizador.modelos.ModoFuncionamiento;
import organizador.repositorios.ConfiguracionRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Representa el servicio de gestión de la configuración global del sistema y del ciclo lectivo.
 * Administra los parámetros temporales del período académico (fechas de recepción y cursada)
 * y evalúa el modo operativo vigente en función de una fecha de referencia.
 */
@Service
public class ConfiguracionService {

    private static final String ID_CONFIGURACION = "principal";

    private final ConfiguracionRepository configuracionRepository;

    /**
     * Objetivo:
     * Inicializar el servicio inyectando el repositorio de configuración para el acceso a la base de datos.
     *
     * Precondiciones:
     * - {@code configuracionRepository} debe estar instanciado y gestionado por el contenedor de Spring.
     *
     * Parámetros:
     * @param configuracionRepository Repositorio para la persistencia y lectura de entidades {@link Configuracion}.
     *
     * Retorno:
     * Instancia lista para operar de {@link ConfiguracionService}.
     *
     * Postcondiciones:
     * - El atributo inmutable del repositorio queda referenciado en la instancia del servicio.
     */
    public ConfiguracionService(
            ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    /**
     * Objetivo:
     * Recuperar los parámetros de configuración general vigentes en el sistema bajo el identificador predeterminado.
     *
     * Precondiciones:
     * - La base de datos debe ser accesible.
     * - Debe existir previamente un registro guardado con el identificador {@code ID_CONFIGURACION}.
     *
     * Retorno:
     * @return {@link Configuracion} Entidad persistida que contiene las fechas límite de recepción y cursada.
     *
     * Postcondiciones:
     * - Operación de solo lectura; los datos en el repositorio permanecen inmutables.
     *
     * Excepciones:
     * @throws IllegalStateException Si no se encuentra configurado el período académico en la base de datos.
     */
    public Configuracion obtenerConfiguracion() {
        return configuracionRepository.findById(ID_CONFIGURACION)
                .orElseThrow(() -> new IllegalStateException(
                        "Todavía no se configuró el período académico"));
    }

    /**
     * Objetivo:
     * Obtener el modo de funcionamiento en el que se encuentra el sistema considerando la fecha actual del reloj del sistema.
     *
     * Precondiciones:
     * - Debe existir una configuración general válida persistida en el sistema.
     *
     * Retorno:
     * @return {@link ModoFuncionamiento} Estado operativo en curso ({@code RECEPCION_SOLICITUDES}, {@code CURSADA} o {@code FUERA_DE_PERIODO}).
     *
     * Postcondiciones:
     * - Operación libre de efectos secundarios.
     *
     * Excepciones:
     * @throws IllegalStateException Si la configuración del período académico aún no ha sido creada.
     */
    public ModoFuncionamiento obtenerModoActual() {
        return obtenerModo(LocalDate.now());
    }

    /**
     * Objetivo:
     * Determinar el modo de funcionamiento del sistema para una fecha específica suministrada,
     * permitiendo evaluar calendarios operativos o realizar pruebas sobre períodos arbitrarios.
     *
     * Precondiciones:
     * - El parámetro {@code fecha} no debe ser nulo.
     * - Debe existir una configuración académica registrada en la base de datos.
     *
     * Parámetros:
     * @param fecha Instancia de {@link LocalDate} a contrastar contra las ventanas de fechas del ciclo lectivo.
     *
     * Retorno:
     * @return {@link ModoFuncionamiento} Modo correspondiente a la fecha evaluada:
     *         {@link ModoFuncionamiento#RECEPCION_SOLICITUDES} si cae dentro del rango de recepción,
     *         {@link ModoFuncionamiento#CURSADA} si se encuentra dentro del rango de cursada,
     *         o {@link ModoFuncionamiento#FUERA_DE_PERIODO} si no coincide con ninguno de los dos intervalos.
     *
     * Postcondiciones:
     * - No se altera el estado de la base de datos ni de las entidades.
     *
     * Excepciones:
     * @throws IllegalArgumentException Si el parámetro {@code fecha} es {@code null}.
     * @throws IllegalStateException Si la configuración del período académico no existe en el sistema.
     */
    public ModoFuncionamiento obtenerModo(LocalDate fecha) {

        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }

        Configuracion configuracion = obtenerConfiguracion();

        if (configuracion.estaEnRecepcion(fecha)) {
            return ModoFuncionamiento.RECEPCION_SOLICITUDES;
        }

        if (configuracion.estaEnCursada(fecha)) {
            return ModoFuncionamiento.CURSADA;
        }

        return ModoFuncionamiento.FUERA_DE_PERIODO;
    }

    /**
     * Objetivo:
     * Guardar o actualizar la configuración general del período académico, forzando el uso
     * del identificador institucional predeterminado ({@code ID_CONFIGURACION}).
     *
     * Precondiciones:
     * - El parámetro {@code configuracion} no debe ser nulo.
     * - La base de datos debe estar disponible para operaciones de escritura.
     *
     * Parámetros:
     * @param configuracion Instancia de {@link Configuracion} portadora de las fechas de inicio y fin para recepción y cursada.
     *
     * Retorno:
     * @return {@link Configuracion} Entidad persistida en la base de datos con el identificador normalizado.
     *
     * Postcondiciones:
     * - Si existía una configuración previa con id "principal", sus rangos de fechas quedan sobrescritos.
     * - Si no existía, se crea un nuevo registro con identificador "principal".
     *
     * Excepciones:
     * @throws IllegalArgumentException Si {@code configuracion} es {@code null}.
     */
    public Configuracion guardarConfiguracion(
            Configuracion configuracion) {

        if (configuracion == null) {
            throw new IllegalArgumentException(
                    "La configuración no puede ser nula");
        }

        Configuracion nuevaConfiguracion = new Configuracion(
                ID_CONFIGURACION,
                configuracion.getFechaInicioRecepcion(),
                configuracion.getFechaFinRecepcion(),
                configuracion.getFechaInicioCursada(),
                configuracion.getFechaFinCursada()
        );

        return configuracionRepository.save(nuevaConfiguracion);
    }
}
