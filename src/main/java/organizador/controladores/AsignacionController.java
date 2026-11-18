
package organizador.controladores;

import organizador.modelos.ModoFuncionamiento;
import organizador.modelos.Solicitud;
import organizador.modelos.Usuario;
import organizador.repositorios.UsuarioRepository;
import organizador.servicios.AsignadorService;
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

/**
 * Controlador de las solicitudes de espacio y de su asignación.
 * Es el circuito pensado para la planificación de la cursada: el profesor pide un espacio
 * para ciertos días de la semana< y un horario (a diferencia de {@code ReservaController},
 * que trabaja con una fecha concreta).
 * El resultado depende del modo de funcionamiento del sistema, que se calcula según la
 * fecha actual y la configuración del período ({@code ConfiguracionService}):
 * {@code RECEPCION_SOLICITUDES}: la solicitud queda pendiente hasta que termine la recepción.</li>
 * {@code CURSADA}: la solicitud se evalúa en el momento.
 * {@code FUERA_DE_PERIODO}: no se aceptan solicitudes.
 * Para decidir quién se queda con cada espacio, las solicitudes se ordenan por año de
 * carrera y puntaje de prioridad, y Prolog comprueba la compatibilidad y la disponibilidad.
 * Si no hay lugar, el administrador puede crear una propuesta alternativa que el
 * profesor acepta o rechaza.
 * Los mensajes de éxito ({@code ok}) y de error ({@code error}) se envían con
 * {@code RedirectAttributes.addFlashAttribute}: viven hasta la próxima pantalla
 * y se muestran una sola vez.
 */
@Controller
public class AsignacionController {

    private final AsignadorService asignador;
    private final UsuarioRepository usuarios;

    private final GestorEspacios gestorEspacios;
    private final GestorPropuestas gestorPropuestas;

    public AsignacionController(
            AsignadorService asignador,
            UsuarioRepository usuarios, GestorEspacios gestorEspacios,
            GestorPropuestas gestorPropuestas) {

        this.asignador = asignador;
        this.usuarios = usuarios;
        this.gestorEspacios = gestorEspacios;
        this.gestorPropuestas = gestorPropuestas;

    }

    /**
     * Busca en la base de datos al usuario que tiene la sesión iniciada.
     *
     * @param authentication datos de la sesión; su nombre es el correo del usuario
     * @return el usuario autenticado
     * @throws IllegalStateException si el usuario de la sesión no existe
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        return usuarios.findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el usuario de la sesión."
                        ));
    }


    /**
     * Muestra la pantalla de solicitudes y asignaciones.
     * Ruta: {@code GET /prueba-asignaciones}. Acceso: profesores y administradores.
     * Datos que se envían a la plantilla:
     * {@code profesores}: todos, si es administrador; solo el propio usuario, si es profesor.</li>
     * {@code materias}: materias registradas.
     * {@code solicitudes}: todas, si es administrador; solo las propias, si es profesor.</li>
     * {@code actividades}: tipos de actividad que se pueden elegir (final, parcial, etc.).</li>
     * {@code esAdministrador} y {@code usuario}.
     * Solo para el administrador, además: {@code solicitudesPendientes} y
     * {@code espaciosDisponibles}, que se usan para proponer alternativas.
     *
     * @param authentication datos de la sesión
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla de solicitudes y asignaciones
     */
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

