package com.autogestion.repository;

import com.autogestion.entity.Comprobante;
import com.autogestion.entity.EstadoComprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /** Comprobante vigente de una OT (un anulado no bloquea re-emitir). */
    Optional<Comprobante> findByOrdenTrabajoIdAndEstado(Long ordenTrabajoId, EstadoComprobante estado);

    List<Comprobante> findByClienteIdOrderByFechaEmisionDesc(Long clienteId);

    @Query("""
        SELECT c.tipo, COUNT(c), COALESCE(SUM(c.totalGravado), 0), COALESCE(SUM(c.igv), 0), COALESCE(SUM(c.total), 0)
        FROM Comprobante c WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        GROUP BY c.tipo
        """)
    List<Object[]> resumenPorTipo(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT COALESCE(SUM(c.total), 0) FROM Comprobante c
        WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        """)
    java.math.BigDecimal ingresosEntre(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT COALESCE(SUM(c.totalGravado), 0) FROM Comprobante c
        WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        """)
    java.math.BigDecimal ingresosSinIgvEntre(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT CAST(c.fechaEmision AS date), COALESCE(SUM(c.total), 0) FROM Comprobante c
        WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        GROUP BY CAST(c.fechaEmision AS date) ORDER BY CAST(c.fechaEmision AS date)
        """)
    List<Object[]> ingresosPorDia(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT YEAR(c.fechaEmision), MONTH(c.fechaEmision), COALESCE(SUM(c.total), 0) FROM Comprobante c
        WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        GROUP BY YEAR(c.fechaEmision), MONTH(c.fechaEmision)
        ORDER BY YEAR(c.fechaEmision), MONTH(c.fechaEmision)
        """)
    List<Object[]> ingresosPorMes(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT m.id, m.nombre, COALESCE(SUM(c.total), 0) FROM Comprobante c
        JOIN c.ordenTrabajo ot JOIN ot.mecanico m
        WHERE c.estado = :estado AND c.fechaEmision >= :desde AND c.fechaEmision < :hasta
        GROUP BY m.id, m.nombre
        """)
    List<Object[]> ingresosPorMecanico(EstadoComprobante estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT c FROM Comprobante c
        LEFT JOIN FETCH c.detalle
        WHERE c.id = :id
        """)
    Optional<Comprobante> buscarCompleto(Long id);

    @Query(value = """
        SELECT c.* FROM comprobante c
        WHERE (:tipo IS NULL OR c.tipo = :tipo)
          AND (:estado IS NULL OR c.estado = :estado)
          AND c.fecha_emision >= :desde AND c.fecha_emision < :hasta
          AND (:q IS NULL OR LOWER(CAST(c.cliente_nombre AS text)) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR c.cliente_num_doc LIKE CONCAT('%', CAST(:q AS text), '%')
               OR CONCAT(c.serie, '-', c.numero) LIKE CONCAT('%', CAST(:q AS text), '%'))
        """,
        countQuery = """
        SELECT COUNT(*) FROM comprobante c
        WHERE (:tipo IS NULL OR c.tipo = :tipo)
          AND (:estado IS NULL OR c.estado = :estado)
          AND c.fecha_emision >= :desde AND c.fecha_emision < :hasta
          AND (:q IS NULL OR LOWER(CAST(c.cliente_nombre AS text)) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR c.cliente_num_doc LIKE CONCAT('%', CAST(:q AS text), '%')
               OR CONCAT(c.serie, '-', c.numero) LIKE CONCAT('%', CAST(:q AS text), '%'))
        """,
        nativeQuery = true)
    Page<Comprobante> filtrar(String tipo, String estado, LocalDateTime desde, LocalDateTime hasta, String q, Pageable pageable);
}
