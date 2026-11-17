
package organizador.seguridad;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicioUsuarioDetalles implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public ServicioUsuarioDetalles(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado."));

        // Los usuarios antiguos o sin contraseña no pueden iniciar sesión.
        if (usuario.getContrasena() == null
                || usuario.getContrasena().isBlank()) {
            throw new UsernameNotFoundException(
                    "El usuario todavía no tiene una contraseña configurada.");
        }

        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            throw new UsernameNotFoundException(
                    "El usuario no tiene un rol válido.");
        }

        String rol = usuario.getRol().trim().toUpperCase();

        // Evita agregar ROLE_ dos veces si ya estuviera guardado así.
        if (rol.startsWith("ROLE_")) {
            rol = rol.substring(5);
        }

        if (!rol.equals("PROFESOR") && !rol.equals("ADMINISTRADOR")) {
            throw new UsernameNotFoundException("El rol del usuario no es válido.");
        }

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasena())
                .authorities("ROLE_" + rol)
                .build();
    }
}
