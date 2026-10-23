package com.autogestion.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReporteExcelGeneratorTest {
    @Test
    void excelReleerTiposYFormulas() throws Exception {
        List<ReporteExcelGenerator.Seccion> secciones = new ArrayList<>();
        secciones.add(new ReporteExcelGenerator.Seccion("Test", new String[]{"A", "B"},
                List.of(new Object[]{"Hola", 42}, new Object[]{"Mundo", new BigDecimal("99.99")}), null));
        byte[] excel = ReporteExcelGenerator.generar(secciones, "T", LocalDate.now(), LocalDate.now(), "ADMIN");
        assertNotNull(excel);
        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excel))) {
            assertTrue(wb.getNumberOfSheets() > 0, "Debe tener al menos una hoja");
            Sheet sh = wb.getSheet("Test");
            assertNotNull(sh, "Debe tener hoja 'Test'");
            Row row = sh.getRow(1); // primera fila de datos
            assertNotNull(row);
            assertTrue(row.getCell(1) instanceof Cell);
        }
    }
}
