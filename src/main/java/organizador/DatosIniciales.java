package organizador;

import organizador.modelos.Espacio;
import organizador.servicios.GestorEspacios;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatosIniciales implements CommandLineRunner {

    private final GestorEspacios gestorEspacios;

    public DatosIniciales(GestorEspacios gestorEspacios) {
        this.gestorEspacios = gestorEspacios;
    }

    @Override
    public void run(String... args) {

        if (!gestorEspacios.obtenerEspacios().isEmpty()) {
            return;
        }

        gestorEspacios.registrarEspacio(
                new Espacio("AULA-1", "Aula 1", 40, "aula", "disponible"));
        gestorEspacios.registrarEspacio(
                new Espacio("COMP-1", "Aula de computación 1", 30, "computadoras", "disponible"));
        gestorEspacios.registrarEspacio(
                new Espacio("LAB-1", "Laboratorio 1", 25, "laboratorio",  "disponible"));
    }
}