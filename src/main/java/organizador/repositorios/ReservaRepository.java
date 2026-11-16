package organizador.repositorios;

import organizador.modelos.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Acceso a la tabla RESERVA.
 *
 * JpaRepository<Reserva, Long>: trabaja con Reserva y su @Id es de tipo Long.
 * Trae los métodos básicos (save, existsById, deleteById...) y además
 * declaramos tres consultas propias.
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Todas las reservas, ordenadas por fecha y hora.
    List<Reserva> findAllByOrderByFechaAscHoraInicioAsc();

    // Reservas de una persona (por parte del nombre, sin importar mayúsculas).
    List<Reserva> findByReservadoPorContainingIgnoreCaseOrderByFechaAscHoraInicioAsc(String nombre);

    // Cuenta las reservas del mismo espacio y fecha que se pisan con el horario pedido.
    @Query("""
            select count(r) from Reserva r
            where r.espacio.id = :espacioId
              and r.fecha = :fecha
              and r.horaInicio < :fin
              and r.horaFin > :inicio
            """)
    long contarSuperposiciones(@Param("espacioId") String espacioId,
                               @Param("fecha") LocalDate fecha,
                               @Param("inicio") LocalTime inicio,
                               @Param("fin") LocalTime fin);
}