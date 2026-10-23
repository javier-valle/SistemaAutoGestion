package com.autogestion.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class PagoEntregaCompletaRequest {

    private Long ordenTrabajoId;
    private BigDecimal monto;
    private Boolean registrarEntrega;
}