
package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.Espacio;
import organizador.modelos.Reserva;
import organizador.modelos.Solicitud;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Puente entre Java y SWI-Prolog.
 *
 * Java obtiene los datos actuales desde H2 y los envía a SWI-Prolog.
 * Prolog aplica las reglas de compatibilidad y conflictos horarios y
 * devuelve los identificadores de los espacios que cumplen las condiciones.
 */
@Service
public class PrologService {

    private final String ejecutable;
    private final Path archivoReglas;
    private final ConfiguracionService configuracionService;

    public PrologService(
            @Value("${prolog.ejecutable:C:/Program Files/swipl/bin/swipl.exe}")
            String ejecutable,
            @Value("${prolog.archivo:src/main/prolog/organizador.pl}")
            String archivoReglas,
            ConfiguracionService configuracionService) {

        this.ejecutable = ejecutable;
        this.archivoReglas = Path.of(archivoReglas).toAbsolutePath().normalize();
        this.configuracionService = configuracionService;
    }

    /**
     * Devuelve el primer espacio compatible según el orden recibido.
     * La compatibilidad, la capacidad y los conflictos son evaluados por Prolog.
     */
    public Espacio buscarEspacioCompatible(
            Solicitud solicitud,
            List<Espacio> espacios,
            List<Asignacion> asignaciones,
            List<Reserva> reservas) {

        if (solicitud == null || espacios == null || espacios.isEmpty()) {
            return null;
        }

        List<String> idsDisponibles = consultarEspacios(
                solicitud, espacios, asignaciones, reservas);

        for (Espacio espacio : espacios) {
            if (idsDisponibles.contains(espacio.getId())) {
                return espacio;
            }
        }

        return null;
    }

    /**
     * Comprueba si un espacio específico está disponible
     * para los días y horarios propuestos.
     *
     * Reutiliza las mismas reglas Prolog que las solicitudes originales.
     */
    public boolean validarDisponibilidadPropuesta(
            Solicitud solicitud,
            Espacio espacio,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            List<Asignacion> asignaciones,
            List<Reserva> reservas) {

        return validarDisponibilidadPropuesta(
                solicitud,
                espacio,
                dias,
                horaInicio,
                horaFin,
                asignaciones,
                reservas,
                null
        );
    }

    /**
     * Comprueba la disponibilidad excluyendo una asignación existente.
     * Se utiliza al modificar una asignación para evitar que entre
     * en conflicto consigo misma.
     */
    public boolean validarDisponibilidadPropuesta(
            Solicitud solicitud,
            Espacio espacio,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            List<Asignacion> asignaciones,
            List<Reserva> reservas,
            String idAsignacionExcluir) {

        if (solicitud == null || espacio == null
                || dias == null || dias.isEmpty()
                || horaInicio == null || horaFin == null
                || !horaFin.isAfter(horaInicio)) {
            return false;
        }

        List<String> idsDisponibles = consultarEspacios(
                solicitud,
                List.of(espacio),
                asignaciones,
                reservas,
                dias,
                horaInicio,
                horaFin,
                idAsignacionExcluir
        );

        return idsDisponibles.contains(espacio.getId());
    }

