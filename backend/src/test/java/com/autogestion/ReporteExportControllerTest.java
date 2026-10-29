package com.autogestion;

import com.autogestion.controller.ReporteController;
import com.autogestion.service.ReporteExportService;
import com.autogestion.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * El navegador manda la tabla visible (títulos, tipos y valores) y recibe el
 * archivo ya formateado. Esta prueba fija ese contrato para que un cambio de
 * nombres en el DTO no rompa la pantalla de reportes en silencio.
 */
class ReporteExportControllerTest {

    private ReporteExportService exportService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        ReporteService reporteService = mock(ReporteService.class);
        exportService = mock(ReporteExportService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReporteController(reporteService, exportService)).build();
    }

    @Test
    void descargaExcelConNombreSeguroYContenidoXlsx() throws Exception {
        when(exportService.exportarTabla(any(), eq("excel"), any(LocalDate.class), any(LocalDate.class), any()))
                .thenReturn(new byte[]{1, 2, 3});

        String json = """
                {
                  "titulo": "Rendimiento de mecánicos / Octubre",
                  "nota": "Datos del rango seleccionado.",
                  "columnas": [
                    {"titulo": "Mecánico", "tipo": "TEXTO"},
                    {"titulo": "Ingresos (S/)", "tipo": "DINERO", "totaliza": true}
                  ],
                  "filas": [["Ana Torres", 150.50]]
                }
                """;

        mvc.perform(post("/api/reportes/export/tabla")
                        .param("formato", "excel")
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-29")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"autogestion-rendimiento-de-mecanicos-octubre.xlsx\""))
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void rechazaUnFormatoQueNoEsExcelNiPdf() throws Exception {
        mvc.perform(post("/api/reportes/export/tabla")
                        .param("formato", "csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"X","columnas":[{"titulo":"A"}],"filas":[["a"]]}
                                """))
                .andExpect(status().isBadRequest());
    }
}
