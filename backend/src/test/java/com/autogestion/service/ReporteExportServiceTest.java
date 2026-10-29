package com.autogestion.service;

import com.autogestion.dto.ReporteTablaRequest;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReporteExportServiceTest {

    @Mock
    private ReporteService reporteService;

    @InjectMocks
    private ReporteExportService exportService;

    @Test
    void excelSaleConEncabezadoFormatoYTotal() throws Exception {
        byte[] archivo = exportService.exportarTabla(tabla(), "excel",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 29), "ADMIN");

        assertTrue(archivo.length > 1000, "El libro no debería estar vacío");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            var hoja = wb.getSheetAt(0);
            assertEquals("Rendimiento de mecánicos", hoja.getSheetName());

            Row encabezado = hoja.getRow(2);
            assertEquals("Mecánico", encabezado.getCell(0).getStringCellValue());
            assertEquals("Ingresos (S/)", encabezado.getCell(2).getStringCellValue());

            Row primera = hoja.getRow(3);
            assertEquals("Ana Torres", primera.getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, primera.getCell(1).getCellType());
            assertTrue(primera.getCell(2).getNumericCellValue() > 120.0, "El total debe ser numérico");

            Row segunda = hoja.getRow(4);
            assertEquals(1234.50, segunda.getCell(2).getNumericCellValue(), 0.01,
                    "Un importe con 'S/' y separadores debe seguir siendo número");

            Row totales = hoja.getRow(5);
            assertEquals("TOTAL", totales.getCell(0).getStringCellValue());
            assertFalse(totales.getCell(1).getCellStyle() == null);
        }
    }

    @Test
    void elEncabezadoCierraEnElDiaPedidoSinCorrerElRango() throws Exception {
        byte[] archivo = exportService.exportarTabla(tabla(), "excel",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 29), "ADMIN");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            String subtitulo = wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue();
            assertTrue(subtitulo.startsWith("Del 01/10/2026 al 29/10/2026"),
                    "El rango no debe expandirse dos veces: " + subtitulo);
        }
    }

    @Test
    void pdfSaleEnA4ConLosDatos() {
        byte[] archivo = exportService.exportarTabla(tabla(), "pdf",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 29), "ADMIN");

        String firma = new String(archivo, 0, Math.min(20, archivo.length), StandardCharsets.ISO_8859_1);
        assertTrue(firma.startsWith("%PDF-1."), "Debe ser un PDF válido");
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(archivo)) {
            String texto = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
            assertTrue(texto.contains("Rendimiento de mecánicos"));
            assertTrue(texto.contains("Ana Torres"));
        } catch (Exception e) {
            fail("El PDF generado no se pudo leer: " + e.getMessage());
        }
    }

    private static ReporteTablaRequest tabla() {
        ReporteTablaRequest req = new ReporteTablaRequest();
        req.setTitulo("Rendimiento de mecánicos");
        req.setNota("Datos del rango seleccionado.");

        ReporteTablaRequest.Columna nombre = new ReporteTablaRequest.Columna();
        nombre.setTitulo("Mecánico");
        nombre.setTipo("TEXTO");

        ReporteTablaRequest.Columna ordenes = new ReporteTablaRequest.Columna();
        ordenes.setTitulo("Finalizadas");
        ordenes.setTipo("ENTERO");
        ordenes.setTotaliza(true);

        ReporteTablaRequest.Columna ingresos = new ReporteTablaRequest.Columna();
        ingresos.setTitulo("Ingresos (S/)");
        ingresos.setTipo("DINERO");
        ingresos.setTotaliza(true);

        req.setColumnas(List.of(nombre, ordenes, ingresos));
        req.setFilas(List.of(
                List.of("Ana Torres", 3, new BigDecimal("150.50")),
                List.of("Luis Pérez", 2, "S/ 1,234.50")));
        return req;
    }
}
