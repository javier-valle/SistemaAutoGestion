package com.autogestion.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReporteExcelGeneratorTest {

    @Test
    void excelReleeTiposYSumaSoloColumnasMarcadas() throws Exception {
        List<ReporteExcelGenerator.Seccion> secciones = new ArrayList<>();
        secciones.add(new ReporteExcelGenerator.Seccion("Test",
                List.of(ReporteExcelGenerator.Columna.texto("A"),
                        ReporteExcelGenerator.Columna.entero("B", true),
                        new ReporteExcelGenerator.Columna("Promedio",
                                ReporteExcelGenerator.Tipo.DECIMAL, false)),
                List.of(new Object[]{"Hola", 42, 3.5},
                        new Object[]{"Mundo", 8, 4.5}),
                null));

        byte[] excel = ReporteExcelGenerator.generar(secciones, "T", LocalDate.now(), LocalDate.now(), "ADMIN");

        assertNotNull(excel);
        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excel))) {
            assertTrue(wb.getNumberOfSheets() > 0, "Debe tener al menos una hoja");
            Sheet sh = wb.getSheet("Test");
            assertNotNull(sh, "Debe tener hoja 'Test'");

            Row encabezados = sh.getRow(2);
            assertNotNull(encabezados, "Debe existir la fila de encabezados");
            assertEquals("A", encabezados.getCell(0).getStringCellValue());
            assertEquals("Promedio", encabezados.getCell(2).getStringCellValue());

            Row row = sh.getRow(3);
            assertNotNull(row, "Debe existir la primera fila de datos");
            assertEquals("Hola", row.getCell(0).getStringCellValue());
            assertEquals(42, (int) row.getCell(1).getNumericCellValue());

            Row total = sh.getRow(5);
            assertNotNull(total, "Debe existir la fila de totales");
            assertEquals("TOTAL", total.getCell(0).getStringCellValue());
            assertEquals(50.0, total.getCell(1).getNumericCellValue(), 0.001);
            assertEquals(CellType.BLANK, total.getCell(2).getCellType(),
                    "El promedio no se suma: no es un total con sentido");
        }
    }

    @Test
    void sinColumnasQueTotalicenNoAgregaFilaDeTotal() throws Exception {
        List<ReporteExcelGenerator.Seccion> secciones = List.of(
                new ReporteExcelGenerator.Seccion("Resumen",
                        List.of(ReporteExcelGenerator.Columna.texto("Concepto"),
                                ReporteExcelGenerator.Columna.dinero("Monto (S/)", false)),
                        List.of(new Object[]{"Ingresos", new java.math.BigDecimal("118.00")},
                                new Object[]{"Gastos", new java.math.BigDecimal("18.00")}),
                        null));

        byte[] excel = ReporteExcelGenerator.generar(secciones, "T", LocalDate.now(), LocalDate.now(), "ADMIN");

        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excel))) {
            Sheet sh = wb.getSheet("Resumen");
            assertNotNull(sh);
            assertNull(sh.getRow(5), "No debe inventarse un total");
        }
    }
}
