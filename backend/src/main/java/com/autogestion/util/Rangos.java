package com.autogestion.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Rango por defecto de los listados, en un solo lugar.
 *
 * <p>Convención: el extremo superior es <b>exclusivo</b> (&lt; hasta) salvo
 * {@link #finDiaInclusivo}, que es para las queries sobre {@code LocalDate}
 * que comparan con &lt;=. Así "hasta = hoy" siempre incluye el día completo,
 * y todos los controladores usan la misma zona (America/Lima vía {@link AppTime})
 * en vez de la zona del servidor.</p>
 */
public final class Rangos {

    private Rangos() { }

    /** Desde tan antiguo como exista el negocio: "sin filtro desde". */
    private static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);

    /** Inicio en horas de un rango recibido como LocalDate (o 2000-01-01). */
    public static LocalDateTime inicio(LocalDate desde) {
        return (desde != null ? desde : EPOCH).atStartOfDay();
    }

    /** Inicio en horas de un rango que ya llega con hora. */
    public static LocalDateTime inicio(LocalDateTime desde) {
        return desde != null ? desde : EPOCH.atStartOfDay();
    }

    /** Inicio en días de un rango recibido como LocalDate (o 2000-01-01). */
    public static LocalDate inicioDia(LocalDate desde) {
        return desde != null ? desde : EPOCH;
    }

    /** Fin exclusivo (00:00 del día siguiente) para comparar con "&lt;"; si no llega hasta, mañana. */
    public static LocalDateTime finDia(LocalDate hasta) {
        return hasta != null ? hasta.plusDays(1).atStartOfDay() : AppTime.ahora().plusDays(1);
    }

    /** Fin exclusivo de un rango que ya llega con hora; si no llega hasta, mañana. */
    public static LocalDateTime fin(LocalDateTime hasta) {
        return hasta != null ? hasta : AppTime.ahora().plusDays(1);
    }

    /**
     * Rango de los reportes: mes actual si no llega nada; el extremo superior se
     * devuelve exclusivo (día siguiente) para comparar con "&lt;".
     */
    public static LocalDate[] rangoReporte(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            YearMonth mes = YearMonth.now(AppTime.ZONA);
            return new LocalDate[]{mes.atDay(1), mes.plusMonths(1).atDay(1)};
        }
        LocalDate h = hasta != null ? hasta.plusDays(1)
                : YearMonth.now(AppTime.ZONA).plusMonths(1).atDay(1);
        LocalDate d = desde != null ? desde : h.minusMonths(1).withDayOfMonth(1);
        return new LocalDate[]{d, h};
    }

    /**
     * Fin inclusivo en días para queries que comparan con "&lt;=" sobre LocalDate.
     * Sin filtro llega hasta mañana para no esconder registros fechados a futuro.
     */
    public static LocalDate finDiaInclusivo(LocalDate hasta) {
        return hasta != null ? hasta : AppTime.hoy().plusDays(1);
    }
}
