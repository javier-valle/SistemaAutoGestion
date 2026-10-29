package com.autogestion.repository;

import com.autogestion.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    void deleteByClienteId(Long clienteId);
}
