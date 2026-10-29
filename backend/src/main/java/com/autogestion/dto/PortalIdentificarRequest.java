package com.autogestion.dto;

import lombok.Data;

@Data
public class PortalIdentificarRequest {
    private String documento;
    private String tipoDocumento;
    private String placa;
}
