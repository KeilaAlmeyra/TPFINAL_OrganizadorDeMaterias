
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

/**
 * Controlador de las materias de la carrera.
 * Acceso: solo administradores ({@code SeguridadConfig} protege {@code /materias/**}).
 * Las materias aportan datos que se usan al calcular la prioridad de una solicitud:
 * año de carrera, si es troncal y qué tipo de espacio suele necesitar.
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
@Controller
public class MateriaController {

    private final GestorMaterias gestorMaterias;

    public MateriaController(GestorMaterias gestorMaterias) {
        this.gestorMaterias = gestorMaterias;
    }

    /**
     * Muestra el listado de materias y el formulario de alta o edición.
     * Ruta: {@code GET /materias}. Con {@code ?editar=CODIGO} y una materia existente, se
     * envía como {@code materiaEditar} para precargar el formulario.
     *
     * @param editar código de la materia a modificar (opcional)
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code materias}
     */
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

    /**
     * Registra una materia nueva o modifica una existente.
     * Ruta: {@code POST /materias/guardar}. Un solo método atiende los dos casos: si no hay
     * ninguna materia con ese código, se crea; si ya existe, se actualiza.
     *
     * @param id código de la materia
     * @param nombre nombre de la materia
     * @param cargaHoraria carga horaria total
     * @param troncal si es troncal; si la casilla no se marca, el navegador no envía nada y vale {@code false}
     * @param anioCarrera año de la carrera en que se cursa
     * @param tipoEspacioFrecuente tipo de espacio que suele necesitar (aula, computadoras o laboratorio)
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /materias}
     */
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

    /**
     * Elimina una materia.
     * Ruta: {@code POST /materias/eliminar/{id}}. Si el código no existe, se informa el error.
     *
     * @param id código de la materia a eliminar
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /materias}
     */
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
