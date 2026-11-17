
package organizador.controladores;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuarios")
    public String mostrarUsuarios(Model model) {

        model.addAttribute("usuarios", usuarioRepository.findAll());

        return "usuarios";
    }

    @PostMapping("/usuarios/registrar")
    public String registrarProfesor(
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam String contrasena,
            RedirectAttributes mensajes) {

        nombre = nombre.trim();
        correo = correo.trim().toLowerCase();

        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            mensajes.addFlashAttribute(
                    "error", "Todos los campos son obligatorios."
            );
            return "redirect:/usuarios";
        }

        if (contrasena.length() < 12) {
            mensajes.addFlashAttribute(
                    "error", "La contraseña debe tener al menos 12 caracteres."
            );
            return "redirect:/usuarios";
        }

        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            mensajes.addFlashAttribute(
                    "error", "Ya existe un usuario registrado con ese correo."
            );
            return "redirect:/usuarios";
        }

        Usuario profesor = new Usuario(
                UUID.randomUUID().toString(),
                nombre,
                correo,
                passwordEncoder.encode(contrasena),
                "profesor"
        );

        usuarioRepository.save(profesor);

        mensajes.addFlashAttribute(
                "ok", "Profesor registrado correctamente."
        );

        return "redirect:/usuarios";
    }
}
