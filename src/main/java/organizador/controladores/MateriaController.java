
package organizador.controladores;

import organizador.modelos.Materia;
import organizador.servicios.GestorMaterias;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MateriaController {

    private final GestorMaterias gestorMaterias;

    public MateriaController(GestorMaterias gestorMaterias) {
        this.gestorMaterias = gestorMaterias;
    }

    // Mostrar todas las materias.
    @GetMapping("/materias")
    public String mostrar(
            @RequestParam(required = false) String editar,
            Model model) {

        model.addAttribute(
                "materias",
                gestorMaterias.obtenerMaterias()
        );

        if (editar != null) {
            Materia materia = gestorMaterias.buscarPorId(editar);

            if (materia != null) {
                model.addAttribute("materiaEditar", materia);
            }
        }

        return "materias";
    }

    // Registrar una materia nueva o modificar una existente.
    @PostMapping("/materias/guardar")
    public String guardar(
            @RequestParam String id,
            @RequestParam String nombre,
            @RequestParam int cargaHoraria,
            @RequestParam(defaultValue = "false") boolean troncal,
            @RequestParam int anioCarrera,
            @RequestParam String tipoEspacioFrecuente,
            RedirectAttributes mensajes) {

        try {
            if (gestorMaterias.buscarPorId(id) == null) {

                Materia materia = new Materia(
                        id.trim(),
                        nombre.trim(),
                        cargaHoraria,
                        troncal,
                        anioCarrera,
                        tipoEspacioFrecuente.trim()
                );

                gestorMaterias.registrarMateria(materia);

                mensajes.addFlashAttribute(
                        "ok",
                        "Materia registrada correctamente."
                );

            } else {

                gestorMaterias.modificarMateria(
                        id,
                        nombre,
                        cargaHoraria,
                        troncal,
                        anioCarrera,
                        tipoEspacioFrecuente
                );

                mensajes.addFlashAttribute(
                        "ok",
                        "Materia modificada correctamente."
                );
            }

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/materias";
    }

    // Eliminar una materia.
    @PostMapping("/materias/eliminar/{id}")
    public String eliminar(
            @PathVariable String id,
            RedirectAttributes mensajes) {

        try {
            boolean eliminada = gestorMaterias.eliminarMateria(id);

            if (eliminada) {
                mensajes.addFlashAttribute(
                        "ok",
                        "Materia eliminada correctamente."
                );
            } else {
                mensajes.addFlashAttribute(
                        "error",
                        "No se encontró la materia."
                );
            }

        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/materias";
    }
}
