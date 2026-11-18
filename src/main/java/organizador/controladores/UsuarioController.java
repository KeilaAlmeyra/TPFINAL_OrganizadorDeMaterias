
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

/**
 * Controlador de los usuarios del sistema.
 * Acceso: solo administradores ({@code SeguridadConfig} protege {@code /usuarios/**}).
 * Permite ver los usuarios y registrar profesores. Las contraseñas nunca se guardan en
 * texto plano: se cifran con el {@code PasswordEncoder} (BCrypt) antes de guardarlas.
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
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

    /**
     * Muestra el listado de usuarios.
     * Ruta: {@code GET /usuarios}.
     *
     * @param model datos que se envían a la plantilla ({@code usuarios})
     * @return nombre de la plantilla {@code usuarios}
     */
    @GetMapping("/usuarios")
    public String mostrarUsuarios(Model model) {

        model.addAttribute("usuarios", usuarioRepository.findAll());

        return "usuarios";
    }

    /**
     * Registra un profesor nuevo.
     * Ruta: {@code POST /usuarios/registrar}. Validaciones, en este orden:
     * Nombre, correo y contraseña son obligatorios.
     * La contraseña debe tener al menos 12 caracteres.
     * No puede existir otro usuario con el mismo correo.
     * El correo se guarda en minúsculas y el identificador se genera automáticamente.
     * El rol siempre es {@code "profesor"}: los administradores no se crean desde esta pantalla.
     *
     * @param nombre nombre completo
     * @param correo correo electrónico, que se usa para iniciar sesión
     * @param contrasena contraseña en texto; se cifra antes de guardarla
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /usuarios}
     */
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
