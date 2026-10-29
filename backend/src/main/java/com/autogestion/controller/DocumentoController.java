package com.autogestion.controller;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.service.ConsultaDocumentoService;
import com.autogestion.util.DocumentoValidator;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Validación y consulta de documentos para el PERSONAL autenticado.
 *
 *  - /validar    → solo formato/dígito verificador (no consulta fuentes externas).
 *  - /consultar  → intenta traer nombres/apellidos de una fuente configurada.
 *
 * El personal ya tiene sesión: aquí no se expone la consulta a Internet.
 */
@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final ConsultaDocumentoService consultaDocumentoService;

    @GetMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(@RequestParam String tipo,
                                                       @RequestParam String numero) {
        TipoDocumento td = TipoDocumento.desde(tipo);
        boolean valido = td != null && DocumentoValidator.valido(td, numero);
        String mensaje = valido ? tipo.toUpperCase() + " válido."
                : DocumentoValidator.mensajeError(td, numero);
        Map<String, Object> resp = new HashMap<>();
        resp.put("tipo", tipo.toUpperCase());
        resp.put("numero", DocumentoValidator.normalizar(numero));
        resp.put("valido", valido);
        resp.put("mensaje", mensaje);
        if (td == TipoDocumento.RUC && !valido) {
            int dv = DocumentoValidator.digitoVerificadorRuc(numero);
            if (dv >= 0) resp.put("digitoEsperado", dv);
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/consultar")
    public ConsultaDocumentoResponseDTO consultar(@RequestParam String tipo,
                                                  @RequestParam String numero) {
        return consultaDocumentoService.consultar(tipo, numero);
    }
}
