package com.autogestion.dto;

import lombok.Data;

@Data
public class PortalOtpVerificarRequest {
    private String documento;
    private String tipoDocumento;
    private String codigo;
}
