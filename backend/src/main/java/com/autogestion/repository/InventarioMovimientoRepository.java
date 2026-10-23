package com.autogestion.repository;

import com.autogestion.entity.InventarioMovimiento;
import com.autogestion.entity.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InventarioMovimientoRepository extends JpaRepository<InventarioMovimiento, Long> {
    List<InventarioMovimiento> findByProductoId(Long productoId);
    List<InventarioMovimiento> findByTipo(TipoMovimiento tipo);

    @Query("""
        SELECT m FROM InventarioMovimiento m
        WHERE (:productoId IS NULL OR m.producto.id = :productoId)
          AND (:tipo IS NULL OR m.tipo = :tipo)
          AND (:usuarioId IS NULL OR m.usuario.id = :usuarioId)
          AND m.fecha >= :desde AND m.fecha < :hasta
        """)
    Page<InventarioMovimiento> filtrar(Long productoId, TipoMovimiento tipo, Long usuarioId,
                                       LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    /** Para el kardex: movimientos de un producto en rango, en orden. */
    @Query("""
        SELECT m FROM InventarioMovimiento m
        LEFT JOIN FETCH m.proveedor LEFT JOIN FETCH m.usuario
        WHERE m.producto.id = :productoId AND m.fecha >= :desde AND m.fecha < :hasta
        ORDER BY m.fecha ASC, m.id ASC
        """)
    List<InventarioMovimiento> kardex(Long productoId, LocalDateTime desde, LocalDateTime hasta);

    /** Dinero de un tipo de movimiento en el rango (compras, consumos, mermas). */
    @Query("""
        SELECT COALESCE(SUM(m.cantidad * m.costoUnitario), 0) FROM InventarioMovimiento m
        WHERE m.tipo = :tipo AND m.fecha >= :desde AND m.fecha < :hasta
        """)
    java.math.BigDecimal sumaCostoPorTipo(TipoMovimiento tipo, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT p.nombre, COALESCE(SUM(m.cantidad), 0), COALESCE(SUM(m.cantidad * m.costoUnitario), 0)
        FROM InventarioMovimiento m JOIN m.producto p
        WHERE m.tipo = :tipo AND m.fecha >= :desde AND m.fecha < :hasta
        GROUP BY p.nombre ORDER BY SUM(m.cantidad) DESC
        """)
    List<Object[]> consumoPorProducto(TipoMovimiento tipo, LocalDateTime desde, LocalDateTime hasta, org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT CONCAT('OT #', ot.id), COALESCE(SUM(m.cantidad), 0), COALESCE(SUM(m.cantidad * m.costoUnitario), 0)
        FROM InventarioMovimiento m JOIN m.ordenTrabajo ot
        WHERE m.tipo = :tipo AND m.fecha >= :desde AND m.fecha < :hasta
        GROUP BY ot.id ORDER BY SUM(m.cantidad * m.costoUnitario) DESC
        """)
    List<Object[]> consumoPorOT(TipoMovimiento tipo, LocalDateTime desde, LocalDateTime hasta, org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT u.id, u.nombre, COALESCE(SUM(m.cantidad), 0), COALESCE(SUM(m.cantidad * m.costoUnitario), 0)
        FROM InventarioMovimiento m JOIN m.ordenTrabajo ot JOIN ot.mecanico u
        WHERE m.tipo = :tipo AND m.fecha >= :desde AND m.fecha < :hasta
        GROUP BY u.id, u.nombre ORDER BY SUM(m.cantidad * m.costoUnitario) DESC
        """)
    List<Object[]> consumoPorMecanico(TipoMovimiento tipo, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        SELECT m FROM InventarioMovimiento m LEFT JOIN FETCH m.producto LEFT JOIN FETCH m.proveedor
        WHERE m.tipo = 'ENTRADA' AND m.fecha >= :desde AND m.fecha < :hasta
          AND (:proveedorId IS NULL OR m.proveedor.id = :proveedorId)
        """)
    org.springframework.data.domain.Page<InventarioMovimiento> entradas(LocalDateTime desde, LocalDateTime hasta,
                                                                        Long proveedorId,
                                                                        org.springframework.data.domain.Pageable pageable);
}
