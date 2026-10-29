package com.autogestion.repository;

import com.autogestion.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Fila bloqueada para mover stock: dos consumos simultáneos se serializan
     * y ninguno pisa el stock que leyó el otro (evita descuadres y stock negativo).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> bloquearPorId(@Param("id") Long id);

    @Query("SELECT p FROM Producto p WHERE p.stockActual < p.stockMinimo")
    List<Producto> findConStockBajo();

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.stockActual < p.stockMinimo")
    long countConStockBajo();

    /** Dinero quieto en el estante: stock × último costo. */
    @Query("SELECT COALESCE(SUM(p.stockActual * p.costoUnitario), 0) FROM Producto p")
    java.math.BigDecimal valorInventario();

    @Query("""
        SELECT p.nombre, p.precioUnitario, p.costoUnitario
        FROM Producto p WHERE p.activo = true ORDER BY p.nombre
        """)
    List<Object[]> preciosVsCostos();
}
