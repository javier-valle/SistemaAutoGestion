package com.autogestion.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generador de reportes Excel (.xlsx) con Apache POI.
 * Portada de marca, una hoja por sección, encabezados fijos, filas alternas,
 * formato monetario, totales y áreas de impresión listas para A4 horizontal.
 */
public final class ReporteExcelGenerator {

    private static final String NAVY = "16233A";
    private static final String NAVY_SOFT = "22334F";
    private static final String AMBER = "B45309";
    private static final String CREAM = "FFF7ED";
    private static final String BORDER = "E2E8F0";
    private static final String TEXT = "1F2937";
    private static final String MUTED = "64748B";

    private ReporteExcelGenerator() { }

    /** Tipo de columna: decide formato y si entra en la fila de totales. */
    public enum Tipo { TEXTO, ENTERO, DINERO, DECIMAL, PROMEDIO }

    public record Columna(String titulo, Tipo tipo, boolean totaliza) {
        public static Columna texto(String titulo) { return new Columna(titulo, Tipo.TEXTO, false); }
        public static Columna entero(String titulo, boolean totaliza) { return new Columna(titulo, Tipo.ENTERO, totaliza); }
        public static Columna dinero(String titulo, boolean totaliza) { return new Columna(titulo, Tipo.DINERO, totaliza); }
        public static Columna decimal(String titulo, boolean totaliza) { return new Columna(titulo, Tipo.DECIMAL, totaliza); }
    }

    public record Seccion(String titulo, List<Columna> columnas, List<Object[]> datos, String nota) {
        public String[] encabezados() {
            return columnas == null ? new String[0]
                    : columnas.stream().map(Columna::titulo).toArray(String[]::new);
        }
    }

