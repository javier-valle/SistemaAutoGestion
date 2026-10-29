package com.autogestion.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TDD: el límite por IP permite N consultas por ventana y bloquea la siguiente. */
class ConsultaDocumentoRateLimitTest {

    @Test
    void permiteHastaElMaximoYLuegoBloquea() {
        ConsultaDocumentoRateLimit limite = new ConsultaDocumentoRateLimit(3, 10);
        assertTrue(limite.permitir("10.0.0.1"));
        assertTrue(limite.permitir("10.0.0.1"));
        assertTrue(limite.permitir("10.0.0.1"));
        assertFalse(limite.permitir("10.0.0.1"));
    }

    @Test
    void cadaIpTieneSuPropioContador() {
        ConsultaDocumentoRateLimit limite = new ConsultaDocumentoRateLimit(1, 10);
        assertTrue(limite.permitir("10.0.0.1"));
        assertFalse(limite.permitir("10.0.0.1"));
        assertTrue(limite.permitir("10.0.0.2"));
    }

    @Test
    void ipNulaOEnBlancoSeAgrupaComoDesconocida() {
        ConsultaDocumentoRateLimit limite = new ConsultaDocumentoRateLimit(1, 10);
        assertTrue(limite.permitir(null));
        assertFalse(limite.permitir("  "));
    }
}
