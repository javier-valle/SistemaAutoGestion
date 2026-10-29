package com.autogestion.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reporte PDF con portada, tablas paginadas, filas alternas y numeración.
 * A4 horizontal; el texto se ajusta a cada columna y se corta por páginas
 * repitiendo el encabezado.
 */
public final class ReportePdfGenerator {

    private static final float W = PDRectangle.A4.getHeight();
    private static final float H = PDRectangle.A4.getWidth();
    private static final float M = 38f;
    private static final float ANCHO = W - M * 2;

    private static final PDColor NAVY = rgb(22, 35, 58);
    private static final PDColor NAVY_SUAVE = rgb(34, 51, 79);
    private static final PDColor AMBER = rgb(180, 83, 9);
    private static final PDColor CREMA = rgb(255, 247, 237);
    private static final PDColor GRIS = rgb(100, 116, 139);
    private static final PDColor TEXTO = rgb(31, 41, 55);
    private static final PDColor LINEA = rgb(226, 232, 240);
    private static final PDColor BLANCO = rgb(255, 255, 255);

    private ReportePdfGenerator() { }

    public record Seccion(String titulo, List<String[]> datos, String[] encabezados, String nota) { }

    public static byte[] generar(List<Seccion> secciones, String tituloGeneral,
                                  LocalDate desde, LocalDate hasta, String rol) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            dibujarPortada(doc, bold, normal, secciones, tituloGeneral, desde, hasta, rol);

            List<PDPage> paginas = new ArrayList<>();
            for (Seccion sec : secciones) {
                dibujarSeccion(doc, bold, normal, sec, desde, hasta, rol, paginas);
            }
            for (int i = 0; i < paginas.size(); i++) {
                dibujarPie(doc, bold, normal, paginas.get(i), i + 1, paginas.size());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private static void dibujarPortada(PDDocument doc, PDType1Font bold, PDType1Font normal,
                                       List<Seccion> secciones, String titulo,
                                       LocalDate desde, LocalDate hasta, String rol) throws Exception {
        PDPage page = new PDPage(new PDRectangle(W, H));
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            rect(cs, 0, H - 148, W, 148, NAVY);
            rect(cs, 0, H - 154, W, 6, AMBER);

            texto(cs, bold, 27, BLANCO, M, H - 62, "AutoGestion");
            texto(cs, normal, 12, rgb(199, 210, 229), M, H - 86, "Reporte consolidado del taller");
            texto(cs, normal, 9, rgb(148, 163, 184), M, H - 106,
                    "Sistema de gestión para taller automotriz  ·  Zona América/Lima");

            texto(cs, bold, 15, TEXTO, M, H - 190, texto(titulo, 90));
            texto(cs, bold, 11, AMBER, M, H - 212, rango(desde, hasta));
            texto(cs, normal, 9, GRIS, M, H - 230,
                    "Rol: " + (rol == null ? "ADMIN" : rol) + "   ·   Generado: " + fechaHora(LocalDateTime.now()));

            linea(cs, M, H - 246, W - M, H - 246, LINEA, 0.8f);
            texto(cs, bold, 10, TEXTO, M, H - 266, "CONTENIDO DEL REPORTE");

            float y = H - 288;
            for (int i = 0; i < secciones.size(); i++) {
                Seccion s = secciones.get(i);
                rect(cs, M, y - 3, 18, 14, CREMA);
                texto(cs, bold, 8, AMBER, M + 5, y, String.format("%02d", i + 1));
                texto(cs, normal, 10, TEXTO, M + 26, y, texto(s.titulo(), 74));
                int filas = s.datos() == null ? 0 : s.datos().size();
                texto(cs, normal, 8, GRIS, W - M - 90, y, filas + " fila(s)");
                y -= 19;
                if (y < M + 40) break;
            }

            rect(cs, 0, 0, W, 26, NAVY_SUAVE);
            texto(cs, normal, 7.5f, rgb(199, 210, 229), M, 10,
                    "AutoGestión  ·  Reporte generado automáticamente a partir de los movimientos registrados.");
        }
    }

