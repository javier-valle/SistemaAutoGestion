package com.autogestion.util;

import com.autogestion.dto.ComprobanteResponseDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Representación impresa del comprobante en A4, con la estructura clásica de
 * una boleta o factura electrónica: recuadro del emisor, caja del número,
 * datos del adquirente, detalle por columnas, bloque de importes, desglose de
 * tributos y texto legal enmarcado.
 *
 * <p>Se compone a mano en PDF 1.4 (Helvetica, Helvetica-Bold y
 * Helvetica-Oblique) para no añadir dependencias. No firma XML ni envía nada a
 * SUNAT: es la representación de un comprobante interno de demostración.</p>
 */
public final class TicketPdf {

    private TicketPdf() { }

    private static final float W = 595.28f;
    private static final float H = 841.89f;
    private static final float IZQ = 42f;
    private static final float DER = 553f;

    private static final String NEGRO = "0 0 0";
    private static final String GRIS = "0.34 0.37 0.42";
    private static final String GRIS_CLARO = "0.62 0.65 0.69";
    private static final String ROJO = "0.72 0.09 0.09";
    private static final String VERDE = "0.02 0.45 0.32";

    private static final float C_CANT = 96f;
    private static final float X_UNIDAD = 108f;
    private static final float X_DESC = 172f;
    private static final float ANCHO_DESCRIPCION = 152f;
    private static final float C_VUNIT = 352f;
    private static final float C_DSCTO = 406f;
    private static final float C_IMPORTE = 496f;
    private static final float C_ICBPER = 544f;
    private static final float X_ETIQUETA = 426f;
    private static final float X_CAJA = 430f;
    private static final float X_VALOR = 536f;

