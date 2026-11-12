package organizador.servicios;

import organizador.modelos.Solicitud;

public class GestorSolicitudes {

    /**
     * Verifica que una solicitud cumpla
     * con los requisitos básicos del sistema.
     *
     * @return null si es válida o un mensaje si hay un error.
     */
    public String validarSolicitud(Solicitud solicitud) {

        // 1. Verificar que exista la solicitud.
        if (solicitud == null) {
            return "La solicitud no puede estar vacía.";
        }

        // 2. Verificar que tenga un profesor.
        if (solicitud.getProfesor() == null
                || !solicitud.getProfesor().esProfesor()) {
            return "La solicitud debe pertenecer a un profesor.";
        }

        // 3. Verificar que tenga una materia.
        if (solicitud.getMateria() == null) {
            return "La solicitud debe tener una materia.";
        }

        // 4. Verificar la cantidad de alumnos.
        if (solicitud.getCantidadAlumnos() <= 0) {
            return "La cantidad de alumnos debe ser mayor a cero.";
        }

        // 5. Verificar que tenga días seleccionados.
        if (solicitud.getDias() == null
                || solicitud.getDias().isEmpty()) {
            return "Debe seleccionar al menos un día.";
        }

        // 6. Verificar que el horario sea válido.
        if (!solicitud.horarioValido()) {
            return "El horario de la solicitud no es válido.";
        }

        return null;
    }
}

