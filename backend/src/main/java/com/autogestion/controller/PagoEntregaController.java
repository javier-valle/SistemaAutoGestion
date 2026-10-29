package com.autogestion.controller;

import com.autogestion.dto.OrdenTrabajoFinalizadaDTO;
import com.autogestion.dto.PagoEntregaCompletaRequest;
import com.autogestion.dto.PagoEntregaResponseDTO;
import com.autogestion.dto.PagoRequest;
import com.autogestion.service.OrdenTrabajoService;
import com.autogestion.service.PagoEntregaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@RolesAllowed({"ADMIN", "RECEPCIONISTA"})
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PagoEntregaController {

    private final PagoEntregaService pagoEntregaService;
    private final OrdenTrabajoService ordenTrabajoService;

    @GetMapping("/ordenes-trabajo/{id}/monto")
    public ResponseEntity<Map<String, BigDecimal>> obtenerMonto(@PathVariable Long id) {
        BigDecimal monto = pagoEntregaService.obtenerMonto(id);
        return ResponseEntity.ok(Map.of("monto", monto));
    }

    /** @deprecated Desde Fase 1 se emite con POST /api/comprobantes (folio, IGV y snapshot). */
    @Deprecated
    @PostMapping("/pagos")
    public ResponseEntity<PagoEntregaResponseDTO> registrarPago(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(pagoEntregaService.registrarPago(request));
    }

    @PostMapping("/entregas/{ordenTrabajoId}")
    public ResponseEntity<PagoEntregaResponseDTO> registrarEntrega(@PathVariable Long ordenTrabajoId) {
        return ResponseEntity.ok(pagoEntregaService.registrarEntrega(ordenTrabajoId));
    }

    @GetMapping("/ordenes-trabajo/{id}/pago")
    public ResponseEntity<?> obtenerPago(@PathVariable Long id) {
        Optional<PagoEntregaResponseDTO> pago = pagoEntregaService.obtenerPorOT(id);
        return pago.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @GetMapping("/ordenes-trabajo/finalizadas-completa")
    public ResponseEntity<List<OrdenTrabajoFinalizadaDTO>> listarFinalizadasCompleta() {
        return ResponseEntity.ok(ordenTrabajoService.listarFinalizadasConPago());
    }

    
    /** @deprecated Desde Fase 1 se emite con POST /api/comprobantes (folio, IGV y snapshot). */
    @Deprecated
    @PostMapping("/pago-entrega/completa")
    public ResponseEntity<PagoEntregaResponseDTO> registrarPagoEntregaCompleta(@Valid @RequestBody PagoEntregaCompletaRequest request) {
        return ResponseEntity.ok(ordenTrabajoService.registrarPagoEntregaCompleto(request));
    }
}