    private static void dibujarSeccion(PDDocument doc, PDType1Font bold, PDType1Font normal,
                                       Seccion sec, LocalDate desde, LocalDate hasta,
                                       String rol, List<PDPage> paginas) throws Exception {
        int cols = columnas(sec);
        float[] anchos = anchos(sec, cols);
        List<String[]> datos = sec.datos() == null ? List.of() : sec.datos();
        int i = 0;
        PDPage ultima = null;
        float ultimoY = 0;

        do {
            PDPage page = new PDPage(new PDRectangle(W, H));
            doc.addPage(page);
            paginas.add(page);
            ultima = page;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                dibujarCabecera(cs, bold, normal, sec, desde, hasta, rol);
                float y = H - 92;
                filaEncabezado(cs, bold, sec, cols, anchos, y);
                y -= 22;

                if (datos.isEmpty()) {
                    texto(cs, normal, 9, GRIS, M + 4, y - 8, "Sin datos en este rango.");
                    y -= 26;
                }

                int inicioPagina = i;
                while (i < datos.size()) {
                    String[] fila = datos.get(i);
                    List<List<String>> lineas = new ArrayList<>();
                    float alto = 0;
                    for (int c = 0; c < cols; c++) {
                        String valor = c < fila.length ? safe(fila[c]) : "";
                        List<String> ls = envolver(valor.isBlank() ? "-" : valor, normal, 7.6f, anchos[c] - 10);
                        lineas.add(ls);
                        alto = Math.max(alto, ls.size() * 9.4f + 8);
                    }
                    if (y - alto < M + 34 && i > inicioPagina) break;

                    if (i % 2 == 1) rect(cs, M, y - alto, ANCHO, alto, rgb(249, 250, 252));
                    for (int c = 0; c < cols; c++) {
                        float x = M + 5 + desplazamiento(anchos, c);
                        boolean derecha = esNumero(c < fila.length ? fila[c] : null);
                        float cy = y - 11;
                        for (String ln : lineas.get(c)) {
                            float tx = derecha
                                    ? x + anchos[c] - 10 - ancho(ln, normal, 7.6f)
                                    : x;
                            texto(cs, normal, 7.6f, TEXTO, tx, cy, ln);
                            cy -= 9.4f;
                        }
                    }
                    linea(cs, M, y - alto, W - M, y - alto, LINEA, 0.5f);
                    y -= alto;
                    i++;
                }
                ultimoY = y;
            }
        } while (i < datos.size());

