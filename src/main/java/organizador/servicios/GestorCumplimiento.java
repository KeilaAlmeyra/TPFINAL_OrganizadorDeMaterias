package organizador.servicios;

import organizador.modelos.Asignacion;
import organizador.modelos.RegistroCumplimiento;
import organizador.repositorios.RegistroCumplimientoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GestorCumplimiento {

    private final RegistroCumplimientoRepository repositorio;

    public GestorCumplimiento(
            RegistroCumplimientoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public RegistroCumplimiento registrarResultado(
            String id,
            Asignacion asignacion,
            LocalDate fecha,
            String resultado) {

        RegistroCumplimiento registro =
                new RegistroCumplimiento(id, asignacion, fecha, resultado);

        return repositorio.save(registro);
    }

    public List<RegistroCumplimiento> obtenerRegistros() {
        return repositorio.findAll();
    }

    public List<RegistroCumplimiento> obtenerRegistrosDeProfesor(
            String idProfesor) {

        return repositorio.findAll().stream()
                .filter(registro -> registro.getAsignacion()
                        .getSolicitud()
                        .getProfesor()
                        .getId()
                        .equals(idProfesor))
                .toList();
    }
}