
package organizador.controladores;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.GestorEspacios;
import organizador.servicios.ReservaService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;
    private final GestorEspacios gestorEspacios;

    public ReservaController(
            ReservaService reservaService,
            UsuarioRepository usuarioRepository,
            GestorEspacios gestorEspacios) {

        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
        this.gestorEspacios = gestorEspacios;
    }

    // Muestra el formulario para crear una reserva.
    @GetMapping("/reservar")
    public String mostrarFormulario(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        model.addAttribute("espaciosDisponibles",
                gestorEspacios.obtenerDisponibles());

        model.addAttribute("usuario", usuario.getNombre());
        model.addAttribute("hoy", LocalDate.now());

        return "nueva-reserva";
    }

    // Muestra las reservas del usuario o todas si es administrador.
    @GetMapping("/mis-reservas")
    public String mostrarReservas(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        model.addAttribute(
                "reservas",
                reservaService.listar(
                        usuario.getCorreo(),
                        usuario.esAdministrador()
                )
        );

        model.addAttribute("esAdministrador", usuario.esAdministrador());

        return "mis-reservas";
    }

    // Registra una nueva reserva.
    @PostMapping("/reservar")
    public String reservar(
            @RequestParam(required = false) String espacio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);

            reservaService.reservar(
                    espacio,
                    fecha,
                    horaInicio,
                    horaFin,
                    usuario.getNombre(),
                    usuario.getCorreo()
            );

            mensajes.addFlashAttribute(
                    "ok",
                    "¡Reserva guardada correctamente!"
            );

            return "redirect:/mis-reservas";

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());

            return "redirect:/reservar";
        }
    }

    // Cancela una reserva respetando los permisos del usuario.
    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);

            boolean esAdministrador = usuario.esAdministrador();

            if (reservaService.cancelar(
                    id,
                    usuario.getCorreo(),
                    esAdministrador)) {

                mensajes.addFlashAttribute(
                        "ok",
                        "Reserva cancelada correctamente."
                );

            } else {
                mensajes.addFlashAttribute(
                        "error",
                        "La reserva no existe o no tenés permiso para cancelarla."
                );
            }

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mis-reservas";
    }


    // Pantalla exclusiva para revisar solicitudes pendientes.
    @GetMapping("/solicitudes-pendientes")
    public String mostrarPendientes(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioAutenticado(authentication);

        if (!usuario.esAdministrador()) {
            return "redirect:/";
        }

        model.addAttribute(
                "solicitudes",
                reservaService.listarPendientes()
        );

        return "solicitudes-pendientes";
    }

    // El administrador aprueba una solicitud.
    @PostMapping("/reservas/{id}/aprobar")
    public String aprobar(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);

            if (!usuario.esAdministrador()) {
                return "redirect:/";
            }

            reservaService.aprobar(id);

            mensajes.addFlashAttribute(
                    "ok",
                    "La solicitud fue aprobada correctamente."
            );

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/solicitudes-pendientes";
    }

    // El administrador rechaza una solicitud indicando el motivo.
    @PostMapping("/reservas/{id}/rechazar")
    public String rechazar(
            @PathVariable Long id,
            @RequestParam String motivo,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioAutenticado(authentication);

            if (!usuario.esAdministrador()) {
                return "redirect:/";
            }

            reservaService.rechazar(id, motivo);

            mensajes.addFlashAttribute(
                    "ok",
                    "La solicitud fue rechazada."
            );

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/solicitudes-pendientes";
    }


    private Usuario obtenerUsuarioAutenticado(
            Authentication authentication) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Debés iniciar sesión."
            );
        }

        return usuarioRepository
                .findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el usuario autenticado."
                ));
    }
}
