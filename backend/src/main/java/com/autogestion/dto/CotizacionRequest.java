package com.autogestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CotizacionRequest {
    @NotNull private Long diagnosticoId;
    private List<ServicioCotizacion> servicios;
    private List<ProductoCotizacion> productos;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ServicioCotizacion {
        @NotNull private Long servicioId;
        private BigDecimal precio;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProductoCotizacion {
        @NotNull private Long productoId;
        private Integer cantidadEstimada;
        private BigDecimal precioUnitario;
    }
}
