package com.autogestion.controller;

import com.autogestion.dto.PortalIdentificarRequest;
import com.autogestion.dto.PortalOtpSolicitarRequest;
import com.autogestion.dto.PortalOtpVerificarRequest;
import com.autogestion.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    @PostMapping("/identificar")
    public ResponseEntity<?> identificar(@RequestBody PortalIdentificarRequest req) {
        boolean ok = portalService.validarIdentificacionModoA(req.getDocumento(), req.getTipoDocumento(), req.getPlaca());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.OK).body("No encontramos coincidencia con los datos ingresados");
        }
        return ResponseEntity.ok("Identificado (modo A). Token de portal pendiente de implementación.");
    }

    @PostMapping("/otp/solicitar")
    public ResponseEntity<?> solicitarOtp(@RequestBody PortalOtpSolicitarRequest req) {
        portalService.solicitarOtp(req.getDocumento(), req.getTipoDocumento());
        return ResponseEntity.status(HttpStatus.OK).body("Si hay coincidencia, recibirás el código.");
    }

    @PostMapping("/otp/verificar")
    public ResponseEntity<?> verificarOtp(@RequestBody PortalOtpVerificarRequest req) {
        boolean ok = portalService.verificarOtp(req.getDocumento(), req.getTipoDocumento(), req.getCodigo());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.OK).body("No encontramos coincidencia con los datos ingresados");
        }
        return ResponseEntity.ok("Verificado (modo B). Token de portal pendiente de implementación.");
    }
}
