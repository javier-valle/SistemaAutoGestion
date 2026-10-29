package com.autogestion.controller;

import com.autogestion.dto.ComprobanteEmitirRequest;
import com.autogestion.dto.ComprobanteResponseDTO;
import com.autogestion.util.Rangos;
import com.autogestion.entity.EstadoComprobante;
import com.autogestion.entity.TipoComprobante;
import com.autogestion.service.ComprobanteService;
import com.autogestion.util.TicketPdf;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RolesAllowed({"ADMIN", "RECEPCIONISTA"})
@RestController
@RequestMapping("/api/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @PostMapping
    public ResponseEntity<ComprobanteResponseDTO> emitir(@Valid @RequestBody ComprobanteEmitirRequest request,
                                                         Authentication auth) {
        Long usuarioId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(comprobanteService.emitir(request, usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(comprobanteService.obtener(id));
    }

    @GetMapping
    public ResponseEntity<Page<ComprobanteResponseDTO>> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDateTime d = Rangos.inicio(desde);
        LocalDateTime h = Rangos.finDia(hasta);
        String tipoCode = (tipo != null && !tipo.isEmpty()) ? tipo : null;
        String estadoCode = (estado != null && !estado.isEmpty()) ? estado : null;
        return ResponseEntity.ok(comprobanteService.listar(tipoCode, estadoCode, d, h,
                (q == null || q.isBlank()) ? null : q.trim(), pageable));
    }

    /**
     * Representación impresa en PDF (A4): la dibuja el backend, el navegador
     * solo la descarga. Así la representación impresa no depende del JS cliente.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        ComprobanteResponseDTO c = comprobanteService.obtener(id);
        String nombre = c.getTipo() + "-" + c.getFolio() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(TicketPdf.generar(c));
    }

    @GetMapping("/previsualizar")
    public ResponseEntity<ComprobanteResponseDTO> previsualizar(@RequestParam Long ordenTrabajoId,
                                                                @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(comprobanteService.previsualizar(ordenTrabajoId, tipo));
    }

    @RolesAllowed("ADMIN")
    @PostMapping("/{id}/anular")
    public ResponseEntity<ComprobanteResponseDTO> anular(@PathVariable Long id,
                                                         @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(comprobanteService.anular(id, body.get("motivo")));
    }
}