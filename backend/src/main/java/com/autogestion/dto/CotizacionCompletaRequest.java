package com.autogestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CotizacionCompletaRequest {

    @NotNull
    private Long diagnosticoId;

    private List<ServicioItem> servicios;

    private List<ProductoItem> productos;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ServicioItem {
        @NotNull private Long servicioId;
        @NotNull private BigDecimal precio;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProductoItem {
        @NotNull private Long productoId;
        @NotNull private Integer cantidadEstimada;
        @NotNull private BigDecimal precioUnitario;
    }
}