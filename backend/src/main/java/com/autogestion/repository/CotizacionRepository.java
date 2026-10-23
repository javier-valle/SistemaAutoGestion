package com.autogestion.repository;

import com.autogestion.entity.Cotizacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @Query("SELECT c FROM Cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.mecanico LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente WHERE c.estado = :estado")
    List<Cotizacion> findByEstado(com.autogestion.entity.EstadoCotizacion estado);

    @Query("SELECT c FROM Cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.mecanico LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente")
    List<Cotizacion> findAll();

    @Query("SELECT c FROM Cotizacion c LEFT JOIN FETCH c.diagnostico d LEFT JOIN FETCH d.mecanico LEFT JOIN FETCH d.recepcion r LEFT JOIN FETCH r.vehiculo v LEFT JOIN FETCH v.cliente WHERE c.id = :id")
    Optional<Cotizacion> findById(Long id);

    /** Fila bloqueada: quien convierte una cotización en OT espera su turno (anti doble OT). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cotizacion c WHERE c.id = :id")
    Optional<Cotizacion> bloquearPorId(Long id);
}