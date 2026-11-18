
package organizador.controladores;

import organizador.modelos.Configuracion;
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
 * Permite al administrador definir dos períodos: la recepción de solicitudes</b>
 * y la cursada. Según la fecha actual, el sistema funciona en uno de tres modos
 * ({@code ModoFuncionamiento}): fuera de período, recepción de solicitudes o cursada.
 * Ese modo decide qué se puede hacer con las solicitudes de espacio
 * (ver {@code AsignacionController}).
 * Acceso: solo administradores.
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
@Controller
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(
            ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    /**
     * Muestra la configuración vigente.
     * Ruta: {@code GET /configuracion}. Si todavía no se guardó ninguna configuración,
     * el servicio lanza {@code IllegalStateException} y se envía {@code configuracion = null},
     * así la pantalla muestra el formulario vacío.
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code configuracion}
     */
    @GetMapping("/configuracion")
    public String mostrar(Model model) {

        try {
            model.addAttribute(
                    "configuracion",
                    configuracionService.obtenerConfiguracion()
            );
        } catch (IllegalStateException e) {
            model.addAttribute("configuracion", null);
        }

        return "configuracion";
    }

    /**
     * Guarda (o actualiza) la configuración del período académico.
     * Ruta: {@code POST /configuracion}. Hay una única configuración en el sistema, con
     * identificador fijo {@code "principal"}: guardar de nuevo reemplaza la anterior.
     * Si los datos no son válidos, se informa el error y no se guarda nada.
     *
     * @param fechaInicioRecepcion primer día en que se reciben solicitudes ({@code AAAA-MM-DD})
     * @param fechaFinRecepcion último día en que se reciben solicitudes
     * @param fechaInicioCursada primer día de la cursada
     * @param fechaFinCursada último día de la cursada
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /configuracion}
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
