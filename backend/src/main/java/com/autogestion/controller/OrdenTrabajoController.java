package com.autogestion.controller;

import com.autogestion.dto.OrdenTrabajoCompletaRequest;
import com.autogestion.dto.OrdenTrabajoRequest;
import com.autogestion.dto.OrdenTrabajoResponseDTO;
import com.autogestion.dto.OtProductoUsadoResponseDTO;
import com.autogestion.dto.ProductoUsadoRequest;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.service.OrdenTrabajoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/ordenes-trabajo")
@RequiredArgsConstructor
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenTrabajoService;

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PostMapping
    public ResponseEntity<OrdenTrabajoResponseDTO> crear(@Valid @RequestBody OrdenTrabajoRequest request) {
        OrdenTrabajo ot = ordenTrabajoService.crear(request);
        return ResponseEntity.ok(ordenTrabajoService.obtenerPorId(ot.getId()));
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PostMapping("/completa")
    public ResponseEntity<OrdenTrabajoResponseDTO> crearCompleta(@Valid @RequestBody OrdenTrabajoCompletaRequest request) {
        OrdenTrabajo ot = ordenTrabajoService.crearCompleta(request);
        return ResponseEntity.ok(ordenTrabajoService.obtenerPorId(ot.getId()));
    }

    @RolesAllowed({"ADMIN", "MECANICO"})
    @PutMapping("/{id}/estado")
    public ResponseEntity<OrdenTrabajoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrdenTrabajo ot = ordenTrabajoService.cambiarEstado(id, body.get("estado"));
        return ResponseEntity.ok(ordenTrabajoService.obtenerPorId(ot.getId()));
    }

    @RolesAllowed({"ADMIN", "MECANICO"})
    @PostMapping("/{id}/productos-usados")
    public ResponseEntity<OtProductoUsadoResponseDTO> registrarProductoUsado(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUsadoRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ordenTrabajoService.registrarProductoUsado(id, request, Long.valueOf(auth.getName())));
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PutMapping("/{id}/mecanico")
    public ResponseEntity<OrdenTrabajoResponseDTO> reasignarMecanico(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(ordenTrabajoService.reasignarMecanico(id, body.get("mecanicoId")));
    }

    @GetMapping
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ordenTrabajoService.listar(estado));
    }

    @GetMapping("/mecanico/{mecanicoId}")
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listarPorMecanico(@PathVariable Long mecanicoId) {
        return ResponseEntity.ok(ordenTrabajoService.listarPorMecanico(mecanicoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/productos-usados")
    public ResponseEntity<List<OtProductoUsadoResponseDTO>> listarProductosUsados(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.listarProductosUsados(id));
    }
}
