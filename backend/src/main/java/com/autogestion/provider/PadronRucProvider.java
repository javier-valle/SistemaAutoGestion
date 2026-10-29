package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.repository.PadronRucRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** RUC desde el Padrón Reducido de SUNAT cargado en la tabla local padron_ruc. */
@Component
@Order(10)
@RequiredArgsConstructor
public class PadronRucProvider implements ProveedorDocumento {

    private final PadronRucRepository repository;

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.RUC;
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        return repository.findById(numero).map(p -> {
            ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
            dto.setTipoDocumento("RUC");
            dto.setDocumento(p.getRuc());
            dto.setNombre(p.getRazonSocial());
            dto.setRazonSocial(p.getRazonSocial());
            dto.setDireccion(p.getDireccion());
            dto.setEstado(p.getEstado());
            dto.setCondicion(p.getCondicion());
            dto.setFuente("SUNAT Padrón Reducido RUC");
            return dto;
        });
    }
}
