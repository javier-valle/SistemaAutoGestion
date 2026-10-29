package com.autogestion.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generador de reportes Excel (.xlsx) profesional con Apache POI.
 * Formato empresarial: encabezados con estilo, columnas ajustadas,
 * fechas y números con formato, totales resaltados, hojas por sección.
 */
public final class ReporteExcelGenerator {

    private ReporteExcelGenerator() { }

    public record Seccion(String titulo, String[] encabezados, List<Object[]> datos, String nota) { }

    public static byte[] generar(List<Seccion> secciones, String tituloGeneral,
                                  LocalDate desde, LocalDate hasta, String rol) {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle headerStyle = crearHeaderStyle(wb);
        CellStyle bodyStyle = crearBodyStyle(wb);
        CellStyle numberStyle = crearNumberStyle(wb);
        CellStyle intStyle = crearIntStyle(wb);
        CellStyle dateCellStyle = crearDateCellStyle(wb);
        CellStyle totalStyle = crearTotalStyle(wb);
        CellStyle titleStyle = crearTitleStyle(wb);
        CellStyle metaStyle = crearMetaStyle(wb);
        CellStyle noteStyle = crearNoteStyle(wb);

        Sheet portada = wb.createSheet("Portada");
        portada.setDefaultColumnWidth(30);

        int rowPort = 0;
        Row rTitle = portada.createRow(rowPort++);
        Cell cTitle = rTitle.createCell(0);
        cTitle.setCellValue(tituloGeneral);
        cTitle.setCellStyle(titleStyle);
        portada.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowPort - 1, rowPort - 1, 0, 3));

        rowPort += 2;
        String rango = (desde != null && hasta != null)
                ? "Del " + fmtFecha(desde) + " al " + fmtFecha(hasta)
                : "Sin rango definido";
        Row rMeta = portada.createRow(rowPort++);
        Cell cMeta = rMeta.createCell(0);
        cMeta.setCellValue("Zona América/Lima  ·  Rol: " + rol + "  ·  " + rango);
        cMeta.setCellStyle(metaStyle);
        portada.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowPort - 1, rowPort - 1, 0, 3));

        rowPort += 2;
        Row rGen = portada.createRow(rowPort++);
        Cell cGen = rGen.createCell(0);
        cGen.setCellValue("AutoGestión — Sistema de gestión para taller automotriz");
        cGen.setCellStyle(noteStyle);
        portada.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowPort - 1, rowPort - 1, 0, 3));

        rowPort += 2;
        Row rFecha = portada.createRow(rowPort++);
        Cell cFecha = rFecha.createCell(0);
        cFecha.setCellValue("Generado el: " + fmtFechaHora(LocalDateTime.now()));
        cFecha.setCellStyle(metaStyle);
        portada.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowPort - 1, rowPort - 1, 0, 3));

        rowPort += 2;
        Row rSecciones = portada.createRow(rowPort++);
        Cell cSecc = rSecciones.createCell(0);
        cSecc.setCellValue("Secciones incluidas en este reporte:");
        cSecc.setCellStyle(metaStyle);
        portada.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowPort - 1, rowPort - 1, 0, 3));

        for (Seccion sec : secciones) {
            Row rItem = portada.createRow(rowPort++);
            Cell cItem = rItem.createCell(0);
            cItem.setCellValue("•  " + sec.titulo);
            cItem.setCellStyle(bodyStyle);
        }


        for (Seccion sec : secciones) {
            String nombreHoja = sanitizeSheetName(sec.titulo);
            Sheet sh = wb.createSheet(nombreHoja);
            sh.setDefaultColumnWidth(16);

            int r = 0;
            Row rh = sh.createRow(r++);
            Cell ch = rh.createCell(0);
            ch.setCellValue(sec.titulo);
            ch.setCellStyle(titleStyle);
            sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, sec.encabezados != null ? sec.encabezados.length - 1 : 3));

            r++;
            if (sec.encabezados != null && sec.encabezados.length > 0) {
                Row hRow = sh.createRow(r++);
                for (int i = 0; i < sec.encabezados.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(sec.encabezados[i]);
                    c.setCellStyle(headerStyle);
                }
            }

            if (sec.datos != null && !sec.datos.isEmpty()) {
                for (Object[] fila : sec.datos) {
                    Row row = sh.createRow(r++);
                    for (int i = 0; i < fila.length; i++) {
                        Cell cell = row.createCell(i);
                        Object val = fila[i];
                        if (val == null || val.toString().isEmpty()) {
                            cell.setCellValue("—");
                            cell.setCellStyle(bodyStyle);
                        } else if (val instanceof Number n) {
                            boolean esEntero = val instanceof Integer || val instanceof Long || val instanceof Short || val instanceof Byte;
                            if (val instanceof BigDecimal bd) {
                                cell.setCellValue(bd.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
                                cell.setCellStyle(numberStyle);
                            } else if (esEntero) {
                                cell.setCellValue(n.longValue());
                                cell.setCellStyle(intStyle);
                            } else {
                                cell.setCellValue(n.doubleValue());
                                cell.setCellStyle(numberStyle);
                            }
                            if (!esEntero && i == fila.length - 1) {
                                cell.setCellStyle(totalStyle);
                            }
                        } else if (val instanceof java.time.LocalDate ld) {
                            cell.setCellValue(java.util.Date.from(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                            cell.setCellStyle(dateCellStyle);
                        } else if (val instanceof java.time.LocalDateTime ldt) {
                            cell.setCellValue(java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
                            cell.setCellStyle(dateCellStyle);
                        } else {
                            cell.setCellValue(val.toString());
                            cell.setCellStyle(bodyStyle);
                        }
                    }
                }
            } else {
                Row emptyRow = sh.createRow(r++);
                Cell emptyCell = emptyRow.createCell(0);
                emptyCell.setCellValue("Sin datos en este rango.");
                emptyCell.setCellStyle(noteStyle);
            }

            if (sec.nota != null && !sec.nota.isBlank()) {
                r++;
                Row noteRow = sh.createRow(r++);
                Cell noteCell = noteRow.createCell(0);
                noteCell.setCellValue(sec.nota);
                noteCell.setCellStyle(noteStyle);
                sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(r - 1, r - 1, 0, sec.encabezados != null ? sec.encabezados.length - 1 : 3));
            }

            if (sec.encabezados != null && sec.encabezados.length > 0) {
                sh.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1 + (sec.datos != null ? sec.datos.size() : 0), 0, sec.encabezados.length - 1));
                sh.createFreezePane(0, 2);
            }
            for (int i = 0; i < (sec.encabezados != null ? sec.encabezados.length : 4); i++) {
                sh.autoSizeColumn(i);
            }
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            wb.write(out);
            wb.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error generando archivo Excel: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }


    private static CellStyle crearHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle crearBodyStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.DOTTED);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setWrapText(true);
        return s;
    }

    private static CellStyle crearNumberStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(crearBodyStyle(wb));
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static CellStyle crearTotalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(crearNumberStyle(wb));
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle crearTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.DARK_BLUE.getIndex());
        f.setFontHeightInPoints((short) 16);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.MEDIUM);
        return s;
    }

    private static CellStyle crearMetaStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        f.setFontHeightInPoints((short) 9);
        s.setFont(f);
        return s;
    }

    private static CellStyle crearNoteStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setColor(IndexedColors.BLUE.getIndex());
        f.setFontHeightInPoints((short) 9);
        s.setFont(f);
        return s;
    }

    private static CellStyle crearIntStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(crearBodyStyle(wb));
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static CellStyle crearDateCellStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(crearBodyStyle(wb));
        s.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        return s;
    }

    private static String sanitizeSheetName(String name) {

        String s = name.replaceAll("[\\\\/\\?\\*\\[\\]\\:]", "");
        s = s.trim();
        if (s.isEmpty()) s = "Hoja";
        s = s.substring(0, Math.min(s.length(), 31));
        return s;
    }

    private static String fmtFecha(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    private static String fmtFechaHora(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
