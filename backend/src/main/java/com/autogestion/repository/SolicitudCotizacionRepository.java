package com.autogestion.repository;

import com.autogestion.entity.EstadoSolicitudCotizacion;
import com.autogestion.entity.SolicitudCotizacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudCotizacionRepository extends JpaRepository<SolicitudCotizacion, Long> {

    @EntityGraph(attributePaths = {"vehiculo", "cliente"})
    List<SolicitudCotizacion> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    @EntityGraph(attributePaths = {"vehiculo", "cliente"})
    List<SolicitudCotizacion> findByEstadoOrderByCreadoEnDesc(EstadoSolicitudCotizacion estado);

    @EntityGraph(attributePaths = {"vehiculo", "cliente"})
    List<SolicitudCotizacion> findAllByOrderByCreadoEnDesc();

    @EntityGraph(attributePaths = {"vehiculo", "cliente"})
    Optional<SolicitudCotizacion> findByIdAndClienteId(Long id, Long clienteId);

    boolean existsByVehiculoIdAndEstado(Long vehiculoId, EstadoSolicitudCotizacion estado);

    long countByClienteIdAndEstado(Long clienteId, EstadoSolicitudCotizacion estado);
}
