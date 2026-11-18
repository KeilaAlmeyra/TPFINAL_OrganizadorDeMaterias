
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

/**
 * Controlador de las reservas de espacios por fecha y horario.
 * Atiende el formulario para reservar, el listado de reservas, la cancelación y la
 * revisión de solicitudes pendientes por parte del administrador.
 * Las reglas de negocio las aplica {@code ReservaService}: no se puede reservar en
 * una fecha pasada, el horario debe estar entre las 08:00 y las 22:00 en bloques de
 * 30 minutos, y el espacio debe estar habilitado.
 * Una reserva puede estar en cuatro estados:
 *   {@code CONFIRMADA}: el horario es del usuario.
 *   {@code PENDIENTE}: el espacio ya estaba ocupado; el administrador decide.
 *   {@code RECHAZADA}: el administrador la rechazó e indicó el motivo.
 *   {@code CANCELADA}: se canceló; el registro se conserva como historial.
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
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

    /**
     * Muestra el formulario para crear una reserva.
     * Ruta: {@code GET /reservar}. Acceso: cualquier usuario con sesión iniciada.
     * Datos que se envían a la plantilla: {@code espaciosDisponibles} (los espacios que se
     * pueden elegir), {@code usuario} (nombre de quien reserva) y {@code hoy} (fecha actual).
     *
     * @param authentication datos de la sesión
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code nueva-reserva}
     */
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

    /**
     * Muestra el listado de reservas.
     * Ruta: {@code GET /mis-reservas}. Un profesor ve solamente las suyas; un administrador
     * ve las de todos, ordenadas por fecha y hora.
     *
     * @param authentication datos de la sesión
     * @param model datos que se envían a la plantilla ({@code reservas} y {@code esAdministrador})
     * @return nombre de la plantilla {@code mis-reservas}
     */
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

    /**
     * Registra una nueva reserva.
     * Ruta: {@code POST /reservar}. Quién reserva (nombre y correo) se toma de la sesión,
     * no del formulario, así nadie puede reservar a nombre de otra persona.
     * Los parámetros son opcionales a propósito ({@code required = false}): si falta alguno,
     * el servicio responde con un mensaje claro en lugar de un error genérico.
     * Si el espacio ya tiene una reserva confirmada en ese horario, la solicitud <b>no se
     * descarta: queda {@code PENDIENTE} para que el administrador la revise
     * Si los datos no cumplen las reglas (fecha pasada, horario fuera de rango, etc.),
     * se vuelve al formulario con el motivo.
     *
     * @param espacio código del espacio elegido
     * @param fecha día de la reserva ({@code AAAA-MM-DD})
     * @param horaInicio hora de inicio ({@code HH:MM})
     * @param horaFin hora de fin ({@code HH:MM})
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /mis-reservas} si salió bien, o a {@code /reservar} si hubo un error
     */
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

    /**
     * Cancela una reserva.
     * Ruta: {@code POST /reservas/{id}/cancelar}. Un usuario solo puede cancelar sus propias
     * reservas; un administrador puede cancelar cualquiera. La reserva no se borra: pasa al
     * estado {@code CANCELADA} y queda en el historial.
     * Si la reserva no existe, ya estaba cancelada o rechazada, o no pertenece al usuario,
     * se informa el error.
     *
     * @param id identificador de la reserva
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /mis-reservas}
     */    @PostMapping("/reservas/{id}/cancelar")
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


    /**
     * Muestra las solicitudes pendientes de revisión.
     * Ruta: {@code GET /solicitudes-pendientes}. Acceso: solo administradores. Está
     * protegida en {@code SeguridadConfig} y además se verifica el rol acá: si no es
     * administrador, se lo redirige a la página principal.
     *
     * @param authentication datos de la sesión
     * @param model datos que se envían a la plantilla ({@code solicitudes})
     * @return nombre de la plantilla {@code solicitudes-pendientes}, o redirección a {@code /}
     */
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

    /**
     * Aprueba una solicitud pendiente.
     * Ruta: {@code POST /reservas/{id}/aprobar}. Acceso: solo administradores. El servicio
     * comprueba que la solicitud siga pendiente y que el horario ya no esté ocupado; si todo
     * está bien, la reserva pasa a {@code CONFIRMADA}.
     *
     * @param id identificador de la reserva
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /solicitudes-pendientes}
     */
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

    /**
     * Rechaza una solicitud pendiente.
     * Ruta: {@code POST /reservas/{id}/rechazar}. Acceso: solo administradores. Es obligatorio
     * indicar el motivo, que queda guardado y se le muestra al usuario.
     *
     * @param id identificador de la reserva
     * @param motivo explicación del rechazo
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /solicitudes-pendientes}
     */
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


    /**
     * Busca en la base de datos al usuario que tiene la sesión iniciada.
     * El nombre de la sesión es el correo electrónico, sin distinguir mayúsculas.
     *
     * @param authentication datos de la sesión
     * @return el usuario autenticado
     * @throws IllegalArgumentException si no hay sesión o el usuario no existe
     */
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
