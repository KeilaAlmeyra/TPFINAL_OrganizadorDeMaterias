
package organizador.controladores;

import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.GestorEspacios;
import organizador.servicios.ReservaService;
import organizador.modelos.Solicitud;
import organizador.servicios.AsignadorService;
import organizador.modelos.Espacio;
import organizador.servicios.GestorPropuestas;
import organizador.modelos.PropuestaAsignacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final AsignadorService asignadorPruebaService;
    private final GestorPropuestas gestorPropuestas;

    public ReservaController(
            ReservaService reservaService,
            UsuarioRepository usuarioRepository,
            GestorEspacios gestorEspacios,
            AsignadorService asignadorPruebaService,
            GestorPropuestas gestorPropuestas) {

        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
        this.gestorEspacios = gestorEspacios;
        this.asignadorPruebaService = asignadorPruebaService;
        this.gestorPropuestas = gestorPropuestas;
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

        // Solicitudes de reservas puntuales.
        model.addAttribute(
                "solicitudes",
                reservaService.listarPendientes()
        );

        // Solicitudes de asignación de aulas que siguen pendientes.
        List<AsignadorService.FilaSolicitud> todasLasPendientes =
                asignadorPruebaService.obtenerResultados(usuario.getId(), true)
                        .stream()
                        .filter(s -> Solicitud.PENDIENTE.equals(s.estado()))
                        .toList();

// Propuestas que ya fueron enviadas y esperan respuesta.
        List<PropuestaAsignacion> propuestasPendientes =
                gestorPropuestas.obtenerTodasLasPropuestasPendientes();

// Identificamos las solicitudes que ya tienen una propuesta pendiente.
        List<String> solicitudesConPropuesta =
                propuestasPendientes.stream()
                        .map(p -> p.getSolicitud().getId())
                        .toList();

// Solo mostramos para proponer aquellas solicitudes
// que todavía no tienen una propuesta esperando respuesta.
        List<AsignadorService.FilaSolicitud> pendientes =
                todasLasPendientes.stream()
                        .filter(s -> !solicitudesConPropuesta.contains(s.id()))
                        .toList();

        model.addAttribute(
                "solicitudesAsignacionPendientes",
                pendientes
        );

// Enviamos las propuestas pendientes a la segunda sección.
        model.addAttribute(
                "propuestasEsperandoRespuesta",
                propuestasPendientes
        );

// Cada solicitud disponible para proponer recibe sus espacios compatibles.
        Map<String, List<Espacio>> espaciosPorSolicitud = new HashMap<>();

        for (AsignadorService.FilaSolicitud solicitud : pendientes) {
            espaciosPorSolicitud.put(
                    solicitud.id(),
                    gestorPropuestas.obtenerEspaciosCompatibles(solicitud.id())
            );
        }

        model.addAttribute("espaciosPorSolicitud", espaciosPorSolicitud);

// Espacios compatibles para editar cada propuesta pendiente.
        Map<String, List<Espacio>> espaciosPorPropuesta = new HashMap<>();

        for (PropuestaAsignacion propuesta : propuestasPendientes) {
            espaciosPorPropuesta.put(
                    propuesta.getId(),
                    gestorPropuestas.obtenerEspaciosCompatibles(
                            propuesta.getSolicitud().getId()
                    )
            );
        }

        model.addAttribute("espaciosPorPropuesta", espaciosPorPropuesta);
        // Espacios disponibles para proponer alternativas.
        model.addAttribute(
                "espaciosDisponibles",
                gestorEspacios.obtenerDisponibles()
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
