package organizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del sistema Organizador de Materias.
 *
 * Se encarga de iniciar la aplicación Spring Boot
 * y levantar el servidor web para acceder al sistema.
 */
@SpringBootApplication
public class Aplicacion {

    public static void main(String[] args) {

        // Inicia la aplicación y el servidor web.
        SpringApplication.run(Aplicacion.class, args);

    }
}

