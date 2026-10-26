package com.autogestion.repository;

import com.autogestion.entity.OtpCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpCodigoRepository extends JpaRepository<OtpCodigo, Long> {
    Optional<OtpCodigo> findByDocumentoAndTipoDocumentoAndUsadoFalseAndExpiraEnAfter(String documento, String tipoDocumento, java.time.LocalDateTime now);
}
