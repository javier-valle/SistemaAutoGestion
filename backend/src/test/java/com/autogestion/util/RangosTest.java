package com.autogestion.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

/** El rango de reportes tiene un solo lugar y conserva las convenciones del sistema. */
class RangosTest {

    @Test
    void sinFechasDevuelveElMesActualCompleto() {
        YearMonth mes = YearMonth.now(AppTime.ZONA);

        LocalDate[] r = Rangos.rangoReporte(null, null);

        assertEquals(mes.atDay(1), r[0]);
        assertEquals(mes.plusMonths(1).atDay(1), r[1]);
    }

    @Test
    void hastaIncluyeElDiaCompleto() {
        LocalDate[] r = Rangos.rangoReporte(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        assertEquals(LocalDate.of(2026, 10, 1), r[0]);
        assertEquals(LocalDate.of(2026, 10, 16), r[1]);
    }

    @Test
    void desdeSinHastaCierraAlInicioDelMesSiguiente() {
        LocalDate[] r = Rangos.rangoReporte(LocalDate.of(2026, 9, 20), null);
        YearMonth siguiente = YearMonth.now(AppTime.ZONA).plusMonths(1);

        assertEquals(LocalDate.of(2026, 9, 20), r[0]);
        assertEquals(siguiente.atDay(1), r[1]);
    }

    @Test
    void soloHastaArrancaEnElPrimerDiaDelMesAnterior() {
        LocalDate[] r = Rangos.rangoReporte(null, LocalDate.of(2026, 10, 15));

        assertEquals(LocalDate.of(2026, 9, 1), r[0]);
        assertEquals(LocalDate.of(2026, 10, 16), r[1]);
    }
}
