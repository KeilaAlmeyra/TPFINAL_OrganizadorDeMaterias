
package organizador.seguridad;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_NOMBRE:}")
    private String nombre;

    @Value("${ADMIN_EMAIL:}")
    private String correo;

    @Value("${ADMIN_PASSWORD:}")
    private String contrasena;

    public InicializadorAdministrador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        // Si ya existe un administrador, no creamos otro.
        boolean existeAdministrador = usuarioRepository.findAll()
                .stream()
                .anyMatch(Usuario::esAdministrador);

        if (existeAdministrador) {
            System.out.println("Ya existe un administrador.");
            return;
        }

        // Si faltan variables, no creamos una cuenta incompleta.
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
            System.out.println(
                    "No se creó el administrador inicial: faltan variables de entorno."
            );
            return;
        }

        // No sobrescribimos una cuenta que ya tenga ese correo.
        if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new IllegalStateException(
                    "El correo configurado para el administrador ya pertenece a otro usuario."
            );
        }

        if (contrasena.length() < 12) {
            throw new IllegalStateException(
                    "La contraseña inicial debe tener al menos 12 caracteres."
            );
        }

        Usuario administrador = new Usuario(
                UUID.randomUUID().toString(),
                nombre,
                correo,
                passwordEncoder.encode(contrasena),
                "administrador"
        );

        usuarioRepository.save(administrador);

        System.out.println("Administrador inicial creado correctamente.");
    }
}
