package com.autogestion.repository;

import com.autogestion.entity.Recepcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecepcionRepository extends JpaRepository<Recepcion, Long> {

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente"})
    List<Recepcion> findByEstado(com.autogestion.entity.EstadoRecepcion estado);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente"})
    List<Recepcion> findAll();

    @Query("""
        SELECT r FROM Recepcion r
        LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente
        WHERE v.cliente.id = :clienteId ORDER BY r.fechaIngreso DESC
        """)
    List<Recepcion> porCliente(Long clienteId);

    /** Base de "clientes atendidos por día". CAST a fecha funciona en H2 y Postgres. */
    @Query("""
        SELECT CAST(r.fechaIngreso AS date), COUNT(DISTINCT r.vehiculo.cliente.id), COUNT(r)
        FROM Recepcion r JOIN r.vehiculo v
        WHERE r.fechaIngreso >= :desde AND r.fechaIngreso < :hasta
        GROUP BY CAST(r.fechaIngreso AS date) ORDER BY CAST(r.fechaIngreso AS date)
        """)
    List<Object[]> clientesPorDia(LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT r.estado, COUNT(r) FROM Recepcion r GROUP BY r.estado")
    List<Object[]> porEstado();

    long countByVehiculoId(Long vehiculoId);
}
