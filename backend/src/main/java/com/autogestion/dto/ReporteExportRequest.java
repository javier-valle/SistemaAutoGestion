package com.autogestion.dto;

import java.util.List;

public record ReporteExportRequest(
        List<String> reportes,
        String formato,
        java.time.LocalDate desde,
        java.time.LocalDate hasta
) { }
