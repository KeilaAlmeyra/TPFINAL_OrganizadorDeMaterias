package organizador.controladores;

import organizador.modelos.Solicitud;
import organizador.servicios.AsignadorPruebaService;
import organizador.servicios.DatosPruebaAsignacionesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** Página adicional para probar el circuito de solicitudes y asignaciones. */
@Controller
public class AsignadorPruebaController {
    private final AsignadorPruebaService asignador;
    private final DatosPruebaAsignacionesService datosPrueba;

    public AsignadorPruebaController(AsignadorPruebaService asignador,
                                    DatosPruebaAsignacionesService datosPrueba) {
        this.asignador = asignador;
        this.datosPrueba = datosPrueba;
    }

    @GetMapping("/prueba-asignaciones")
    public String mostrar(Model model) {
        model.addAttribute("profesores", asignador.obtenerProfesores());
        model.addAttribute("materias", asignador.obtenerMaterias());
        model.addAttribute("solicitudes", asignador.obtenerResultados());
        model.addAttribute("actividades", List.of(
                Solicitud.EXAMEN_FINAL, Solicitud.PARCIAL,
                Solicitud.RECUPERATORIO, Solicitud.CLASE_OBLIGATORIA,
                Solicitud.CLASE_REGULAR));
        return "prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/solicitar")
    public String solicitar(@RequestParam String profesorId,
                            @RequestParam String materiaId,
                            @RequestParam int cantidadAlumnos,
                            @RequestParam(required = false) List<DayOfWeek> dias,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
                            LocalTime horaInicio,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
                            LocalTime horaFin,
                            @RequestParam String tipoActividad,
                            RedirectAttributes mensajes) {
        try {
            asignador.crearSolicitud(profesorId, materiaId, cantidadAlumnos,
                    dias, horaInicio, horaFin, tipoActividad);
            mensajes.addFlashAttribute("ok", "Solicitud guardada como PENDIENTE.");
        } catch (IllegalArgumentException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/procesar")
    public String procesar(RedirectAttributes mensajes) {
        AsignadorPruebaService.ResultadoProcesamiento resultado =
                asignador.procesarPendientes();
        mensajes.addFlashAttribute("ok", "Procesamiento terminado: "
                + resultado.aprobadas() + " aprobadas y "
                + resultado.rechazadas() + " rechazadas.");
        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/cargar-datos")
    public String cargarDatos(RedirectAttributes mensajes) {
        datosPrueba.cargar();
        mensajes.addFlashAttribute("ok", "Datos de ejemplo cargados (sin duplicarlos).");
        return "redirect:/prueba-asignaciones";
    }
}
