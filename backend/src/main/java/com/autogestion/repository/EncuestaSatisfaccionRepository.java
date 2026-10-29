package com.autogestion.repository;

import com.autogestion.entity.EncuestaSatisfaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncuestaSatisfaccionRepository extends JpaRepository<EncuestaSatisfaccion, Long> {

    /** Una encuesta por OT y cliente: el portal no deja calificar dos veces. */
    boolean existsByOrdenTrabajoIdAndClienteId(Long ordenTrabajoId, Long clienteId);

    List<EncuestaSatisfaccion> findByClienteIdOrderByCreadoEnDesc(Long clienteId);
}
