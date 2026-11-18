
package organizador.controladores;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.GestorEspacios;
import organizador.servicios.ReservaService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * Controlador de la página principal.
 * Es la pantalla a la que llega el usuario después de iniciar sesión. Junta los datos
 * que muestra la portada: los espacios, las reservas del usuario y su rol.
 */
@Controller
public class InicioController {

    private final GestorEspacios gestorEspacios;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public InicioController(GestorEspacios gestorEspacios,
                            ReservaService reservaService,
                            UsuarioRepository usuarioRepository) {
        this.gestorEspacios = gestorEspacios;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Muestra la página principal.
     * Ruta: {@code GET /}. Acceso: cualquier usuario con sesión iniciada.
     * Datos que se envían a la plantilla {@code index}:
     *   {@code espacios}: todos los espacios registrados.
     *   {@code espaciosDisponibles}: solo los espacios habilitados para usarse.</li>
     *   {@code reservas}: las reservas del usuario (todas, si es administrador).</li>
     *   {@code usuario}: nombre de quien inició sesión.
     *   {@code esAdministrador}: permite mostrar u ocultar opciones según el rol.</li>
     *   {@code hoy}: fecha actual.
     * @param authentication datos de la sesión; su nombre es el correo del usuario
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code index}
     * @throws IllegalStateException si el usuario de la sesión no existe en la base de datos
     */
    @GetMapping("/")
    public String inicio(Authentication authentication, Model model) {

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el usuario autenticado."));

        boolean esAdministrador = usuario.esAdministrador();

        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());
        model.addAttribute("espaciosDisponibles",
                gestorEspacios.obtenerDisponibles());

        model.addAttribute("reservas",
                reservaService.listar(usuario.getCorreo(), esAdministrador));

        model.addAttribute("usuario", usuario.getNombre());
        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("hoy", LocalDate.now());

        return "index";
    }
}
