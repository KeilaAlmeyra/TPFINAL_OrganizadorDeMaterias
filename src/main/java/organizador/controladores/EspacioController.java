
package organizador.controladores;

import organizador.modelos.Espacio;
import organizador.servicios.GestorEspacios;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EspacioController {

    private final GestorEspacios gestorEspacios;

    public EspacioController(GestorEspacios gestorEspacios) {
        this.gestorEspacios = gestorEspacios;
    }

    // Consulta de espacios para profesores y administradores.
    @GetMapping("/consulta-espacios")
    public String consultar(Model model) {
        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());
        return "consulta-espacios";
    }

    // Administración exclusiva del administrador.
    @GetMapping("/espacios")
    public String mostrar(
            @RequestParam(required = false) String editar,
            Model model) {

        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());

        if (editar != null && !editar.isBlank()) {
            Espacio espacio = gestorEspacios.buscarPorId(editar);

            if (espacio != null) {
                model.addAttribute("espacioEditar", espacio);
            }
        }

        return "espacios";
    }

    // Registrar un nuevo espacio.
    @PostMapping("/espacios/registrar")
    public String registrar(
            @RequestParam String id,
            @RequestParam String nombre,
            @RequestParam int capacidadMaxima,
            @RequestParam String tipoEspacio,
            RedirectAttributes mensajes) {

        try {
            Espacio espacio = new Espacio(
                    id.trim(),
                    nombre.trim(),
                    capacidadMaxima,
                    tipoEspacio.trim(),
                    "disponible"
            );

            gestorEspacios.registrarEspacio(espacio);

            mensajes.addFlashAttribute(
                    "ok", "Espacio registrado correctamente.");

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/espacios";
    }

    // Modificar un espacio existente.
    @PostMapping("/espacios/modificar")
    public String modificar(
            @RequestParam String id,
            @RequestParam String nombre,
            @RequestParam int capacidadMaxima,
            @RequestParam String tipoEspacio,
            @RequestParam String estado,
            RedirectAttributes mensajes) {

        try {
            gestorEspacios.modificarEspacio(
                    id,
                    nombre,
                    capacidadMaxima,
                    tipoEspacio,
                    estado
            );

            mensajes.addFlashAttribute(
                    "ok", "Espacio modificado correctamente.");

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/espacios";
    }

    // Eliminar un espacio.
    @PostMapping("/espacios/eliminar/{id}")
    public String eliminar(
            @PathVariable String id,
            RedirectAttributes mensajes) {

        try {
            boolean eliminado = gestorEspacios.eliminarEspacio(id);

            if (eliminado) {
                mensajes.addFlashAttribute(
                        "ok", "Espacio eliminado correctamente.");
            } else {
                mensajes.addFlashAttribute(
                        "error", "No se encontró el espacio que desea eliminar.");
            }

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/espacios";
    }
}
