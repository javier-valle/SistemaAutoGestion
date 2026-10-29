package com.autogestion.controller;

import com.autogestion.service.PadronRucImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Solo ADMIN (ver SecurityConfig): carga y estado del padrón RUC local. */
@RestController
@RequestMapping("/api/admin/padron-ruc")
@RequiredArgsConstructor
public class PadronRucController {

    private final PadronRucImportService importService;

    @PostMapping("/importar")
    public ResponseEntity<Map<String, Object>> importar() {
        if (importService.iniciarAsync()) {
            return ResponseEntity.accepted().body(Map.of("mensaje", "Importación iniciada. Consulta /estado."));
        }
        return ResponseEntity.status(409).body(Map.of("mensaje", "Ya hay una importación en curso."));
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return importService.estado();
    }
}