        if (sec.nota() != null && !sec.nota().isBlank()) {
            List<String> nota = envolver("Nota: " + sec.nota(), normal, 7.4f, ANCHO - 16);
            float alto = 12 * nota.size() + 10;
            if (ultimoY - alto < M + 34) {
                PDPage page = new PDPage(new PDRectangle(W, H));
                doc.addPage(page);
                paginas.add(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    dibujarCabecera(cs, bold, normal, sec, desde, hasta, rol);
                    ultima = page;
                    dibujarNota(cs, normal, nota, H - 110, alto);
                }
            } else {
                try (PDPageContentStream cs = new PDPageContentStream(doc, ultima,
                        org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true)) {
                    dibujarNota(cs, normal, nota, ultimoY - 12, alto);
                }
            }
        }
    }

    private static void dibujarNota(PDPageContentStream cs, PDType1Font normal,
                                    List<String> nota, float y, float alto) throws Exception {
        rect(cs, M, y - alto, ANCHO, alto, CREMA);
        float ny = y - 13;
        for (String ln : nota) {
            texto(cs, normal, 7.4f, GRIS, M + 8, ny, ln);
            ny -= 12;
        }
    }

    private static void dibujarCabecera(PDPageContentStream cs, PDType1Font bold, PDType1Font normal,
                                        Seccion sec, LocalDate desde, LocalDate hasta,
                                        String rol) throws Exception {
        rect(cs, 0, H - 58, W, 58, NAVY);
        rect(cs, 0, H - 61, W, 3, AMBER);
        texto(cs, bold, 14, BLANCO, M, H - 30, texto(sec.titulo(), 62));
        texto(cs, normal, 8, rgb(199, 210, 229), W - M - 92, H - 30, "AutoGestion");
        texto(cs, normal, 8, GRIS, M, H - 76,
                rango(desde, hasta) + "   ·   Rol: " + (rol == null ? "ADMIN" : rol));
    }

    private static void filaEncabezado(PDPageContentStream cs, PDType1Font bold, Seccion sec,
                                       int cols, float[] anchos, float y) throws Exception {
        rect(cs, M, y - 20, ANCHO, 20, AMBER);
        for (int c = 0; c < cols; c++) {
            String h = sec.encabezados() != null && c < sec.encabezados().length ? sec.encabezados()[c] : "";
            float x = M + 5 + desplazamiento(anchos, c);
            texto(cs, bold, 8, BLANCO, x, y - 13, texto(h, 34));
        }
    }

    private static float[] anchos(Seccion sec, int cols) {
        float[] pesos = new float[cols];
        for (int c = 0; c < cols; c++) {
            int max = sec.encabezados() != null && c < sec.encabezados().length
                    ? sec.encabezados()[c].length() : 8;
            if (sec.datos() != null) {
                for (String[] fila : sec.datos()) {
                    if (c < fila.length) max = Math.max(max, Math.min(60, safe(fila[c]).length()));
                }
            }
            pesos[c] = Math.max(7, Math.min(38, max));
        }
        float suma = 0;
        for (float p : pesos) suma += p;
        float[] w = new float[cols];
        for (int c = 0; c < cols; c++) w[c] = Math.max(48, ANCHO * pesos[c] / suma);
        float total = 0;
        for (float x : w) total += x;
        if (total > ANCHO) {
            for (int c = 0; c < cols; c++) w[c] = w[c] * ANCHO / total;
        }
        return w;
    }

    private static int columnas(Seccion sec) {
        int cols = sec.encabezados() == null ? 1 : sec.encabezados().length;
        if (sec.datos() != null) {
            for (String[] f : sec.datos()) cols = Math.max(cols, f.length);
        }
        return Math.max(1, cols);
    }

    private static float desplazamiento(float[] anchos, int hasta) {
        float x = 0;
        for (int i = 0; i < hasta; i++) x += anchos[i];
        return x;
    }

    private static void dibujarPie(PDDocument doc, PDType1Font bold, PDType1Font normal,
                                   PDPage page, int num, int total) throws Exception {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true)) {
            linea(cs, M, 26, W - M, 26, LINEA, 0.6f);
            texto(cs, normal, 7, GRIS, M, 15, "AutoGestión  ·  Reporte consolidado del taller");
            String pag = "Página " + num + " de " + total;
            texto(cs, bold, 7, GRIS, W - M - normal.getStringWidth(pag) / 1000f * 7, 15, pag);
        }
    }

    private static List<String> envolver(String texto, PDType1Font font, float tam, float ancho) throws Exception {
        List<String> salida = new ArrayList<>();
        if (texto == null || texto.isBlank()) { salida.add("-"); return salida; }
        for (String parrafo : texto.split("\\n")) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split("\\s+")) {
                if (palabra.isEmpty()) continue;
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (ancho(prueba, font, tam) <= ancho || actual.length() == 0) {
                    if (ancho(prueba, font, tam) > ancho && actual.length() == 0) {
                        String resto = palabra;
                        while (ancho(resto, font, tam) > ancho && resto.length() > 1) {
                            int corte = resto.length();
                            while (corte > 1 && ancho(resto.substring(0, corte), font, tam) > ancho) corte--;
                            salida.add(resto.substring(0, corte));
                            resto = resto.substring(corte);
                        }
                        actual = new StringBuilder(resto);
                    } else {
                        actual = new StringBuilder(prueba);
                    }
                } else {
                    salida.add(actual.toString());
                    actual = new StringBuilder(palabra);
                }
            }
            if (actual.length() > 0) salida.add(actual.toString());
        }
        if (salida.isEmpty()) salida.add("-");
        return salida;
    }

    private static float ancho(String s, PDType1Font font, float tam) throws Exception {
        return font.getStringWidth(s) / 1000f * tam;
    }

    private static boolean esNumero(String s) {
        if (s == null || s.isBlank()) return false;
        String t = s.trim();
        if (t.startsWith("S/")) return true;
        return t.matches("^-?\\d+([.,]\\d+)?%?$");
    }

    private static void texto(PDPageContentStream cs, PDType1Font font, float tam, PDColor color,
                              float x, float y, String t) throws Exception {
        String limpio = safe(t);
        if (limpio.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, tam);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(limpio);
        cs.endText();
    }

    private static void rect(PDPageContentStream cs, float x, float y, float w, float h, PDColor color) throws Exception {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void linea(PDPageContentStream cs, float x1, float y1, float x2, float y2,
                              PDColor color, float grosor) throws Exception {
        cs.setStrokingColor(color);
        cs.setLineWidth(grosor);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static PDColor rgb(int r, int g, int b) {
        return new PDColor(new float[]{r / 255f, g / 255f, b / 255f}, PDDeviceRGB.INSTANCE);
    }

    private static String rango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) return "Sin rango definido";
        return "Del " + fecha(desde) + " al " + fecha(hasta);
    }

    private static String fecha(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String fechaHora(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String texto(String s, int max) {
        String t = safe(s);
        if (t.length() <= max) return t;
        return t.substring(0, max - 1) + "...";
    }

    /** Las fuentes estándar usan WinAnsi: reemplaza símbolos fuera de ese juego. */
    private static String safe(String s) {
        if (s == null) return "";
        String t = s.replace('\u2013', '-').replace('\u2014', '-')
                .replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u201c', '"').replace('\u201d', '"')
                .replace("\u2022", "-")
                .replace("\u2192", "->").replace("\u2265", ">=").replace("\u2264", "<=")
                .replace("\u2713", "OK").replace("\u00a0", " ");
        StringBuilder sb = new StringBuilder(t.length());
        for (char c : t.toCharArray()) {
            if (c == '\n' || c == '\r' || (c >= 32 && c <= 126) || (c >= 160 && c <= 255)) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
