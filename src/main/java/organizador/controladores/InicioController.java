
package organizador.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador encargado de gestionar la página de inicio.
 *
 * Recibe las solicitudes del navegador y determina
 * qué página debe mostrar el sistema.
 */
@Controller
public class InicioController {

    // Atiende las visitas a la dirección principal del sistema.
    @GetMapping("/")
    public String inicio() {
        return "index";
    }
}