    /**
     * Consulta utilizando los días y horarios originales de la solicitud.
     */
    private List<String> consultarEspacios(
            Solicitud solicitud,
            List<Espacio> espacios,
            List<Asignacion> asignaciones,
            List<Reserva> reservas) {

        return consultarEspacios(
                solicitud,
                espacios,
                asignaciones,
                reservas,
                solicitud.getDias(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin(),
                null
        );
    }

    /**
     * Consulta Prolog con días, horarios y una asignación opcionalmente excluida.
     */
    private List<String> consultarEspacios(
            Solicitud solicitud,
            List<Espacio> espacios,
            List<Asignacion> asignaciones,
            List<Reserva> reservas,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            String idAsignacionExcluir) {

        if (!Files.isRegularFile(archivoReglas)) {
            throw new IllegalStateException(
                    "No se encontró el archivo de reglas Prolog: " + archivoReglas);
        }

        Path scriptTemporal = null;

        try {
            scriptTemporal = Files.createTempFile(
                    "consulta-organizador-", ".pl");

            String contenidoScript = construirScript(
                    solicitud,
                    espacios,
                    asignaciones,
                    reservas,
                    dias,
                    horaInicio,
                    horaFin,
                    idAsignacionExcluir
            );

            Files.writeString(
                    scriptTemporal,
                    contenidoScript,
                    StandardCharsets.UTF_8
            );

            System.out.println("===== SCRIPT PROLOG GENERADO =====");
            System.out.println(contenidoScript);
            System.out.println("===== FIN SCRIPT PROLOG =====");

            Process proceso = new ProcessBuilder(
                    ejecutable,
                    "-q",
                    "-s", scriptTemporal.toAbsolutePath().toString(),
                    "-g", "main",
                    "-t", "halt")
                    .redirectErrorStream(true)
                    .start();

            if (!proceso.waitFor(15, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();

                throw new IllegalStateException(
                        "La consulta a SWI-Prolog superó el tiempo máximo de espera.");
            }

            String salida = new String(
                    proceso.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (proceso.exitValue() != 0
                    || !salida.contains("__PROLOG_INICIO__")
                    || !salida.contains("__PROLOG_FIN__")) {

                throw new IllegalStateException(
                        "No se pudo ejecutar la consulta Prolog. Verifique SWI-Prolog "
                                + "y organizador.pl. Detalle: " + salida.trim());
            }

            List<String> ids = new ArrayList<>();
            boolean leyendoResultados = false;

            for (String linea : salida.split("\\R")) {
                String valor = linea.trim();

                if ("__PROLOG_INICIO__".equals(valor)) {
                    leyendoResultados = true;
                } else if ("__PROLOG_FIN__".equals(valor)) {
                    break;
                } else if (leyendoResultados && !valor.isEmpty()) {
                    ids.add(valor);
                }
            }

            return ids;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo iniciar SWI-Prolog. Compruebe la propiedad "
                            + "prolog.ejecutable. Ruta configurada: " + ejecutable,
                    e
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Se interrumpió la comunicación con SWI-Prolog.", e);

        } finally {
            if (scriptTemporal != null) {
                try {
                    Files.deleteIfExists(scriptTemporal);
                } catch (IOException ignored) {
                    // El archivo temporal se elimina al finalizar la consulta.
                }
            }
        }
    }

    /**
     * Construye una consulta autocontenida con una fotografía de los datos de H2.
     */
    private String construirScript(
            Solicitud solicitud,
            List<Espacio> espacios,
            List<Asignacion> asignaciones,
            List<Reserva> reservas,
            List<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            String idAsignacionExcluir) {

        StringBuilder script = new StringBuilder();
        String rutaProlog = archivoReglas.toString().replace("\\", "/");

        script.append(":- consult(")
                .append(atomo(rutaProlog))
                .append(").\n");

        script.append("main :-\n");
        script.append("    retractall(espacio(_, _, _, _)),\n");
        script.append("    retractall(ocupacion(_, _, _, _)),\n");

        // Registrar los espacios.
        for (Espacio espacio : espacios) {
            script.append("    assertz(espacio(")
                    .append(atomo(espacio.getId())).append(", ")
                    .append(atomo(normalizar(espacio.getTipoEspacio()))).append(", ")
                    .append(espacio.getCapacidadMaxima()).append(", ")
                    .append(atomo(normalizar(espacio.getEstado())))
                    .append(")),\n");
        }

        // Registrar las asignaciones activas, excepto la que se está editando.
        if (asignaciones != null) {
            for (Asignacion asignacion : asignaciones) {

                if (asignacion == null
                        || !asignacion.estaActiva()
                        || (idAsignacionExcluir != null
                        && idAsignacionExcluir.equals(asignacion.getId()))) {
                    continue;
                }

                for (DayOfWeek dia : asignacion.getDiasAsignados()) {
                    agregarOcupacion(
                            script,
                            asignacion.getEspacio().getId(),
                            dia,
                            asignacion.getHoraInicioAsignada(),
                            asignacion.getHoraFinAsignada()
                    );
                }
            }
        }

        // Registrar las reservas directas que todavía ocupan un horario.
        if (reservas != null) {

            LocalDate hoy = LocalDate.now();
            LocalTime ahora = LocalTime.now();

            LocalDate finCursada = configuracionService
                    .obtenerConfiguracion()
                    .getFechaFinCursada();

            for (Reserva reserva : reservas) {

                if (reserva == null || reserva.getFecha() == null) {
                    continue;
                }

                LocalDate fechaReserva = reserva.getFecha();

                // Ignora reservas anteriores al día actual.
                if (fechaReserva.isBefore(hoy)) {
                    continue;
                }

                // Ignora reservas posteriores a la cursada.
                if (fechaReserva.isAfter(finCursada)) {
                    continue;
                }

                // Si la reserva es hoy, solo considera las que todavía no terminaron.
                if (fechaReserva.equals(hoy)
                        && !reserva.getHoraFin().isAfter(ahora)) {
                    continue;
                }

                agregarOcupacion(
                        script,
                        reserva.getEspacio().getId(),
                        fechaReserva.getDayOfWeek(),
                        reserva.getHoraInicio(),
                        reserva.getHoraFin()
                );
            }
        }

        String diasProlog = dias.stream()
                .map(dia -> atomo(diaProlog(dia)))
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));

        script.append("    findall(ID, espacio_disponible_dias(ID, ")
                .append(atomo(normalizar(
                        solicitud.getMateria().getTipoEspacioFrecuente())))
                .append(", ")
                .append(solicitud.getCantidadAlumnos())
                .append(", ")
                .append(diasProlog)
                .append(", ")
                .append(minutos(horaInicio))
                .append(", ")
                .append(minutos(horaFin))
                .append("), IDs),\n")
                .append("    writeln('__PROLOG_INICIO__'),\n")
                .append("    forall(member(ID, IDs), writeln(ID)),\n")
                .append("    writeln('__PROLOG_FIN__').\n");

        return script.toString();
    }

    private void agregarOcupacion(
            StringBuilder script,
            String idEspacio,
            DayOfWeek dia,
            LocalTime inicio,
            LocalTime fin) {

        script.append("    assertz(ocupacion(")
                .append(atomo(idEspacio)).append(", ")
                .append(atomo(diaProlog(dia))).append(", ")
                .append(minutos(inicio)).append(", ")
                .append(minutos(fin)).append(")),\n");
    }

    private int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    private String normalizar(String valor) {
        return valor == null
                ? ""
                : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String diaProlog(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
    }

    /**
     * Escapa texto para representarlo como átomo Prolog entre comillas simples.
     */
    private String atomo(String valor) {
        String seguro = valor == null ? "" : valor;

        seguro = seguro.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        return "'" + seguro + "'";
    }
}
