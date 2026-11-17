
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
