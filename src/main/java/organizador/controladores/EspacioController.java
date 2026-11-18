
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


/**
 * Controlador de los espacios físicos de la facultad (aulas, aulas con computadoras y
 * laboratorios).
 * La consulta está abierta a cualquier usuario con sesión; el alta, la modificación y la
 * baja son exclusivas del administrador ({@code SeguridadConfig} protege {@code /espacios/**}).
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
@Controller
public class EspacioController {

    private final GestorEspacios gestorEspacios;

    public EspacioController(GestorEspacios gestorEspacios) {
        this.gestorEspacios = gestorEspacios;
    }

    /**
     * Muestra el listado de espacios, solo para consulta.
     * Ruta: {@code GET /consulta-espacios}. Acceso: cualquier usuario con sesión iniciada.
     *
     * @param model datos que se envían a la plantilla ({@code espacios})
     * @return nombre de la plantilla {@code consulta-espacios}
     */
    @GetMapping("/consulta-espacios")
    public String consultar(Model model) {
        model.addAttribute("espacios", gestorEspacios.obtenerEspacios());
        return "consulta-espacios";
    }

    /**
     * Muestra la pantalla de administración de espacios.
     * Ruta: {@code GET /espacios}. Acceso: solo administradores. Si se agrega el parámetro
     * {@code ?editar=CODIGO} y el espacio existe, se envía como {@code espacioEditar} para
     * que el formulario aparezca precargado.
     *
     * @param editar código del espacio a modificar (opcional)
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code espacios}
     */
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

    /**
     * Registra un espacio nuevo.
     * Ruta: {@code POST /espacios/registrar}. El espacio se crea siempre en estado
     * {@code "disponible"}. Se eliminan los espacios en blanco al principio y al final de los
     * textos. Si el código ya existe, se informa el error.
     *
     * @param id código único del espacio (por ejemplo {@code LAB-1})
     * @param nombre nombre para mostrar
     * @param capacidadMaxima cantidad máxima de personas
     * @param tipoEspacio tipo de espacio (aula, computadoras o laboratorio)
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /espacios}
     */
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

    /**
     * Modifica un espacio existente.
     * Ruta: {@code POST /espacios/modificar}. A diferencia del alta, acá también se puede
     * cambiar el estado, por ejemplo para dejar un espacio fuera de servicio.
     *
     * @param id código del espacio a modificar (no cambia)
     * @param nombre nombre nuevo
     * @param capacidadMaxima capacidad nueva
     * @param tipoEspacio tipo nuevo
     * @param estado estado nuevo
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /espacios}
     */
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

    /**
     * Elimina un espacio.
     * Ruta: {@code POST /espacios/eliminar/{id}}. Si el código no existe, se informa el error.
     *
     * @param id código del espacio a eliminar
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /espacios}
     */
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
