
package organizador.controladores;

import organizador.modelos.Configuracion;
import organizador.modelos.ModoFuncionamiento;
import organizador.servicios.ConfiguracionService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Controlador de la configuración del período académico.
 *
 * Permite al administrador definir los períodos de recepción de solicitudes
 * y de cursada.
 *
 * Los botones de planificación inicial y reintentos permanecen visibles
 * durante todo el año, pero se deshabilitan durante la recepción.
 *
 * Acceso: solo administradores.
 */
@Controller
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(
            ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    /**
     * Muestra la configuración académica vigente.
     *
     * También informa si actualmente se encuentra dentro del período
     * de recepción, para controlar los botones de la pantalla.
     *
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla configuracion
     */
    @GetMapping("/configuracion")
    public String mostrar(Model model) {

        try {
            Configuracion configuracion =
                    configuracionService.obtenerConfiguracion();

            model.addAttribute("configuracion", configuracion);

            model.addAttribute(
                    "estaEnRecepcion",
                    configuracionService.obtenerModoActual()
                            == ModoFuncionamiento.RECEPCION_SOLICITUDES
            );

        } catch (IllegalStateException e) {
            model.addAttribute("configuracion", null);
            model.addAttribute("estaEnRecepcion", false);
        }

        return "configuracion";
    }

    /**
     * Guarda o actualiza la configuración académica.
     *
     * @param fechaInicioRecepcion primer día de recepción
     * @param fechaFinRecepcion último día de recepción
     * @param fechaInicioCursada primer día de cursada
     * @param fechaFinCursada último día de cursada
     * @param mensajes mensajes para mostrar después de la redirección
     * @return redirección a configuracion
     */
    @PostMapping("/configuracion")
    public String guardar(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicioRecepcion,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFinRecepcion,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicioCursada,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFinCursada,

            RedirectAttributes mensajes) {

        try {
            Configuracion configuracion = new Configuracion(
                    "principal",
                    fechaInicioRecepcion,
                    fechaFinRecepcion,
                    fechaInicioCursada,
                    fechaFinCursada
            );

            configuracionService.guardarConfiguracion(configuracion);

            mensajes.addFlashAttribute(
                    "ok",
                    "La configuración académica se guardó correctamente."
            );

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/configuracion";
    }
}
