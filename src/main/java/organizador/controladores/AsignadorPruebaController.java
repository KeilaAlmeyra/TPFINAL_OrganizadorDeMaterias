
package organizador.controladores;

import organizador.modelos.ModoFuncionamiento;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.AsignadorPruebaService;
import organizador.servicios.DatosPruebaAsignacionesService;
import organizador.servicios.GestorEspacios;
import organizador.servicios.GestorPropuestas;


import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Controller
public class AsignadorPruebaController {

    private final AsignadorPruebaService asignador;
    private final DatosPruebaAsignacionesService datosPrueba;
    private final UsuarioRepository usuarios;

    private final GestorEspacios gestorEspacios;
    private final GestorPropuestas gestorPropuestas;

    public AsignadorPruebaController(
            AsignadorPruebaService asignador,
            DatosPruebaAsignacionesService datosPrueba,
            UsuarioRepository usuarios,GestorEspacios gestorEspacios,
            GestorPropuestas gestorPropuestas) {

        this.asignador = asignador;
        this.datosPrueba = datosPrueba;
        this.usuarios = usuarios;
        this.gestorEspacios = gestorEspacios;
        this.gestorPropuestas = gestorPropuestas;

    }

    private Usuario obtenerUsuarioActual(Authentication authentication) {
        return usuarios.findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el usuario de la sesión."
                        ));
    }

    @GetMapping("/prueba-asignaciones")
    public String mostrar(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioActual(authentication);
        boolean esAdministrador = usuario.esAdministrador();

        model.addAttribute(
                "profesores",
                esAdministrador
                        ? asignador.obtenerProfesores()
                        : List.of(usuario)
        );

        model.addAttribute("materias", asignador.obtenerMaterias());
        model.addAttribute(
                "solicitudes",
                asignador.obtenerResultados(usuario.getId(), esAdministrador)
        );

        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("usuario", usuario);

        model.addAttribute("actividades", List.of(
                Solicitud.EXAMEN_FINAL,
                Solicitud.PARCIAL,
                Solicitud.RECUPERATORIO,
                Solicitud.CLASE_OBLIGATORIA,
                Solicitud.CLASE_REGULAR
        ));

        if (esAdministrador) {

            model.addAttribute(
                    "solicitudesPendientes",
                    asignador.obtenerResultados(usuario.getId(), true)
                            .stream()
                            .filter(s -> Solicitud.PENDIENTE.equals(s.estado()))
                            .toList()
            );

            model.addAttribute(
                    "espaciosDisponibles",
                    gestorEspacios.obtenerDisponibles()
            );
        }

        return "prueba-asignaciones";
    }


    @GetMapping("/mis-asignaciones")
    public String mostrarMisAsignaciones(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioActual(authentication);
        boolean esAdministrador = usuario.esAdministrador();

        model.addAttribute(
                "asignaciones",
                asignador.obtenerAsignaciones(
                        usuario.getId(),
                        esAdministrador
                )
        );

        model.addAttribute(
                "propuestas",
                esAdministrador
                        ? List.of()
                        : gestorPropuestas.obtenerPropuestasProfesor(usuario.getId())
        );

        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("usuario", usuario);

        return "mis-asignaciones";
    }


    @PostMapping("/propuestas/crear")
    public String crearPropuesta(
            Authentication authentication,

            @RequestParam String solicitudId,
            @RequestParam String espacioId,
            @RequestParam(required = false) List<DayOfWeek> dias,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime horaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime horaFin,

            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            if (!usuario.esAdministrador()) {
                return "redirect:/";
            }

            gestorPropuestas.crearPropuesta(
                    solicitudId,
                    espacioId,
                    dias,
                    horaInicio,
                    horaFin
            );

            mensajes.addFlashAttribute(
                    "ok",
                    "La propuesta alternativa fue creada correctamente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/propuestas/{id}/aceptar")
    public String aceptarPropuesta(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            if (usuario.esAdministrador()) {
                throw new IllegalArgumentException(
                        "El administrador no puede responder propuestas."
                );
            }

            gestorPropuestas.aceptarPropuesta(id, usuario.getId());

            mensajes.addFlashAttribute(
                    "ok",
                    "Aceptaste la propuesta. La asignación fue confirmada."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mis-asignaciones";
    }


    @PostMapping("/propuestas/{id}/rechazar")
    public String rechazarPropuesta(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            if (usuario.esAdministrador()) {
                throw new IllegalArgumentException(
                        "El administrador no puede responder propuestas."
                );
            }

            gestorPropuestas.rechazarPropuesta(id, usuario.getId());

            mensajes.addFlashAttribute(
                    "ok",
                    "Rechazaste la propuesta. Tu solicitud original sigue pendiente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mis-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/solicitar")
    public String solicitar(
            Authentication authentication,

            @RequestParam(required = false) String profesorId,
            @RequestParam String materiaId,
            @RequestParam int cantidadAlumnos,
            @RequestParam(required = false) List<DayOfWeek> dias,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime horaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime horaFin,

            @RequestParam String tipoActividad,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            // El profesor no puede elegir otra identidad.
            // El administrador sí puede seleccionar al profesor.
            String idProfesor = usuario.esAdministrador()
                    ? profesorId
                    : usuario.getId();

            if (idProfesor == null || idProfesor.isBlank()) {
                throw new IllegalArgumentException(
                        "Debe seleccionar un profesor."
                );
            }

            ModoFuncionamiento modo = asignador.crearSolicitud(
                    idProfesor,
                    materiaId,
                    cantidadAlumnos,
                    dias,
                    horaInicio,
                    horaFin,
                    tipoActividad
            );

            if (modo == ModoFuncionamiento.RECEPCION_SOLICITUDES) {
                mensajes.addFlashAttribute(
                        "ok",
                        "Solicitud recibida correctamente. "
                                + "Quedará pendiente hasta finalizar "
                                + "el período de recepción."
                );

            } else if (modo == ModoFuncionamiento.CURSADA) {
                mensajes.addFlashAttribute(
                        "ok",
                        "Solicitud recibida y evaluada individualmente. "
                                + "Consultá su estado para conocer el resultado."
                );
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/procesar")
    public String procesar(RedirectAttributes mensajes) {

        try {
            AsignadorPruebaService.ResultadoProcesamiento resultado =
                    asignador.procesarPendientes();

            mensajes.addFlashAttribute(
                    "ok",
                    "Procesamiento terminado: "
                            + resultado.aprobadas() + " aprobadas y "
                            + resultado.rechazadas() + " rechazadas."
            );

        } catch (IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/reintentar")
    public String reintentar(RedirectAttributes mensajes) {

        try {
            AsignadorPruebaService.ResultadoProcesamiento resultado =
                    asignador.reintentarPendientes();

            mensajes.addFlashAttribute(
                    "ok",
                    "Reintento terminado: "
                            + resultado.aprobadas() + " solicitudes aprobadas y "
                            + resultado.rechazadas() + " rechazadas. "
                            + "Las que siguen sin espacio permanecen pendientes."
            );

        } catch (IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/prueba-asignaciones";
    }

    @PostMapping("/prueba-asignaciones/cargar-datos")
    public String cargarDatos(RedirectAttributes mensajes) {

        try {
            datosPrueba.cargar();

            mensajes.addFlashAttribute(
                    "ok",
                    "Datos de ejemplo cargados (sin duplicarlos)."
            );

        } catch (RuntimeException e) {
            mensajes.addFlashAttribute(
                    "error",
                    "No se pudieron cargar los datos: " + e.getMessage()
            );
        }

        return "redirect:/prueba-asignaciones";
    }
}