    private static final float Y_TABLA = 276f;
    private static final float Y_FIN_TOTALES = H - 292f;
    private static final float Y_FIN_CONTINUA = H - 110f;
    private static final float FILA = 13.5f;
    private static final float FILA_DOBLE = 21f;

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);
    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private static final Map<String, String> MONEDAS = Map.of(
            "PEN", "SOLES",
            "USD", "DÓLARES AMERICANOS",
            "EUR", "EUROS");

    private static final Map<String, String> UNIDADES = Map.ofEntries(
            Map.entry("ZZ", "UNIDAD"),
            Map.entry("NIU", "UNIDAD"),
            Map.entry("KGM", "KILOGRAMO"),
            Map.entry("GRM", "GRAMO"),
            Map.entry("LTR", "LITRO"),
            Map.entry("MLT", "MILILITRO"),
            Map.entry("MTR", "METRO"),
            Map.entry("MTQ", "METRO CÚBICO"),
            Map.entry("HUR", "HORA"),
            Map.entry("MIN", "MINUTO"),
            Map.entry("DZN", "DOCENA"),
            Map.entry("BX", "CAJA"),
            Map.entry("SET", "JUEGO"),
            Map.entry("GLL", "GALÓN"));

    public static byte[] generar(ComprobanteResponseDTO c) {
        boolean factura = "FACTURA".equals(c.getTipo());
        boolean anulado = "ANULADO".equals(c.getEstado());
        String titulo = factura ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA";
        String moneda = MONEDAS.getOrDefault(valor(c.getMoneda(), "PEN").toUpperCase(Locale.ROOT), "SOLES");

        List<Item> items = new ArrayList<>();
        for (ComprobanteResponseDTO.DetalleDTO d : c.getDetalle() == null
                ? List.<ComprobanteResponseDTO.DetalleDTO>of() : c.getDetalle()) {
            items.add(new Item(d));
        }

        List<List<Item>> paginasItems = repartir(items);
        List<Pagina> paginas = new ArrayList<>();

        for (int i = 0; i < paginasItems.size(); i++) {
            boolean ultima = i == paginasItems.size() - 1;
            Pagina p = new Pagina();
            float y = cabecera(p, c, titulo, moneda, paginas.size() + 1, paginasItems.size());

            for (Item it : paginasItems.get(i)) {
                y = fila(p, it, y);
            }

            float fin;
            if (ultima) {
                fin = totales(p, c, y, anulado);
            } else {
                fin = cierreParcial(p, y);
            }

            marco(p, fin);
            pie(p, paginas.size() + 1, paginasItems.size(), anulado);
            paginas.add(p);
        }

        if (paginas.isEmpty()) {
            Pagina p = new Pagina();
            float y = cabecera(p, c, titulo, moneda, 1, 1);
            float fin = totales(p, c, y, anulado);
            marco(p, fin);
            pie(p, 1, 1, anulado);
            paginas.add(p);
        }
        return enPDF(paginas);
    }

    private static float cabecera(Pagina p, ComprobanteResponseDTO c, String titulo,
                                  String moneda, int numero, int total) {
        String fechaImpresion = LocalDateTime.now().format(FECHA);
        p.txt(IZQ, 30, "F1", 7.5f, GRIS, fechaImpresion);
        p.centro(30, "F2", 7.5f, NEGRO, "::: " + titulo + " - Impresión :::");

        p.caja(330, 50, DER - 8 - 330, 60, 1.2f);
        p.der(DER - 16, 65, "F2", 8.5f, NEGRO, titulo);
        p.der(DER - 16, 80, "F2", 8.5f, NEGRO, "RUC: " + valor(c.getEmisorRuc(), "-"));
        p.der(DER - 16, 98, "F2", 9.5f, NEGRO, valor(c.getFolio(), "-"));
        if (total > 1) {
            p.der(DER - 16, 106, "F1", 6f, GRIS, "Hoja " + numero + " de " + total);
        }

        float ye = 66;
        String razon = valor(c.getEmisorRazonSocial(), valor(c.getEmisorNombreComercial(), "-"));
        p.txt(IZQ + 10, ye, "F2", 9.5f, NEGRO, razon);
        ye += 13;
        for (String l : p.wrap(valor(c.getEmisorDireccion(), ""), 52).stream().limit(2).toList()) {
            p.txt(IZQ + 10, ye, "F1", 7.5f, NEGRO, l);
            ye += 10;
        }
        String contacto = "";
        if (c.getEmisorUbigeo() != null && !c.getEmisorUbigeo().isBlank()) {
            contacto = c.getEmisorUbigeo();
        }
        if (c.getEmisorTelefono() != null && !c.getEmisorTelefono().isBlank()) {
            contacto = contacto.isBlank() ? "TEL " + c.getEmisorTelefono()
                    : contacto + "  ·  TEL " + c.getEmisorTelefono();
        }
        if (!contacto.isBlank()) {
            p.txt(IZQ + 10, ye, "F1", 7.5f, NEGRO, contacto);
        }

        p.linea(IZQ, 120, DER, 0.6f, NEGRO);

        float yf = 138;
        yf = campo(p, yf, "Fecha de Vencimiento", c.getFechaEmision() == null ? "-"
                : c.getFechaEmision().format(FECHA));
        yf = campo(p, yf, "Fecha de Emisión", c.getFechaEmision() == null ? "-"
                : c.getFechaEmision().format(FECHA_HORA));
        yf = campo(p, yf, "Señor(es)", valor(c.getClienteNombre(), "-"));
        yf = campo(p, yf, valor(c.getClienteTipoDoc(), "DOC"), valor(c.getClienteNumDoc(), "-"));
        yf = campo(p, yf, "Tipo de Moneda", moneda);
        if (c.getClienteDireccion() != null && !c.getClienteDireccion().isBlank()) {
            yf = campo(p, yf, "Dirección", c.getClienteDireccion());
        }
        List<String> obs = p.wrap(valor(c.getObservacion(), "-"), 62);
        for (int i = 0; i < Math.min(2, obs.size()); i++) {
            yf = campo(p, yf, i == 0 ? "Observación" : "", obs.get(i));
        }

        p.linea(IZQ, 246, DER, 0.6f, NEGRO);
        p.der(C_CANT, 254, "F1", 7.5f, NEGRO, "Cantidad");
        p.txt(X_UNIDAD, 254, "F1", 7.5f, NEGRO, "Unidad");
        p.txt(X_UNIDAD, 264, "F1", 7.5f, NEGRO, "Medida");
        p.txt(X_DESC, 254, "F1", 7.5f, NEGRO, "Descripción");
        p.der(C_VUNIT, 254, "F1", 7.5f, NEGRO, "Valor");
        p.der(C_VUNIT, 264, "F1", 7.5f, NEGRO, "Unitario(*)");
        p.der(C_DSCTO, 254, "F1", 7.5f, NEGRO, "Descuento(*)");
        p.der(C_IMPORTE, 254, "F1", 7.5f, NEGRO, "Importe de");
        p.der(C_IMPORTE, 264, "F1", 7.5f, NEGRO, "Venta(**)");
        p.der(C_ICBPER, 254, "F1", 7.5f, NEGRO, "ICBPER");
        p.linea(IZQ, 270, DER, 0.6f, NEGRO);
        return Y_TABLA;
    }

    private static float campo(Pagina p, float y, String etiqueta, String valor) {
        if (!etiqueta.isBlank()) {
            p.der(164, y, "F2", 7.5f, NEGRO, etiqueta);
            p.txt(168, y, "F2", 7.5f, NEGRO, ":");
        }
        p.txt(176, y, "F1", 8, NEGRO, valor);
        return y + 12.5f;
    }

    private static float fila(Pagina p, Item it, float y) {
        p.der(C_CANT, y, "F1", 8, NEGRO, it.cantidad);
        p.txt(X_UNIDAD, y, "F1", 8, NEGRO, it.unidad);
        for (int i = 0; i < it.lineas.size(); i++) {
            p.txt(X_DESC, y + i * 9.5f, "F1", 8, NEGRO, it.lineas.get(i));
        }
        p.der(C_VUNIT, y, "F1", 8, NEGRO, it.valorUnitario);
        p.der(C_DSCTO, y, "F1", 8, NEGRO, it.descuento);
        p.der(C_IMPORTE, y, "F1", 8, NEGRO, it.importe);
        p.der(C_ICBPER, y, "F1", 8, NEGRO, it.icbper);
        return y + it.alto();
    }

    private static float totales(Pagina p, ComprobanteResponseDTO c,
                                 float y, boolean anulado) {
        p.linea(IZQ, y + 6, DER, 0.6f, NEGRO);
        float yt = y + 18;
        yt = importe(p, yt, "Otros Cargos :", dinero(BigDecimal.ZERO), false, false);
        yt = importe(p, yt, "Otros Tributos :", dinero(BigDecimal.ZERO), false, false);
        yt = importe(p, yt, "ICBPER :", dinero(BigDecimal.ZERO), true, false);
        yt = importe(p, yt, "Importe Total :", dinero(c.getTotal()), true, true);

        String letras = valor(c.getTotalLetras(), "-");
        String son = letras.toUpperCase(Locale.ROOT).startsWith("SON")
                ? letras : "SON: " + letras;
        float ys = yt + 10;
        for (String linea : envolver(son, "F2", 8.5f, X_VALOR - IZQ - 10)) {
            p.der(X_VALOR, ys, "F2", 8.5f, NEGRO, linea);
            ys += 10;
        }
        yt = ys;
        p.linea(IZQ, yt, DER, 0.6f, NEGRO);

        float yi = yt + 14;
        p.txt(IZQ + 6, yi, "F1", 7.5f, NEGRO, "(*) Sin impuestos.");
        p.txt(IZQ + 6, yi + 10, "F1", 7.5f, NEGRO,
                "(**) Incluye impuestos, de ser Op. Gravada.");
        String qr = valor(c.getQrContenido(), "");
        if (!qr.isBlank()) {
            dibujarQr(p, qr, IZQ + 8, yi + 22, 58);
            p.txt(IZQ + 76, yi + 44, "F1", 6.5f, GRIS, "Escanea el QR para");
            p.txt(IZQ + 76, yi + 53, "F1", 6.5f, GRIS, "verificar los datos del");
            p.txt(IZQ + 76, yi + 62, "F1", 6.5f, GRIS, "comprobante.");
        }

        float yd = yi - 3;
        yd = desglose(p, yd, "Op. Gravada :", dinero(c.getTotalGravado()), false);
        yd = desglose(p, yd, "Op. Exonerada :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "Op. Inafecta :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "ISC :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "IGV :", dinero(c.getIgv()), false);
        yd = desglose(p, yd, "ICBPER :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "Otros Cargos :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "Otros Tributos :", dinero(BigDecimal.ZERO), false);
        yd = desglose(p, yd, "Importe Total :", dinero(c.getTotal()), true);

        float inicioLegal = Math.max(yd, yi + (qr.isBlank() ? 30 : 90)) + 6;
        float altoLegal = 38;
        p.caja(IZQ + 6, inicioLegal, DER - IZQ - 12, altoLegal, 0.6f);
        List<String> legal = p.wrap("Esta es una representación impresa de un comprobante "
                + "electrónico de demostración, generada por el Sistema de AutoGestión "
                + "(proyecto académico). No ha sido firmada digitalmente ni remitida a SUNAT, "
                + "por lo que carece de validez tributaria.", 118);
        float yl = inicioLegal + 10;
        for (String l : legal) {
            p.centro(yl, "F3", 6.6f, NEGRO, l);
            yl += 8f;
        }

        float fin = inicioLegal + altoLegal;
        String estado = anulado ? "COMPROBANTE ANULADO" : "PAGO REGISTRADO";
        p.centro(fin + 14, "F2", 8.5f, anulado ? ROJO : VERDE, estado);
        if (anulado && c.getMotivoAnulacion() != null && !c.getMotivoAnulacion().isBlank()) {
            p.centro(fin + 24, "F1", 7, ROJO, "Motivo: " + c.getMotivoAnulacion());
        }
        return anulado && c.getMotivoAnulacion() != null ? fin + 32 : fin + 22;
    }

    /**
     * QR vectorial: se pintan los módulos como rectángulos negros. Sin imágenes
     * incrustadas el PDF sigue siendo liviano y el código queda nítido a cualquier
     * zoom. Si el contenido no se puede codificar, la hoja simplemente no lo lleva.
     */
    private static void dibujarQr(Pagina p, String contenido, float x, float yTop, float lado) {
        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 33, 33);
        } catch (WriterException e) {
            return;
        }
        p.caja(x - 3, yTop - 3, lado + 6, lado + 6, 0.6f);
        float modulo = lado / matriz.getWidth();
        for (int fila = 0; fila < matriz.getHeight(); fila++) {
            for (int col = 0; col < matriz.getWidth(); col++) {
                if (matriz.get(col, fila)) {
                    p.rectRelleno(x + col * modulo, yTop + fila * modulo, modulo, modulo);
                }
            }
        }
    }

    private static float cierreParcial(Pagina p, float y) {
        p.centro(y + 24, "F1", 7.5f, GRIS, "Continúa en la hoja siguiente");
        return y + 40;
    }

    private static float importe(Pagina p, float y, String etiqueta, String valor,
                                 boolean caja, boolean fuerte) {
        String font = fuerte ? "F2" : "F1";
        p.der(X_ETIQUETA, y + 8, font, 7.5f, NEGRO, etiqueta);
        if (caja) {
            p.caja(X_CAJA, y - 1, X_VALOR - X_CAJA + 8, 12, 0.6f);
        }
        p.der(X_VALOR, y + 8, font, 7.5f, NEGRO, valor);
        return y + 12;
    }

    private static float desglose(Pagina p, float y, String etiqueta, String valor, boolean fuerte) {
        String font = fuerte ? "F2" : "F1";
        p.der(X_ETIQUETA, y + 8, font, 7.5f, NEGRO, etiqueta);
        p.caja(X_CAJA, y - 1, X_VALOR - X_CAJA + 8, 11, 0.6f);
        p.der(X_VALOR, y + 8, font, 7.5f, NEGRO, valor);
        return y + 11;
    }

    private static void marco(Pagina p, float fin) {
        p.caja(IZQ, 44, DER - IZQ, fin - 44, 1f);
    }

    private static void pie(Pagina p, int numero, int total, boolean anulado) {
        p.txt(IZQ, H - 34, "F1", 6.5f, GRIS_CLARO,
                "Sistema de AutoGestión · comprobante interno de demostración"
                        + (anulado ? " · ANULADO" : ""));
        p.der(DER, H - 34, "F1", 6.5f, GRIS_CLARO, numero + "/" + Math.max(1, total));
    }

    private static List<List<Item>> repartir(List<Item> items) {
        List<List<Item>> paginas = new ArrayList<>();
        if (items.isEmpty()) {
            paginas.add(new ArrayList<>());
            return paginas;
        }
        float presupuestoUltima = Y_FIN_TOTALES - Y_TABLA;
        float presupuestoContinua = Y_FIN_CONTINUA - Y_TABLA;
        int i = 0;
        while (i < items.size()) {
            float resto = 0;
            for (int k = i; k < items.size(); k++) {
                resto += items.get(k).alto();
            }
            List<Item> pagina = new ArrayList<>();
            if (resto <= presupuestoUltima) {
                pagina.addAll(items.subList(i, items.size()));
                paginas.add(pagina);
                return paginas;
            }
            float acumulado = 0;
            while (i < items.size()) {
                float alto = items.get(i).alto();
                if (acumulado + alto > presupuestoContinua) break;
                acumulado += alto;
                pagina.add(items.get(i));
                i++;
            }
            if (pagina.isEmpty()) {
                pagina.add(items.get(i));
                i++;
            }
            paginas.add(pagina);
        }
        return paginas;
    }

    private static final class Item {
        private final String cantidad;
        private final String unidad;
        private final List<String> lineas;
        private final String valorUnitario;
        private final String descuento = "0.00";
        private final String importe;
        private final String icbper = "0.00";

        private Item(ComprobanteResponseDTO.DetalleDTO d) {
            this.cantidad = cant(d.getCantidad());
            this.unidad = UNIDADES.getOrDefault(
                    valor(d.getUnidadMedida(), "ZZ").toUpperCase(Locale.ROOT), "UNIDAD");
            this.valorUnitario = decimal(d.getValorUnitario() == null
                    ? d.getPrecioUnitario() : d.getValorUnitario());
            this.importe = decimal(d.getImporteTotal());
            List<String> trozos = envolver(valor(d.getDescripcion(), "-"), "F1", 8, ANCHO_DESCRIPCION);
            if (trozos.size() > 2) {
                this.lineas = List.of(trozos.get(0),
                        recortar(trozos.get(1), "F1", 8, ANCHO_DESCRIPCION));
            } else {
                this.lineas = trozos;
            }
        }

        private float alto() {
            return lineas.size() > 1 ? FILA_DOBLE : FILA;
        }
    }

    private static String recortar(String texto, String font, float size, float maximo) {
        String corte = texto;
        while (!corte.isEmpty() && ancho(corte + "...", font, size) > maximo) {
            corte = corte.substring(0, corte.length() - 1);
        }
        return corte.trim() + "...";
    }

    private static String valor(String v, String porDefecto) {
        return v == null || v.isBlank() ? porDefecto : v;
    }

    private static String decimal(BigDecimal v) {
        return v == null ? "0.00" : String.format(Locale.ROOT, "%.2f", v);
    }

    private static String dinero(BigDecimal v) {
        return "S/ " + decimal(v);
    }

    private static String cant(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }

    private static byte[] enPDF(List<Pagina> paginas) {
        List<String> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) {
            kids.append(i == 0 ? "" : " ").append(6 + i * 2).append(" 0 R");
        }
        objetos.add("<< /Type /Pages /Kids [" + kids + "] /Count " + paginas.size() + " >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < paginas.size(); i++) {
            int pagina = 6 + i * 2;
            int contenido = pagina + 1;
            objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(W) + " " + n(H)
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents "
                    + contenido + " 0 R >>");
            String stream = String.join("\n", paginas.get(i).ops);
            objetos.add("<< /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length
                    + " >>\nstream\n" + stream + "\nendstream");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        escribir(out, "%PDF-1.4\n");
        int[] offs = new int[objetos.size()];
        for (int i = 0; i < objetos.size(); i++) {
            offs[i] = out.size();
            escribir(out, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
        }
        int inicioXref = out.size();
        StringBuilder xref = new StringBuilder("xref\n0 ").append(objetos.size() + 1)
                .append("\n0000000000 65535 f \n");
        for (int off : offs) {
            xref.append(String.format(Locale.ROOT, "%010d", off)).append(" 00000 n \n");
        }
        xref.append("trailer\n<< /Size ").append(objetos.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(inicioXref).append("\n%%EOF");
        escribir(out, xref.toString());
        return out.toByteArray();
    }

    private static String n(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static final class Pagina {
        private final List<String> ops = new ArrayList<>();

        private void color(String c) {
            ops.add(c + " rg");
        }

        private void caja(float x, float yTop, float w, float h, float grosor) {
            color(NEGRO);
            ops.add(grosor + " w " + n(x) + " " + n(H - yTop - h) + " " + n(w) + " "
                    + n(h) + " re S");
        }

        private void rectRelleno(float x, float yTop, float w, float h) {
            color(NEGRO);
            ops.add(n(x) + " " + n(H - yTop - h) + " " + n(w) + " " + n(h) + " re f");
        }

        private void linea(float x1, float yTop, float x2, float grosor, String c) {
            color(c);
            ops.add(grosor + " w " + n(x1) + " " + n(H - yTop) + " m " + n(x2) + " "
                    + n(H - yTop) + " l S");
        }

        private void txt(float x, float yTop, String font, float size, String c, String texto) {
            if (texto == null || texto.isEmpty()) return;
            color(c);
            ops.add("BT /" + font + " " + size + " Tf " + n(x) + " " + n(H - yTop)
                    + " Td (" + esc(texto) + ") Tj ET");
        }

        private void centro(float yTop, String font, float size, String c, String texto) {
            txt((W - ancho(texto, font, size)) / 2f, yTop, font, size, c, texto);
        }

        private void der(float xDer, float yTop, String font, float size, String c, String texto) {
            txt(xDer - ancho(texto, font, size), yTop, font, size, c, texto);
        }

        private List<String> wrap(String texto, int maximo) {
            List<String> salida = new ArrayList<>();
            String resto = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
            while (resto.length() > maximo) {
                int corte = resto.lastIndexOf(' ', maximo);
                if (corte <= 0) corte = maximo;
                salida.add(resto.substring(0, corte).trim());
                resto = resto.substring(corte).trim();
            }
            if (!resto.isBlank()) salida.add(resto);
            if (salida.isEmpty()) salida.add("-");
            return salida;
        }
    }

    private static List<String> envolver(String texto, String font, float size, float maximo) {
        List<String> salida = new ArrayList<>();
        String resto = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        if (resto.isEmpty()) {
            salida.add("-");
            return salida;
        }
        while (ancho(resto, font, size) > maximo) {
            int corte = resto.length();
            while (corte > 1 && ancho(resto.substring(0, corte), font, size) > maximo) {
                corte--;
            }
            int espacio = resto.lastIndexOf(' ', corte);
            if (espacio > 0) corte = espacio;
            if (corte <= 0) corte = 1;
            salida.add(resto.substring(0, corte).trim());
            resto = resto.substring(corte).trim();
        }
        if (!resto.isBlank()) salida.add(resto);
        return salida;
    }

    private static float ancho(String texto, String font, float size) {
        if (texto == null || texto.isEmpty()) return 0;
        boolean negrita = "F2".equals(font);
        int total = 0;
        for (int i = 0; i < texto.length(); i++) {
            total += glifo(texto.charAt(i), negrita);
        }
        return total * size / 1000f;
    }

    private static final int[] ANCHOS = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};

    private static final int[] ANCHOS_NEGRITA = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
            975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
            333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
            611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    private static int glifo(char ch, boolean negrita) {
        if (ch >= 32 && ch <= 126) {
            return negrita ? ANCHOS_NEGRITA[ch - 32] : ANCHOS[ch - 32];
        }
        char base = sinAcento(ch);
        if (base != ch) return glifo(base, negrita);
        return switch (ch) {
            case '·' -> 278;
            case '°', 'º' -> 400;
            case '€' -> 556;
            default -> negrita ? 611 : 556;
        };
    }

    private static char sinAcento(char ch) {
        return switch (ch) {
            case 'á', 'à', 'â', 'ä', 'ã', 'å' -> 'a';
            case 'é', 'è', 'ê', 'ë' -> 'e';
            case 'í', 'ì', 'î', 'ï' -> 'i';
            case 'ó', 'ò', 'ô', 'ö', 'õ' -> 'o';
            case 'ú', 'ù', 'û', 'ü' -> 'u';
            case 'ñ' -> 'n';
            case 'ç' -> 'c';
            case 'Á', 'À', 'Â', 'Ä', 'Ã', 'Å' -> 'A';
            case 'É', 'È', 'Ê', 'Ë' -> 'E';
            case 'Í', 'Ì', 'Î', 'Ï' -> 'I';
            case 'Ó', 'Ò', 'Ô', 'Ö', 'Õ' -> 'O';
            case 'Ú', 'Ù', 'Û', 'Ü' -> 'U';
            case 'Ñ' -> 'N';
            case 'Ç' -> 'C';
            default -> ch;
        };
    }

    private static void escribir(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

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
            if (ch == '\u00A0' || ch == '\u2007' || ch == '\u202F') { sb.append(' '); continue; }
            if (ch >= 0xA0 && ch <= 0xFF) { sb.append(ch); continue; }
            switch (ch) {
                case '–', '—', '―', '−' -> sb.append('-');
                case '‘', '’', '‚' -> sb.append('\'');
                case '“', '”', '„' -> sb.append('"');
                case '…' -> sb.append("...");
                case '×' -> sb.append('x');
                default -> sb.append('?');
            }
        }
        return sb.toString();
    }
}
