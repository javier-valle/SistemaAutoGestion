package com.autogestion.controller;

import com.autogestion.dto.SolicitudCotizacionNotaRequest;
import com.autogestion.dto.SolicitudCotizacionResponseDTO;
import com.autogestion.service.SolicitudCotizacionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Bandeja del personal para las solicitudes que crean los clientes desde el portal. */
@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/solicitudes-cotizacion")
@RequiredArgsConstructor
public class SolicitudCotizacionController {

    private final SolicitudCotizacionService solicitudService;

    @GetMapping
    public ResponseEntity<List<SolicitudCotizacionResponseDTO>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(solicitudService.listar(estado));
    }

    @PutMapping("/{id}/atender")
    public ResponseEntity<SolicitudCotizacionResponseDTO> atender(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SolicitudCotizacionNotaRequest body) {
        return ResponseEntity.ok(solicitudService.atender(id, body == null ? null : body.getNota()));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudCotizacionResponseDTO> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SolicitudCotizacionNotaRequest body) {
        return ResponseEntity.ok(solicitudService.rechazar(id, body == null ? null : body.getNota()));
    }
}
