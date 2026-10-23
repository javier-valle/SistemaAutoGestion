package com.autogestion.controller;

import com.autogestion.dto.DiagnosticoRequest;
import com.autogestion.dto.DiagnosticoResponseDTO;
import com.autogestion.service.DiagnosticoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/diagnosticos")
@RequiredArgsConstructor
public class DiagnosticoController {

    private final DiagnosticoService diagnosticoService;

    @PostMapping
    public ResponseEntity<DiagnosticoResponseDTO> crear(@Valid @RequestBody DiagnosticoRequest request) {
        return ResponseEntity.ok(diagnosticoService.crear(request));
    }

    @GetMapping("/recepcion/{recepcionId}")
    public ResponseEntity<List<DiagnosticoResponseDTO>> listarPorRecepcion(@PathVariable Long recepcionId) {
        return ResponseEntity.ok(diagnosticoService.listarPorRecepcion(recepcionId));
    }
}
