
package organizador.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import organizador.servicios.GestorEspacios;
import org.springframework.ui.Model;

/**
 * Controlador encargado de gestionar la página de inicio.
 *
 * Recibe las solicitudes del navegador y determina
 * qué página debe mostrar el sistema.
 */
@Controller
public class InicioController {

    private final GestorEspacios gestorEspacios;

    public InicioController(GestorEspacios gestorEspacios) {
        this.gestorEspacios = gestorEspacios;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());
        return "index";
    }
}
