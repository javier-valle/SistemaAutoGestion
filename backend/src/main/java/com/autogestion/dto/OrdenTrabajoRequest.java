package com.autogestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrdenTrabajoRequest {
    @NotNull(message = "Falta la cotización")
    private Long cotizacionId;
    @NotNull(message = "Falta el mecánico")
    private Long mecanicoId;
}
