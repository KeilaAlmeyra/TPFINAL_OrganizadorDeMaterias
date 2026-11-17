
package organizador.repositorios;

import organizador.modelos.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findAllByOrderByFechaAscHoraInicioAsc();

    List<Reserva> findByReservadoPorContainingIgnoreCaseOrderByFechaAscHoraInicioAsc(
            String nombre);

    // Reservas pertenecientes a una cuenta.
    List<Reserva> findByPropietarioCorreoIgnoreCaseOrderByFechaAscHoraInicioAsc(
            String correo);

    // Solicitudes que esperan revisión del administrador.
    List<Reserva> findByEstadoOrderByFechaAscHoraInicioAsc(String estado);

    // Busca una reserva únicamente si pertenece a ese usuario.
    Optional<Reserva> findByIdAndPropietarioCorreoIgnoreCase(
            Long id, String correo);



    @Query("""
        select count(r) from Reserva r
        where r.espacio.id = :espacioId
          and r.fecha = :fecha
          and (r.estado = 'CONFIRMADA' or r.estado is null)
          and r.horaInicio < :fin
          and r.horaFin > :inicio
        """)
    long contarSuperposiciones(@Param("espacioId") String espacioId,
                               @Param("fecha") LocalDate fecha,
                               @Param("inicio") LocalTime inicio,
                               @Param("fin") LocalTime fin);

    boolean existsByEspacio_Id(String espacioId);
}
