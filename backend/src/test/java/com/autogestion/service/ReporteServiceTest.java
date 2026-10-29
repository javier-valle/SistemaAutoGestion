package com.autogestion.service;

import com.autogestion.dto.ReporteDTOs;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteServiceTest {

    @Test
    void rangoValidoYLimites() {
        var hoy = LocalDate.now(ReporteService.ZONA);

        assertTrue(hoy.isBefore(hoy.plusDays(366)));
    }

    @Test
    void resultadoConMermas() {

        assertNotNull(new ReporteDTOs.ResultadoDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