    public static byte[] generar(List<Seccion> secciones, String tituloGeneral,
                                  LocalDate desde, LocalDate hasta, String rol) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Estilos st = new Estilos(wb);
            crearPortada(wb, st, secciones, tituloGeneral, desde, hasta, rol);
            for (Seccion sec : secciones) {
                crearHoja(wb, st, sec, desde, hasta, rol);
            }
            wb.setActiveSheet(0);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error generando archivo Excel: " + e.getMessage(), e);
        }
    }

    private static void crearPortada(XSSFWorkbook wb, Estilos st, List<Seccion> secciones,
                                     String tituloGeneral, LocalDate desde, LocalDate hasta, String rol) {
        XSSFSheet sh = wb.createSheet("Portada");
        sh.setDefaultColumnWidth(26);
        sh.setColumnWidth(0, 30 * 256);
        sh.setColumnWidth(1, 30 * 256);
        sh.setColumnWidth(2, 14 * 256);
        sh.setColumnWidth(3, 14 * 256);
        sh.setDisplayGridlines(false);

        Row banda = sh.createRow(0);
        banda.setHeightInPoints(46);
        for (int i = 0; i < 4; i++) {
            Cell c = banda.createCell(i);
            c.setCellStyle(st.banda);
        }
        sh.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Cell marca = banda.getCell(0);
        marca.setCellValue("AutoGestion");
        marca.setCellStyle(st.marca);

        Row sub = sh.createRow(1);
        sub.setHeightInPoints(26);
        Cell csub = sub.createCell(0);
        csub.setCellValue("Reporte consolidado del taller");
        csub.setCellStyle(st.bandaSub);
        sh.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

        Row titulo = sh.createRow(3);
        titulo.setHeightInPoints(24);
        Cell ct = titulo.createCell(0);
        ct.setCellValue(tituloGeneral == null || tituloGeneral.isBlank()
                ? "Reporte consolidado" : tituloGeneral);
        ct.setCellStyle(st.tituloPortada);
        sh.addMergedRegion(new CellRangeAddress(3, 3, 0, 3));

        Row rango = sh.createRow(5);
        Cell cr = rango.createCell(0);
        cr.setCellValue(rangoTexto(desde, hasta));
        cr.setCellStyle(st.destacado);
        sh.addMergedRegion(new CellRangeAddress(5, 5, 0, 3));

        Row meta = sh.createRow(6);
        Cell cm = meta.createCell(0);
        cm.setCellValue("Rol: " + (rol == null ? "ADMIN" : rol)
                + "   |   Zona: America/Lima   |   Generado: " + fmtFechaHora(LocalDateTime.now()));
        cm.setCellStyle(st.meta);
        sh.addMergedRegion(new CellRangeAddress(6, 6, 0, 3));

        Row separador = sh.createRow(8);
        Cell cs = separador.createCell(0);
        cs.setCellValue("Contenido del reporte");
        cs.setCellStyle(st.seccionTitulo);
        sh.addMergedRegion(new CellRangeAddress(8, 8, 0, 3));

        int r = 9;
        for (int i = 0; i < secciones.size(); i++) {
            Seccion sec = secciones.get(i);
            Row fila = sh.createRow(r++);
            fila.setHeightInPoints(20);
            Cell num = fila.createCell(0);
            num.setCellValue(String.format("%02d", i + 1));
            num.setCellStyle(st.numeroPortada);
            Cell nom = fila.createCell(1);
            nom.setCellValue(sec.titulo());
            nom.setCellStyle(st.nombrePortada);
            Cell hoja = fila.createCell(2);
            hoja.setCellValue("Hoja " + (i + 2));
            hoja.setCellStyle(st.meta);
            Cell filas = fila.createCell(3);
            filas.setCellValue((sec.datos() == null ? 0 : sec.datos().size()) + " fila(s)");
            filas.setCellStyle(st.meta);
        }

        Row pie = sh.createRow(r + 2);
        Cell cp = pie.createCell(0);
        cp.setCellValue("Generado por AutoGestion: sistema de gestion para taller automotriz.");
        cp.setCellStyle(st.pie);
        sh.addMergedRegion(new CellRangeAddress(r + 2, r + 2, 0, 3));

        wb.setPrintArea(wb.getSheetIndex(sh), 0, 3, 0, Math.max(1, r + 2));
        sh.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sh.getPrintSetup().setLandscape(true);
    }

    private static void crearHoja(XSSFWorkbook wb, Estilos st, Seccion sec,
                                  LocalDate desde, LocalDate hasta, String rol) {
        XSSFSheet sh = wb.createSheet(nombreHoja(sec.titulo()));
        sh.setDisplayGridlines(false);
        sh.setDefaultRowHeightInPoints(18);

        List<Columna> columnas = sec.columnas() == null ? List.of() : sec.columnas();
        int nCols = Math.max(1, columnas.size());

        Row banda = sh.createRow(0);
        banda.setHeightInPoints(30);
        for (int i = 0; i < nCols; i++) banda.createCell(i).setCellStyle(st.bandaHoja);
        sh.addMergedRegion(new CellRangeAddress(0, 0, 0, nCols - 1));
        Cell titulo = banda.getCell(0);
        titulo.setCellValue(sec.titulo());
        titulo.setCellStyle(st.tituloHoja);

        Row sub = sh.createRow(1);
        sh.addMergedRegion(new CellRangeAddress(1, 1, 0, nCols - 1));
        Cell csub = sub.createCell(0);
        csub.setCellValue(rangoTexto(desde, hasta) + "   |   Rol: " + (rol == null ? "ADMIN" : rol));
        csub.setCellStyle(st.subHoja);

        if (!columnas.isEmpty()) {
            Row h = sh.createRow(2);
            h.setHeightInPoints(24);
            for (int i = 0; i < nCols; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(columnas.get(i).titulo());
                c.setCellStyle(st.encabezado);
            }
        }

        List<Object[]> filas = sec.datos() == null ? List.of() : sec.datos();
        BigDecimal[] totales = new BigDecimal[nCols];

        int r = 3;
        for (int idx = 0; idx < filas.size(); idx++) {
            Object[] datos = filas.get(idx);
            Row row = sh.createRow(r++);
            row.setHeightInPoints(19);
            for (int i = 0; i < nCols; i++) {
                Object val = i < datos.length ? datos[i] : null;
                Columna col = i < columnas.size() ? columnas.get(i) : null;
                Cell cell = row.createCell(i);
                pintarValor(cell, val, i == 0, idx, st, col == null ? Tipo.TEXTO : col.tipo());
                if (col != null && col.totaliza() && val instanceof Number n) {
                    totales[i] = (totales[i] == null ? BigDecimal.ZERO : totales[i])
                            .add(new BigDecimal(n.toString()));
                }
            }
        }

        int ultimaFilaDatos = r - 1;
        boolean hayTotales = filas.size() > 1 && columnas.stream().anyMatch(Columna::totaliza);
        if (hayTotales) {
            Row total = sh.createRow(r);
            total.setHeightInPoints(22);
            for (int i = 0; i < nCols; i++) {
                Cell c = total.createCell(i);
                c.setCellStyle(st.total);
                if (i == 0) {
                    c.setCellValue("TOTAL");
                    continue;
                }
                Columna col = i < columnas.size() ? columnas.get(i) : null;
                if (col != null && col.totaliza() && totales[i] != null) {
                    CellStyle estilo = estiloTotal(col.tipo(), st);
                    c.setCellStyle(estilo);
                    escribirNumero(c, totales[i], estilo);
                }
            }
        }

        if (filas.isEmpty()) {
            Row vacio = sh.createRow(3);
            sh.addMergedRegion(new CellRangeAddress(3, 3, 0, nCols - 1));
            Cell c = vacio.createCell(0);
            c.setCellValue("Sin datos en este rango.");
            c.setCellStyle(st.vacio);
        }

        int filaNota = -1;
        if (sec.nota() != null && !sec.nota().isBlank()) {
            int nr = Math.max(r, 4) + 1;
            sh.addMergedRegion(new CellRangeAddress(nr, nr, 0, nCols - 1));
            Cell c = sh.createRow(nr).createCell(0);
            c.setCellValue("Nota: " + sec.nota());
            c.setCellStyle(st.nota);
            filaNota = nr;
        }

        ajustarColumnas(sh, sec, filas, nCols);
        if (!columnas.isEmpty()) {
            int fin = Math.max(2, ultimaFilaDatos);
            sh.setAutoFilter(new CellRangeAddress(2, fin, 0, nCols - 1));
            sh.createFreezePane(0, 3);
            sh.setRepeatingRows(new CellRangeAddress(2, 2, 0, nCols - 1));
        }
        wb.setPrintArea(wb.getSheetIndex(sh), 0, nCols - 1, 0, Math.max(Math.max(3, r), filaNota));
        PrintSetup ps = sh.getPrintSetup();
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setLandscape(true);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        sh.setFitToPage(true);
        sh.setHorizontallyCenter(true);
        sh.setMargin(org.apache.poi.ss.usermodel.PageMargin.LEFT, 0.4);
        sh.setMargin(org.apache.poi.ss.usermodel.PageMargin.RIGHT, 0.4);
        sh.setMargin(org.apache.poi.ss.usermodel.PageMargin.TOP, 0.5);
        sh.setMargin(org.apache.poi.ss.usermodel.PageMargin.BOTTOM, 0.5);
    }

    private static void pintarValor(Cell cell, Object val, boolean primera, int idx, Estilos st, Tipo tipo) {
        if (val == null || val.toString().isBlank()) {
            cell.setCellValue("-");
            cell.setCellStyle(primera ? st.celdaFuerte(idx) : st.celda(idx));
            return;
        }
        if (val instanceof BigDecimal bd) {
            escribirNumero(cell, bd, st.moneda(idx));
            return;
        }
        if (val instanceof Number n) {
            switch (tipo) {
                case DINERO -> escribirNumero(cell, new BigDecimal(n.toString()), st.moneda(idx));
                case ENTERO -> {
                    cell.setCellValue(n.longValue());
                    cell.setCellStyle(st.entero(idx));
                }
                case DECIMAL, PROMEDIO -> {
                    cell.setCellValue(n.doubleValue());
                    cell.setCellStyle(st.decimal(idx));
                }
                default -> {
                    cell.setCellValue(val.toString());
                    cell.setCellStyle(primera ? st.celdaFuerte(idx) : st.celda(idx));
                }
            }
            return;
        }
        if (val instanceof LocalDate ld) {
            cell.setCellValue(java.util.Date.from(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            cell.setCellStyle(st.fecha(idx));
            return;
        }
        if (val instanceof LocalDateTime ldt) {
            cell.setCellValue(java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            cell.setCellStyle(st.fecha(idx));
            return;
        }
        cell.setCellValue(val.toString());
        cell.setCellStyle(primera ? st.celdaFuerte(idx) : st.celda(idx));
    }

    private static void escribirNumero(Cell cell, BigDecimal valor, CellStyle estilo) {
        cell.setCellValue(valor.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
        cell.setCellStyle(estilo);
    }

    private static CellStyle estiloTotal(Tipo tipo, Estilos st) {
        return tipo == Tipo.DINERO ? st.totalNumero : st.totalEntero;
    }

    private static void ajustarColumnas(Sheet sh, Seccion sec, List<Object[]> filas, int nCols) {
        List<Columna> columnas = sec.columnas() == null ? List.of() : sec.columnas();
        for (int i = 0; i < nCols; i++) {
            int max = i < columnas.size() ? largo(columnas.get(i).titulo()) : 8;
            for (Object[] fila : filas) {
                if (i < fila.length) {
                    Object v = fila[i];
                    boolean dinero = i < columnas.size() && columnas.get(i).tipo() == Tipo.DINERO;
                    String t = (dinero && v instanceof Number n)
                            ? "S/ " + new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP)
                            : textoCelda(v);
                    max = Math.max(max, largo(t));
                }
            }
            int ancho = Math.min(48, Math.max(10, max + 3));
            sh.setColumnWidth(i, ancho * 256);
        }
    }

    private static int largo(String s) {
        if (s == null) return 0;
        int n = 0;
        for (char c : s.toCharArray()) n += c > 127 ? 2 : 1;
        return n;
    }

    private static String textoCelda(Object v) {
        if (v == null) return "-";
        if (v instanceof BigDecimal bd) return "S/ " + bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        if (v instanceof LocalDate ld) return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (v instanceof LocalDateTime ldt) return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return v.toString();
    }

    private static String nombreHoja(String nombre) {
        String s = nombre == null ? "Hoja" : nombre.replaceAll("[\\\\/?*\\[\\]:]", "").trim();
        if (s.isEmpty()) s = "Hoja";
        return s.substring(0, Math.min(s.length(), 31));
    }

    private static String rangoTexto(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) return "Sin rango definido";
        return "Del " + fmtFecha(desde) + " al " + fmtFecha(hasta);
    }

    private static String fmtFecha(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String fmtFechaHora(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static XSSFColor color(String hex) {
        XSSFColor c = new XSSFColor();
        byte[] rgb = new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
        c.setRGB(rgb);
        return c;
    }

    private static XSSFFont fuente(XSSFWorkbook wb, int tamano, boolean negrita, String hex) {
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) tamano);
        f.setBold(negrita);
        f.setColor(color(hex));
        return f;
    }

    private static final class Estilos {
        final XSSFWorkbook wb;
        final CellStyle banda, bandaSub, marca, tituloPortada, destacado, meta, numeroPortada,
                pie, bandaHoja, tituloHoja, vacio, nota;
        final XSSFCellStyle seccionTitulo, nombrePortada, subHoja, encabezado,
                total, totalNumero, totalEntero;
        final CellStyle[] clara, alterna, claraFuerte, alternaFuerte, monedaClara, monedaAlterna,
                enteroClara, enteroAlterna, decimalClara, decimalAlterna, fechaClara, fechaAlterna;

        Estilos(XSSFWorkbook wb) {
            this.wb = wb;
            banda = wb.createCellStyle();
            banda.setFillForegroundColor(color(NAVY));
            banda.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            bandaSub = wb.createCellStyle();
            bandaSub.setFillForegroundColor(color(NAVY_SOFT));
            bandaSub.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bandaSub.setFont(fuente(wb, 11, false, "C7D2E5"));
            bandaSub.setAlignment(HorizontalAlignment.CENTER);
            bandaSub.setVerticalAlignment(VerticalAlignment.CENTER);

            marca = wb.createCellStyle();
            marca.cloneStyleFrom(banda);
            marca.setFont(fuente(wb, 24, true, "FFFFFF"));
            marca.setAlignment(HorizontalAlignment.CENTER);
            marca.setVerticalAlignment(VerticalAlignment.CENTER);

            tituloPortada = wb.createCellStyle();
            tituloPortada.setFont(fuente(wb, 15, true, TEXT));
            tituloPortada.setAlignment(HorizontalAlignment.CENTER);

            destacado = wb.createCellStyle();
            destacado.setFont(fuente(wb, 12, true, AMBER));
            destacado.setAlignment(HorizontalAlignment.CENTER);

            meta = wb.createCellStyle();
            meta.setFont(fuente(wb, 9, false, MUTED));
            meta.setAlignment(HorizontalAlignment.CENTER);

            seccionTitulo = wb.createCellStyle();
            seccionTitulo.setFont(fuente(wb, 11, true, TEXT));
            seccionTitulo.setBorderBottom(BorderStyle.THIN);
            seccionTitulo.setBottomBorderColor(color(BORDER));

            numeroPortada = wb.createCellStyle();
            numeroPortada.setFont(fuente(wb, 10, true, AMBER));
            numeroPortada.setAlignment(HorizontalAlignment.CENTER);

            nombrePortada = wb.createCellStyle();
            nombrePortada.setFont(fuente(wb, 10, false, TEXT));
            nombrePortada.setBorderBottom(BorderStyle.HAIR);
            nombrePortada.setBottomBorderColor(color(BORDER));

            pie = wb.createCellStyle();
            pie.setFont(fuente(wb, 8, false, MUTED));
            pie.setAlignment(HorizontalAlignment.CENTER);

            bandaHoja = wb.createCellStyle();
            bandaHoja.setFillForegroundColor(color(NAVY));
            bandaHoja.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            bandaHoja.setBorderBottom(BorderStyle.NONE);

            tituloHoja = wb.createCellStyle();
            tituloHoja.cloneStyleFrom(bandaHoja);
            tituloHoja.setFont(fuente(wb, 13, true, "FFFFFF"));
            tituloHoja.setAlignment(HorizontalAlignment.LEFT);
            tituloHoja.setVerticalAlignment(VerticalAlignment.CENTER);
            tituloHoja.setIndention((short) 1);

            subHoja = wb.createCellStyle();
            subHoja.setFont(fuente(wb, 8, false, MUTED));
            subHoja.setAlignment(HorizontalAlignment.LEFT);
            subHoja.setIndention((short) 1);
            subHoja.setBorderBottom(BorderStyle.THIN);
            subHoja.setBottomBorderColor(color(BORDER));

            encabezado = wb.createCellStyle();
            encabezado.setFillForegroundColor(color(AMBER));
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setFont(fuente(wb, 9, true, "FFFFFF"));
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            borde(encabezado, AMBER);
            encabezado.setWrapText(true);

            total = wb.createCellStyle();
            total.setFillForegroundColor(color(CREAM));
            total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            total.setFont(fuente(wb, 9, true, TEXT));
            total.setAlignment(HorizontalAlignment.LEFT);
            total.setVerticalAlignment(VerticalAlignment.CENTER);
            borde(total, BORDER);
            total.setBorderTop(BorderStyle.DOUBLE);
            total.setTopBorderColor(color(AMBER));

            totalNumero = wb.createCellStyle();
            totalNumero.cloneStyleFrom(total);
            totalNumero.setAlignment(HorizontalAlignment.RIGHT);
            totalNumero.setDataFormat(wb.createDataFormat().getFormat("\"S/ \"#,##0.00"));

            totalEntero = wb.createCellStyle();
            totalEntero.cloneStyleFrom(total);
            totalEntero.setAlignment(HorizontalAlignment.RIGHT);
            totalEntero.setDataFormat(wb.createDataFormat().getFormat("#,##0"));

            vacio = wb.createCellStyle();
            vacio.setFont(fuente(wb, 9, false, MUTED));
            vacio.setAlignment(HorizontalAlignment.CENTER);
            vacio.setVerticalAlignment(VerticalAlignment.CENTER);

            nota = wb.createCellStyle();
            nota.setFont(fuente(wb, 8, false, MUTED));
            nota.setAlignment(HorizontalAlignment.LEFT);
            nota.setWrapText(true);

            clara = new CellStyle[2];
            alterna = new CellStyle[2];
            claraFuerte = new CellStyle[2];
            alternaFuerte = new CellStyle[2];
            monedaClara = new CellStyle[2];
            monedaAlterna = new CellStyle[2];
            enteroClara = new CellStyle[2];
            enteroAlterna = new CellStyle[2];
            decimalClara = new CellStyle[2];
            decimalAlterna = new CellStyle[2];
            fechaClara = new CellStyle[2];
            fechaAlterna = new CellStyle[2];
            for (int k = 0; k < 2; k++) {
                String fondo = k == 1 ? CREAM : "FFFFFF";
                clara[k] = base(fondo, false, false, false);
                alterna[k] = clara[k];
                claraFuerte[k] = base(fondo, true, false, false);
                alternaFuerte[k] = claraFuerte[k];
                monedaClara[k] = base(fondo, false, true, false);
                monedaAlterna[k] = monedaClara[k];
                enteroClara[k] = base(fondo, false, false, false);
                enteroAlterna[k] = enteroClara[k];
                decimalClara[k] = base(fondo, false, false, false);
                decimalAlterna[k] = decimalClara[k];
                enteroClara[k].setDataFormat(wb.createDataFormat().getFormat("#,##0"));
                enteroAlterna[k] = enteroClara[k];
                decimalClara[k].setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
                decimalAlterna[k] = decimalClara[k];
                monedaClara[k].setDataFormat(wb.createDataFormat().getFormat("\"S/ \"#,##0.00"));
                monedaAlterna[k] = monedaClara[k];
                fechaClara[k] = base(fondo, false, false, false);
                fechaClara[k].setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));
                fechaAlterna[k] = fechaClara[k];
            }
        }

        private XSSFCellStyle base(String fondo, boolean negrita, boolean derecha, boolean centrado) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(color(fondo));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setFont(fuente(wb, 9, negrita, TEXT));
            s.setAlignment(derecha ? HorizontalAlignment.RIGHT
                    : centrado ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(true);
            borde(s, BORDER);
            return s;
        }

        private void borde(XSSFCellStyle s, String hex) {
            s.setBorderBottom(BorderStyle.HAIR);
            s.setBorderTop(BorderStyle.HAIR);
            s.setBorderLeft(BorderStyle.HAIR);
            s.setBorderRight(BorderStyle.HAIR);
            s.setBottomBorderColor(color(hex));
            s.setTopBorderColor(color(hex));
            s.setLeftBorderColor(color(hex));
            s.setRightBorderColor(color(hex));
        }

        CellStyle celda(int idx) { return clara[idx % 2]; }
        CellStyle celdaFuerte(int idx) { return claraFuerte[idx % 2]; }
        CellStyle moneda(int idx) { return monedaClara[idx % 2]; }
        CellStyle entero(int idx) { return enteroClara[idx % 2]; }
        CellStyle decimal(int idx) { return decimalClara[idx % 2]; }
        CellStyle fecha(int idx) { return fechaClara[idx % 2]; }
    }
}