        return "asignaciones";
    }

    /**
     * Muestra las asignaciones confirmadas y las propuestas recibidas.
     * Ruta: {@code GET /mis-asignaciones}. Un profesor ve sus asignaciones y las propuestas
     * alternativas que le hizo el administrador; el administrador ve todas las asignaciones
     * (y ninguna propuesta, porque no las responde).
     *
     * @param authentication datos de la sesión
     * @param model datos que se envían a la plantilla
     * @return nombre de la plantilla {@code mis-asignaciones}
     */
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

    /**
     * Crea una propuesta alternativa para una solicitud pendiente.
     * Ruta: {@code POST /propuestas/crear}. Acceso: solo administradores. Se usa cuando la
     * solicitud no pudo asignarse: el administrador sugiere otro espacio, otros días u otro
     * horario. Antes de guardarla, el servicio comprueba con Prolog que el espacio sea
     * compatible y esté libre en esos días y horas.
     *
     * @param solicitudId identificador de la solicitud pendiente
     * @param espacioId código del espacio propuesto
     * @param dias días de la semana propuestos
     * @param horaInicio hora de inicio propuesta ({@code HH:MM})
     * @param horaFin hora de fin propuesta ({@code HH:MM})
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a la pantalla de solicitudes; a {@code /} si el usuario no es administrador
     */
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

        return "redirect:/asignaciones";
    }

    /**
     * El profesor acepta una propuesta alternativa.
     * Ruta: {@code POST /propuestas/{id}/aceptar}. Solo el profesor dueño de la solicitud puede
     * responderla; el administrador no. Antes de confirmar se vuelve a comprobar que el espacio
     * siga libre. Si todo esta bien, se genera la asignación definitiva.
     *
     * @param id identificador de la propuesta
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /mis-asignaciones}
     */
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

    /**
     * El profesor rechaza una propuesta alternativa.
     * Ruta: {@code POST /propuestas/{id}/rechazar}. La solicitud original no se modifica: sigue
     * pendiente a la espera de otra opción.
     *
     * @param id identificador de la propuesta
     * @param authentication datos de la sesión
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a {@code /mis-asignaciones}
     */
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
     * Presenta una solicitud de espacio.
     * Ruta: {@code POST /prueba-asignaciones/solicitar}. Acceso: profesores y administradores.
     * Un profesor siempre solicita a su nombre, aunque intente enviar otro identificador; el
     * administrador debe elegir el profesor. El mensaje final depende del modo del sistema:
     * en recepción queda pendiente; en cursada se evalúa de inmediato.
     * Los días son opcionales en la firma ({@code required = false}) para que la validación
     * del servicio responda "Debe seleccionar al menos un día" en lugar de un error genérico.
     *
     * @param profesorId profesor solicitante (solo lo usa el administrador)
     * @param materiaId código de la materia
     * @param cantidadAlumnos cantidad de alumnos esperados
     * @param dias días de la semana
     * @param horaInicio hora de inicio ({@code HH:MM})
     * @param horaFin hora de fin ({@code HH:MM})
     * @param tipoActividad tipo de actividad (final, parcial, recuperatorio, clase obligatoria o regular)
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a la pantalla de solicitudes
     */
    @PostMapping("/asignaciones/solicitar")
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

        return "redirect:/asignaciones";
    }

    /**
     * Procesa todas las solicitudes pendientes juntas.
     * Ruta: {@code POST /prueba-asignaciones/procesar}. Acceso: solo administradores. Solo
     * funciona en modo {@code CURSADA}, es decir, una vez cerrada la recepción. Las solicitudes
     * se ordenan por prioridad y se asignan una por una; las que no encuentran espacio se
     * rechazan. Se informa cuántas se aprobaron y cuántas se rechazaron.
     *
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a la pantalla de solicitudes
     */
    @PostMapping("/asignaciones/procesar")
    public String procesar(RedirectAttributes mensajes) {

        try {
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

        return "redirect:/asignaciones";
    }

    /**
     * Vuelve a intentar asignar las solicitudes pendientes.
     * Ruta: {@code POST /prueba-asignaciones/reintentar}. Acceso: solo administradores. Solo
     * funciona en modo {@code CURSADA}. A diferencia de {@link #procesar}, las solicitudes que
     * siguen sin espacio no se rechazan: permanecen pendientes para un próximo intento
     * (por ejemplo, si se liberó un lugar).
     *
     * @param mensajes mensajes que se muestran después de la redirección
     * @return redirección a la pantalla de solicitudes
     */
    @PostMapping("/asignaciones/reintentar")
    public String reintentar(RedirectAttributes mensajes) {

        try {
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

        return "redirect:/asignaciones";
    }
}