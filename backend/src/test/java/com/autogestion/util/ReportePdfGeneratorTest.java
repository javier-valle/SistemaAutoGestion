package com.autogestion.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReportePdfGeneratorTest {

    @Test
    void pdfGeneradoNoCorrupto() throws Exception {
        var sec = new ReportePdfGenerator.Seccion("Test", java.util.List.of(
            new String[]{"A", "B"},
            new String[]{"C", "D"}
        ), new String[]{"Col1", "Col2"}, null);
        byte[] pdf = ReportePdfGenerator.generar(java.util.List.of(sec), "Título", java.time.LocalDate.now(), java.time.LocalDate.now(), "ADMIN");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "PDF muy pequeño");
        String start = new String(pdf, 0, Math.min(20, pdf.length), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-1.4", start.trim().substring(0, 9), "Debe ser PDF 1.4");
        String texto = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(texto.contains("Test"), "Debe contener el título de la sección");
    }
}
