package com.autogestion.util;

import com.autogestion.dto.ComprobanteResponseDTO;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Constancia de comprobante en PDF (ticket de 80 mm) generada en el backend,
 * sin librerías externas: mismo diseño que el generador que vivía en
 * frontend/pages/pago_entrega.html. Migrado a Java para que la representación
 * impresa salga de la misma fuente de verdad que los datos fiscales.
 *
 * <p>Salida: PDF 1.4 con dos fuentes base (Helvetica / Helvetica-Bold) y un
 * solo página, texto codificado en WinAnsi (Latin-1).</p>
 */
public final class TicketPdf {

    private TicketPdf() { }

    private static final float W = 226.8f;
    private static final int M = 16;
    private static final float R = W - 16;
    private static final float BASE = 560;
    private static final float ALTO_LINEA_DETALLE = 22;

    private static final String DARK = "0.06 0.09 0.16";
    private static final String AMBER = "0.85 0.47 0.02";
    private static final String GREEN = "0.02 0.59 0.41";
    private static final String GRAY = "0.39 0.45 0.55";
    private static final String LINE = "0.89 0.91 0.94";
    private static final String WHITE = "1 1 1";
    private static final String ROJO = "0.86 0.15 0.15";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");

    public static byte[] generar(ComprobanteResponseDTO c) {
        List<ComprobanteResponseDTO.DetalleDTO> detalle =
                c.getDetalle() == null ? List.of() : c.getDetalle();
        float alto = BASE + detalle.size() * ALTO_LINEA_DETALLE;
        Lienzo t = new Lienzo(alto);
        String titulo = "FACTURA".equals(c.getTipo()) ? "FACTURA ELECTRONICA"
                : "BOLETA DE VENTA ELECTRONICA";
        boolean anulado = "ANULADO".equals(c.getEstado());
        String moneda = c.getMoneda() == null ? "PEN" : c.getMoneda();


        t.rect(0, 0, W, 62, DARK);
        String emisor = valor(c.getEmisorNombreComercial(), c.getEmisorRazonSocial());
        t.txt(M, 22, "F2", 11, WHITE, corta(emisor, 30));
        t.txt(M, 36, "F1", 7.5f, WHITE, "RUC " + valor(c.getEmisorRuc(), ""));
        t.txt(M, 48, "F1", 6.5f, "0.75 0.8 0.86", corta(valor(c.getEmisorDireccion(), ""), 44));


        t.rect(0, 62, W, 34, AMBER);
        t.centro(74, "F2", 9, WHITE, titulo);
        t.centro(86, "F2", 11, WHITE, valor(c.getFolio(), ""));


        float y = 112;
        y = t.txt(M, y, "F1", 7, GRAY, valor(c.getClienteTipoDoc(), "-") + " " + valor(c.getClienteNumDoc(), "-")) + 11;
        for (String l : t.wrap(valor(c.getClienteNombre(), ""), 34).stream().limit(2).toList()) {
            y = t.txt(M, y, "F2", 9, DARK, l) + 11;
        }
        if (c.getClienteDireccion() != null && !c.getClienteDireccion().isBlank()) {
            for (String l : t.wrap(c.getClienteDireccion(), 40).stream().limit(2).toList()) {
                y = t.txt(M, y, "F1", 7, GRAY, l) + 9;
            }
        }
        String fecha = c.getFechaEmision() == null ? "-" : c.getFechaEmision().format(FECHA);
        y = t.txt(M, y, "F1", 7, GRAY, fecha + " · " + moneda + " · "
                + valor(c.getFormaPago(), "") + " · " + valor(c.getMetodoPago(), "")) + 8;
        if (c.getObservacion() != null && !c.getObservacion().isBlank()) {
            for (String l : t.wrap(c.getObservacion(), 42).stream().limit(2).toList()) {
                y = t.txt(M, y, "F1", 7, GRAY, l) + 9;
            }
        }
        y += 4;
        y = t.linea(y, LINE, 0.7f) + 12;


        for (var d : detalle) {
            String desc = cant(d.getCantidad()) + " " + valor(d.getUnidadMedida(), "")
                    + " " + valor(d.getDescripcion(), "");
            List<String> trozos = t.wrap(desc, 36).stream().limit(2).toList();
            for (int i = 0; i < trozos.size(); i++) {
                if (i == 0) t.der(R, y, "F2", 8, DARK, dinero(d.getImporteTotal()));
                y = t.txt(M, y, "F1", 7.5f, DARK, trozos.get(i)) + 10;
            }
            y += 2;
        }


        y = t.linea(y, LINE, 0.7f) + 13;
        t.txt(M, y, "F1", 8.5f, GRAY, "Op. gravada");
        t.der(R, y, "F1", 8.5f, DARK, dinero(c.getTotalGravado()));
        y += 12;
        t.txt(M, y, "F1", 8.5f, GRAY, "IGV 18%");
        t.der(R, y, "F1", 8.5f, DARK, dinero(c.getIgv()));
        y += 15;
        y = t.linea(y, DARK, 1.2f) + 15;
        t.txt(M, y, "F2", 11, DARK, "TOTAL");
        t.der(R, y, "F2", 12, DARK, dinero(c.getTotal()));
        y += 16;
        for (String l : t.wrap(valor(c.getTotalLetras(), ""), 44).stream().limit(3).toList()) {
            y = t.txt(M, y, "F1", 6.5f, DARK, l) + 9;
        }
        y += 6;


        for (String l : t.wrap("QR: " + valor(c.getQrContenido(), ""), 52).stream().limit(3).toList()) {
            y = t.txt(M, y, "F1", 5.5f, GRAY, l) + 8;
        }
        y = t.txt(M, y, "F1", 5.5f, GRAY, "Hash: -") + 12;
        y = t.centro(y, "F2", 9, anulado ? ROJO : GREEN, anulado ? "COMPROBANTE ANULADO" : "PAGO REGISTRADO") + 13;
        y = t.centro(y, "F1", 7.5f, GRAY, "Gracias por su preferencia") + 11;
        for (String l : t.wrap("Representación impresa de comprobante INTERNO (proyecto académico). "
                + "Sin validez tributaria ante SUNAT.", 50).stream().limit(3).toList()) {
            t.centro(y, "F1", 6, GRAY, l);
            y += 8;
        }
        return t.enPDF();
    }



