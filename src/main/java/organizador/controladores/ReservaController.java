package organizador.controladores;

import organizador.servicios.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Recibe los formularios de reservar y cancelar,
 * y vuelve a la página de inicio con un mensaje.
 */
@Controller
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/reservar")
    public String reservar(
            @RequestParam(required = false) String espacio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam(required = false) String usuario,
            RedirectAttributes mensajes) {

        try {
            reservaService.reservar(espacio, fecha, horaInicio, horaFin, usuario);
            mensajes.addFlashAttribute("ok", "¡Reserva guardada!");
        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/#mis-reservas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes mensajes) {

        if (reservaService.cancelar(id)) {
            mensajes.addFlashAttribute("ok", "Reserva cancelada.");
        } else {
            mensajes.addFlashAttribute("error", "La reserva no existe.");
        }

        return "redirect:/#mis-reservas";
    }
}