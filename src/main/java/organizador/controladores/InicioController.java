package organizador.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import organizador.servicios.GestorEspacios;
import organizador.servicios.ReservaService;

import java.time.LocalDate;

@Controller
public class InicioController {

    private final GestorEspacios gestorEspacios;
    private final ReservaService reservaService;

    public InicioController(GestorEspacios gestorEspacios,
                            ReservaService reservaService) {
        this.gestorEspacios = gestorEspacios;
        this.reservaService = reservaService;
    }

    @GetMapping("/")
    public String inicio(@RequestParam(required = false) String usuario,
                         Model model) {

        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());
        model.addAttribute("espaciosDisponibles", gestorEspacios.obtenerDisponibles());
        model.addAttribute("reservas", reservaService.listar(usuario));
        model.addAttribute("usuario", usuario);
        model.addAttribute("hoy", LocalDate.now());

        return "index";
    }
}