
package organizador.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la pantalla de inicio de sesión.
 * Solo muestra la página. La verificación del correo y la contraseña la hace
 * Spring Security, configurado en {@code SeguridadConfig}.
 */
@Controller
public class LoginController {

    /**
     * Muestra el formulario de inicio de sesión.
     * Ruta: {@code GET /login}. Acceso: público (no hace falta tener sesión).
     * @return nombre de la plantilla {@code login}
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

