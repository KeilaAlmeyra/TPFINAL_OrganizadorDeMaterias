
package organizador.controladores;

import organizador.modelos.ModoFuncionamiento;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.AsignadorService;
import organizador.servicios.ConfiguracionService;
import organizador.servicios.GestorEspacios;
import organizador.servicios.GestorPropuestas;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Controller
public class AsignacionController {

    private final AsignadorService asignador;
    private final UsuarioRepository usuarios;
    private final GestorEspacios gestorEspacios;
    private final GestorPropuestas gestorPropuestas;
    private final ConfiguracionService configuracionService;

    public AsignacionController(
            AsignadorService asignador,
            UsuarioRepository usuarios,
            GestorEspacios gestorEspacios,
            GestorPropuestas gestorPropuestas,
            ConfiguracionService configuracionService) {

        this.asignador = asignador;
        this.usuarios = usuarios;
        this.gestorEspacios = gestorEspacios;
        this.gestorPropuestas = gestorPropuestas;
        this.configuracionService = configuracionService;
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

        return "asignaciones";
    }

    @GetMapping("/materias-pendientes")
    public String mostrarMateriasPendientes(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioActual(authentication);

        if (!usuario.esAdministrador()) {
            return "redirect:/";
        }

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

        model.addAttribute("esAdministrador", true);
        model.addAttribute("usuario", usuario);

        return "materias-pendientes";
    }

    @GetMapping("/mis-asignaciones")
    public String mostrarMisAsignaciones(
            Authentication authentication,
            Model model) {

        Usuario usuario = obtenerUsuarioActual(authentication);
        boolean esAdministrador = usuario.esAdministrador();

        model.addAttribute(
                "asignaciones",
                asignador.obtenerAsignaciones(usuario.getId(), esAdministrador)
        );

        model.addAttribute(
                "solicitudes",
                asignador.obtenerResultados(usuario.getId(), esAdministrador)
        );

        // Solo se muestran las propuestas que el profesor todavía puede responder.
        model.addAttribute(
                "propuestas",
                esAdministrador
                        ? List.of()
                        : gestorPropuestas.obtenerPropuestasPendientesProfesor(
                        usuario.getId())
        );

        model.addAttribute("esAdministrador", esAdministrador);
        model.addAttribute("usuario", usuario);

        return "mis-asignaciones";
    }



    /**
     * Muestra el formulario para modificar una asignación.
     */
    @GetMapping("/asignaciones/{id}/editar")
    public String mostrarFormularioEditarAsignacion(
            @PathVariable String id,
            Authentication authentication,
            Model model,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            if (!usuario.esAdministrador()) {
                mensajes.addFlashAttribute(
                        "error",
                        "No tenés permiso para modificar asignaciones."
                );
                return "redirect:/mis-asignaciones";
            }

            model.addAttribute(
                    "asignacion",
                    asignador.obtenerAsignacionParaEditar(id)
            );

            model.addAttribute(
                    "espaciosDisponibles",
                    gestorEspacios.obtenerDisponibles()
            );

            model.addAttribute("diasSemana", DayOfWeek.values());

            return "editar-asignacion";

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mis-asignaciones";
        }
    }

    /**
     * Modifica una asignación aprobada.
     */
    @PostMapping("/asignaciones/{id}/editar")
    public String editarAsignacion(
            @PathVariable String id,
            Authentication authentication,
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
                throw new IllegalArgumentException(
                        "No tenés permiso para modificar asignaciones."
                );
            }

            asignador.modificarAsignacion(
                    id,
                    espacioId,
                    dias,
                    horaInicio,
                    horaFin
            );

            mensajes.addFlashAttribute(
                    "ok",
                    "La asignación fue modificada correctamente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mis-asignaciones";
    }

    /**
     * Cancela una asignación aprobada.
     */
    @PostMapping("/asignaciones/{id}/cancelar")
    public String cancelarAsignacion(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);



            asignador.cancelarAsignacion(
                    id,
                    usuario.getId(),
                    usuario.esAdministrador()
            );

            mensajes.addFlashAttribute(
                    "ok",
                    "La asignación fue cancelada correctamente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/mis-asignaciones";
    }


    /**
     * Devuelve los horarios disponibles para una propuesta alternativa.
     */
    @GetMapping("/propuestas/horarios-disponibles")
    @ResponseBody
    public List<GestorPropuestas.HorarioDisponible> obtenerHorariosDisponibles(
            Authentication authentication,
            @RequestParam String solicitudId,
            @RequestParam String espacioId,
            @RequestParam(required = false) List<DayOfWeek> dias) {

        Usuario usuario = obtenerUsuarioActual(authentication);

        if (!usuario.esAdministrador()) {
            throw new IllegalArgumentException(
                    "No tenés permiso para consultar estos horarios."
            );
        }

        return gestorPropuestas.obtenerHorariosDisponibles(
                solicitudId,
                espacioId,
                dias
        );
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
            @RequestParam(required = false) String origen,
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

        if ("solicitudes-pendientes".equals(origen)) {
            return "redirect:/solicitudes-pendientes";
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

    /**
     * Cancela una propuesta alternativa enviada al profesor.
     */
    @PostMapping("/propuestas/{id}/cancelar")
    public String cancelarPropuesta(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes mensajes) {

        try {
            Usuario usuario = obtenerUsuarioActual(authentication);

            if (!usuario.esAdministrador()) {
                throw new IllegalArgumentException(
                        "No tenés permiso para cancelar propuestas."
                );
            }

            gestorPropuestas.cancelarPropuesta(id);

            mensajes.addFlashAttribute(
                    "ok",
                    "Propuesta cancelada. La solicitud volvió a estar disponible."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/solicitudes-pendientes";
    }

    /**
     * Edita una propuesta pendiente enviada al profesor.
     */
    @PostMapping("/propuestas/{id}/editar")
    public String editarPropuesta(
            @PathVariable String id,
            Authentication authentication,
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
                throw new IllegalArgumentException(
                        "No tenés permiso para editar propuestas."
                );
            }

            gestorPropuestas.editarPropuesta(
                    id,
                    espacioId,
                    dias,
                    horaInicio,
                    horaFin
            );

            mensajes.addFlashAttribute(
                    "ok",
                    "Propuesta modificada correctamente."
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/solicitudes-pendientes";
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
                    Solicitud.CLASE_REGULAR
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
            if (configuracionService.obtenerModoActual()
                    == ModoFuncionamiento.RECEPCION_SOLICITUDES) {

                throw new IllegalStateException(
                        "No se pueden procesar solicitudes durante "
                                + "el período de recepción."
                );
            }

            AsignadorService.ResultadoProcesamiento resultado =
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

        return "redirect:/configuracion";
    }

    @PostMapping("/prueba-asignaciones/reintentar")
    public String reintentar(RedirectAttributes mensajes) {

        try {
            if (configuracionService.obtenerModoActual()
                    == ModoFuncionamiento.RECEPCION_SOLICITUDES) {

                throw new IllegalStateException(
                        "No se pueden reintentar asignaciones durante "
                                + "el período de recepción."
                );
            }

            AsignadorService.ResultadoProcesamiento resultado =
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

        return "redirect:/configuracion";
    }
}
