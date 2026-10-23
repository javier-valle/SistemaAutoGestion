package com.autogestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrdenTrabajoCompletaRequest {

    @NotNull
    private Long cotizacionId;

    @NotNull
    private Long mecanicoId;

    private List<ProductoUsadoItem> productosUsados;

    private PagoEntregaItem pagoEntrega;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProductoUsadoItem {
        @NotNull private Long productoId;
        @NotNull private Integer cantidadUsada;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PagoEntregaItem {
        private BigDecimal monto;
        private Boolean registrarEntrega;
    }
}