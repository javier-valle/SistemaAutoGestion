package com.autogestion.controller;

import com.autogestion.dto.CotizacionCompletaRequest;
import com.autogestion.dto.CotizacionRequest;
import com.autogestion.dto.CotizacionResponseDTO;
import com.autogestion.entity.Cotizacion;
import com.autogestion.service.CotizacionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @PostMapping
    public ResponseEntity<CotizacionResponseDTO> crear(@Valid @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionService.crear(request));
    }

    @PostMapping("/completa")
    public ResponseEntity<CotizacionResponseDTO> crearCompleta(@Valid @RequestBody CotizacionCompletaRequest request) {
        return ResponseEntity.ok(cotizacionService.crearCompleta(request));
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<CotizacionResponseDTO> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.aprobar(id));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<CotizacionResponseDTO> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.rechazar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CotizacionResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<CotizacionResponseDTO>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(cotizacionService.listar(estado));
    }

    @GetMapping("/{id}/servicios")
    public ResponseEntity<List<CotizacionResponseDTO.CotizacionServicioDTO>> listarServicios(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.listarServicios(id));
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<CotizacionResponseDTO.CotizacionProductoDTO>> listarProductos(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.listarProductos(id));
    }
}
