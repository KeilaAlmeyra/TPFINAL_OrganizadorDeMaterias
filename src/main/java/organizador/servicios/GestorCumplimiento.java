package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.RegistroCumplimiento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GestorCumplimiento {

    private final List<RegistroCumplimiento> registros;

    public GestorCumplimiento() {
        this.registros = new ArrayList<>();
    }

    public RegistroCumplimiento registrarResultado(
            String id,
            Asignacion asignacion,
            LocalDate fecha,
            String resultado) {

        RegistroCumplimiento registro = new RegistroCumplimiento(
                id,
                asignacion,
                fecha,
                resultado
        );

        registros.add(registro);

        return registro;
    }

    public List<RegistroCumplimiento> obtenerRegistros() {
        return List.copyOf(registros);
    }

    public List<RegistroCumplimiento> obtenerRegistrosDeProfesor(
            String idProfesor) {

        return registros.stream()
                .filter(registro ->
                        registro.getAsignacion()
                                .getSolicitud()
                                .getProfesor()
                                .getId()
                                .equals(idProfesor))
                .toList();
    }
}
