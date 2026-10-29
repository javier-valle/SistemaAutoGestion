package com.autogestion.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

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
        // PDFBox 3 puede escribir 1.6 o superior; basta con que sea un PDF válido.
        assertTrue(start.startsWith("%PDF-1."), "Debe empezar con la firma de un PDF");
        // El contenido va comprimido: se extrae el texto con PDFBox en vez de buscar bytes crudos.
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(texto.contains("Test"), "Debe contener el título de la sección");
        }
    }
}
