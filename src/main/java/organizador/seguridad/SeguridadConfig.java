
package organizador.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(autorizacion -> autorizacion

                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/imagenes/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Acciones exclusivas del administrador.
                        .requestMatchers(
                                "/prueba-asignaciones/procesar",
                                "/prueba-asignaciones/reintentar",
                                "/solicitudes-pendientes",
                                "/reservas/*/aprobar",
                                "/reservas/*/rechazar",
                                "/propuestas/crear"
                        ).hasRole("ADMINISTRADOR")

                        // Ambos roles pueden consultar y presentar solicitudes.
                        .requestMatchers(
                                "/prueba-asignaciones",
                                "/prueba-asignaciones/solicitar"
                        ).hasAnyRole("ADMINISTRADOR", "PROFESOR")

                        // Administración general.
                        .requestMatchers(
                                "/usuarios/**",
                                "/espacios/**",
                                "/configuracion/**",
                                "/materias/**"
                        ).hasRole("ADMINISTRADOR")

                        .anyRequest().authenticated()
                )
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(cierre -> cierre
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
