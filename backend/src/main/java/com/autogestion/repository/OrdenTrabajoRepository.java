package com.autogestion.repository;

import com.autogestion.entity.OrdenTrabajo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    @Query("SELECT ot FROM OrdenTrabajo ot LEFT JOIN FETCH ot.cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente LEFT JOIN FETCH ot.mecanico WHERE ot.estado = :estado")
    List<OrdenTrabajo> findByEstado(com.autogestion.entity.EstadoOT estado);

    @Query("SELECT ot FROM OrdenTrabajo ot LEFT JOIN FETCH ot.cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente LEFT JOIN FETCH ot.mecanico")
    List<OrdenTrabajo> findAll();

    @Query("SELECT ot FROM OrdenTrabajo ot LEFT JOIN FETCH ot.cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente LEFT JOIN FETCH ot.mecanico WHERE ot.mecanico.id = :mecanicoId")
    List<OrdenTrabajo> findByMecanicoId(Long mecanicoId);

    @Query("SELECT ot FROM OrdenTrabajo ot LEFT JOIN FETCH ot.cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente LEFT JOIN FETCH ot.mecanico WHERE ot.id = :id")
    Optional<OrdenTrabajo> findById(Long id);

    @Query("SELECT COUNT(ot) FROM OrdenTrabajo ot WHERE ot.estado = :estado AND ot.fechaFin >= :desde AND ot.fechaFin < :hasta")
    long countFinalizadasEntre(com.autogestion.entity.EstadoOT estado, java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    @Query("SELECT ot.mecanico.id, ot.mecanico.nombreCompleto, COUNT(ot) FROM OrdenTrabajo ot WHERE ot.fechaCreacion >= :desde AND ot.fechaCreacion < :hasta GROUP BY ot.mecanico.id, ot.mecanico.nombreCompleto")
    List<Object[]> asignadasPorMecanico(java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    @Query("SELECT ot.mecanico.id, ot.mecanico.nombreCompleto, COUNT(ot) FROM OrdenTrabajo ot WHERE ot.estado NOT IN :abiertos GROUP BY ot.mecanico.id, ot.mecanico.nombreCompleto")
    List<Object[]> enProcesoPorMecanico(List<com.autogestion.entity.EstadoOT> abiertos);

    @Query("""
        SELECT ot FROM OrdenTrabajo ot LEFT JOIN FETCH ot.mecanico
        WHERE ot.estado = :estado AND ot.fechaFin >= :desde AND ot.fechaFin < :hasta
        """)
    List<com.autogestion.entity.OrdenTrabajo> finalizadasEntre(com.autogestion.entity.EstadoOT estado, java.time.LocalDateTime desde, java.time.LocalDateTime hasta);

    long countByMecanicoIdAndEstadoNotIn(Long mecanicoId, List<com.autogestion.entity.EstadoOT> estados);

    @Query("SELECT COUNT(ot) FROM OrdenTrabajo ot WHERE ot.mecanico.id = :mecanicoId AND ot.estado = 'FINALIZADA' AND ot.fechaFin >= :desde")
    long countFinalizadasDesde(Long mecanicoId, java.time.LocalDateTime desde);

    /** Fila bloqueada: quien emite/verifica una OT espera su turno (anti doble cobro). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ot FROM OrdenTrabajo ot WHERE ot.id = :id")
    Optional<OrdenTrabajo> bloquearPorId(Long id);
}