    private static String valor(String v, String porDefecto) {
        return v == null || v.isBlank() ? porDefecto : v;
    }

    private static String corta(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Como hacía Number(v).toFixed(2) en el navegador. */
    private static String dinero(java.math.BigDecimal v) {
        return String.format(Locale.ROOT, "S/ %.2f", v == null ? java.math.BigDecimal.ZERO : v);
    }

    /** El navegador mostraba 1.000 como "1": sin ceros sobrantes. */
    private static String cant(java.math.BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }



    private static final class Lienzo {
        private final float alto;
        private final List<String> ops = new ArrayList<>();

        private Lienzo(float alto) { this.alto = alto; }

        private static String n(float v) { return String.format(Locale.ROOT, "%.1f", v); }

        private void color(String c) { ops.add(c + " rg"); }

        private void rect(float x, float yTop, float w, float h, String c) {
            color(c);
            ops.add(n(x) + " " + n(alto - yTop - h) + " " + n(w) + " " + n(h) + " re f");
        }

        /** Devuelve la nueva posición vertical (yTop + delta). */
        private float linea(float yTop, String c, float grosor) {
            color(c);
            ops.add(grosor + " w " + n(M) + " " + n(alto - yTop) + " m " + n(R)
                    + " " + n(alto - yTop) + " l S");
            return yTop;
        }

        private float txt(float x, float yTop, String font, float size, String c, String texto) {
            color(c);
            ops.add("BT /" + font + " " + size + " Tf " + n(x) + " " + n(alto - yTop)
                    + " Td (" + esc(texto) + ") Tj ET");
            return yTop;
        }

        private float centro(float yTop, String font, float size, String c, String texto) {
            return txt((W - ancho(texto, font, size)) / 2, yTop, font, size, c, texto);
        }

        private float der(float xR, float yTop, String font, float size, String c, String texto) {
            return txt(xR - ancho(texto, font, size), yTop, font, size, c, texto);
        }

        /** Estimación de ancho que usaba el generador JS (Helvetica ~0.5 em). */
        private static float ancho(String t, String font, float size) {
            return t.length() * size * ("F2".equals(font) ? 0.55f : 0.5f);
        }

        private List<String> wrap(String t, int n) {
            List<String> out = new ArrayList<>();
            String s = t;
            while (s.length() > n) {
                out.add(s.substring(0, n));
                s = s.substring(n);
            }
            out.add(s);
            return out;
        }

        private byte[] enPDF() {
            String stream = String.join("\n", ops);
            List<String> objetos = List.of(
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(W) + " " + n(alto)
                            + "] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>",
                    "<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out, "%PDF-1.4\n");
            int[] offs = new int[objetos.size()];
            for (int i = 0; i < objetos.size(); i++) {
                offs[i] = out.size();
                write(out, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
            }
            int inicioXref = out.size();
            StringBuilder xref = new StringBuilder("xref\n0 ").append(objetos.size() + 1)
                    .append("\n0000000000 65535 f \n");
            for (int off : offs) {
                xref.append(String.format(Locale.ROOT, "%010d", off)).append(" 00000 n \n");
            }
            xref.append("trailer\n<< /Size ").append(objetos.size() + 1)
                    .append(" /Root 1 0 R >>\nstartxref\n").append(inicioXref).append("\n%%EOF");
            write(out, xref.toString());
            return out.toByteArray();
        }

        /** Todo el PDF se compone en Latin-1 (WinAnsi), igual que el generador JS. */
        private static void write(ByteArrayOutputStream out, String s) {
            out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
        }

        /** Escapa para cadena de PDF y de paso normaliza a WinAnsi. */
        private static String esc(String t) {
            String limpio = winAnsi(t);
            StringBuilder sb = new StringBuilder(limpio.length());
            for (int i = 0; i < limpio.length(); i++) {
                char ch = limpio.charAt(i);
                if (ch == '\\' || ch == '(' || ch == ')') sb.append('\\');
                sb.append(ch);
            }
            return sb.toString();
        }

        private static String winAnsi(String t) {
            StringBuilder sb = new StringBuilder(t.length());
            for (int i = 0; i < t.length(); i++) {
                char ch = t.charAt(i);
                if (ch >= 0x20 && ch <= 0x7E) { sb.append(ch); continue; }
                if (ch >= 0xA0 && ch <= 0xFF) { sb.append(ch); continue; }
                switch (ch) {
                    case '–', '—', '―', '−' -> sb.append('-');
                    case '‘', '’', '‚' -> sb.append('\'');
                    case '“', '”', '„' -> sb.append('"');
                    case '…' -> sb.append("...");
                    case ' ', ' ', ' ' -> sb.append(' ');
                    case '×' -> sb.append('x');
                    default -> sb.append('?');
                }
            }
            return sb.toString();
        }
    }
}
