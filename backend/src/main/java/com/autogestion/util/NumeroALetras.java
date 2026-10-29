package com.autogestion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte importes a letras estilo SUNAT: "SON: MIL DOSCIENTOS TREINTA Y 50/100 SOLES".
 * Soporta hasta 999 999 999. Los centavos van como fracción /100.
 */
public final class NumeroALetras {

    private NumeroALetras() { }

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
        "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES",
        "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    public static String convertir(BigDecimal total) {
        return convertir(total, "SOLES");
    }

    public static String convertir(BigDecimal total, String moneda) {
        BigDecimal t = total.setScale(2, RoundingMode.HALF_UP);
        long enteros = t.longValue();
        int centavos = t.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String letras = enteros == 0 ? "CERO" : enLetras(enteros);

        return "SON: " + letras + " Y " + String.format("%02d", centavos) + "/100 " + moneda;
    }

    static String enLetras(long n) {
        if (n < 0 || n > 999_999_999L) throw new IllegalArgumentException("Fuera de rango: " + n);
        if (n < 30) return UNIDADES[(int) n];
        if (n < 100) {
            long d = n / 10, r = n % 10;
            return r == 0 ? DECENAS[(int) d] : DECENAS[(int) d] + " Y " + UNIDADES[(int) r];
        }
        if (n < 1000) {
            if (n == 100) return "CIEN";
            long c = n / 100, r = n % 100;
            return r == 0 ? CENTENAS[(int) c] : CENTENAS[(int) c] + " " + enLetras(r);
        }
        if (n < 1_000_000) {
            long m = n / 1000, r = n % 1000;
            String miles = m == 1 ? "MIL" : enLetras(m) + " MIL";
            return r == 0 ? miles : miles + " " + enLetras(r);
        }
        long m = n / 1_000_000, r = n % 1_000_000;
        String millones = m == 1 ? "UN MILLON" : enLetras(m) + " MILLONES";
        return r == 0 ? millones : millones + " " + enLetras(r);
    }
}
