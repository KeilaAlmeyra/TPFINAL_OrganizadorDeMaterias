
package organizador.servicios;

import organizador.modelos.Configuracion;
import organizador.modelos.ModoFuncionamiento;
import organizador.repositorios.ConfiguracionRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ConfiguracionService {

    private static final String ID_CONFIGURACION = "principal";

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(
            ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    /**
     * Obtiene la configuración general del sistema.
     */
    public Configuracion obtenerConfiguracion() {
        return configuracionRepository.findById(ID_CONFIGURACION)
                .orElseThrow(() -> new IllegalStateException(
                        "Todavía no se configuró el período académico"));
    }

    /**
     * Determina el modo de funcionamiento según la fecha actual.
     */
    public ModoFuncionamiento obtenerModoActual() {
        return obtenerModo(LocalDate.now());
    }

    /**
     * Determina el modo para una fecha específica.
     * Este método también permite probar distintos períodos.
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
     * Guarda la configuración general.
     * Si ya existe una configuración principal, la actualiza.
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

