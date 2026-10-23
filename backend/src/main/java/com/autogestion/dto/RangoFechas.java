package com.autogestion.dto;

import java.time.LocalDate;

public record RangoFechas(LocalDate desde, LocalDate hasta) {
    public RangoFechas {
        if (desde == null || hasta == null) throw new IllegalArgumentException("Rango requiere desde y hasta");
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("Desde debe ser anterior o igual a hasta");
        if (hasta.minusDays(365).isAfter(desde)) throw new IllegalArgumentException("Rango máximo 366 días");
    }
    public LocalDate hastaExclusivo() { return hasta.plusDays(1); }
}
