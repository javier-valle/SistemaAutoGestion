package com.autogestion.repository;

import com.autogestion.entity.PortalAccesoLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalAccesoLogRepository extends JpaRepository<PortalAccesoLog, Long> {

    void deleteByClienteId(Long clienteId);
}
