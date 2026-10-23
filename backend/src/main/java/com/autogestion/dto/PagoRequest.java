package com.autogestion.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class PagoRequest {
    private Long ordenTrabajoId;
    private BigDecimal monto;
}
