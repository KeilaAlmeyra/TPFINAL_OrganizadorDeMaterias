
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

@Controller
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(
            ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

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
