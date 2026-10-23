package com.autogestion.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ReportePdfGenerator {
    private ReportePdfGenerator() { }

    public record Seccion(String titulo, List<String[]> datos, String[] encabezados, String nota) { }

    public static byte[] generar(List<Seccion> secciones, String tituloGeneral,
                                  LocalDate desde, LocalDate hasta, String rol) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (Seccion sec : secciones) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = 750;
                    cs.setFont(fontBold, 14);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(sec.titulo != null ? sec.titulo : tituloGeneral);
                    cs.endText();
                    y -= 30;

                    if (sec.encabezados != null && sec.encabezados.length > 0) {
                        float x = 50;
                        float colW = 400f / sec.encabezados.length;
                        cs.setFont(fontBold, 9);
                        for (int i = 0; i < sec.encabezados.length; i++) {
                            cs.beginText();
                            cs.newLineAtOffset(x + 4, y);
                            cs.showText(sec.encabezados[i]);
                            cs.endText();
                            x += colW;
                        }
                        y -= 16;
                    }

                    cs.setFont(fontNormal, 8);
                    if (sec.datos != null) {
                        for (String[] fila : sec.datos) {
                            float x = 50;
                            float colW = 400f / (fila != null ? fila.length : 1);
                            for (String c : fila) {
                                String text = c != null ? c : "—";
                                cs.beginText();
                                cs.newLineAtOffset(x + 2, y);
                                cs.showText(text.length() > 30 ? text.substring(0, 30) : text);
                                cs.endText();
                                x += colW;
                            }
                            y -= 12;
                            if (y < 60) break;
                        }
                    }
                    if (sec.nota != null && !sec.nota.isBlank()) {
                        cs.beginText();
                        cs.setFont(fontNormal, 7);
                        cs.newLineAtOffset(50, y - 20);
                        cs.showText("Nota: " + sec.nota);
                        cs.endText();
                    }
                }
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private static String fmtFecha(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}