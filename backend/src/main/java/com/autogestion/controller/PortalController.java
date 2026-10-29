package com.autogestion.controller;

import com.autogestion.config.EmpresaProperties;
import com.autogestion.config.PortalTokenService;
import com.autogestion.dto.PortalComprobanteDTO;
import com.autogestion.dto.PortalContactoRequest;
import com.autogestion.dto.PortalEncuestaDTO;
import com.autogestion.dto.PortalEncuestaPendienteDTO;
import com.autogestion.dto.PortalEncuestaRequest;
import com.autogestion.dto.PortalHistorialDTO;
import com.autogestion.dto.PortalIdentificarRequest;
import com.autogestion.dto.PortalInfoDTO;
import com.autogestion.dto.PortalRegistroRequest;
import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.dto.CotizacionResponseDTO;
import com.autogestion.service.ConsultaDocumentoRateLimit;
import com.autogestion.service.CotizacionService;
import com.autogestion.service.ConsultaDocumentoService;
import com.autogestion.dto.PortalResumenDTO;
import com.autogestion.dto.SolicitudCotizacionRequest;
import com.autogestion.dto.SolicitudCotizacionResponseDTO;
import com.autogestion.entity.Cliente;
import com.autogestion.service.PortalService;
import com.autogestion.service.SolicitudCotizacionService;
import com.autogestion.util.TicketPdf;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Portal del cliente.
 *  - /api/public/portal/**  → sin sesión (identificación). Devuelve un token de rol CLIENTE.
 *  - /api/portal/**         → solo con token CLIENTE (ver SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
public class PortalController {

    private static final String SIN_COINCIDENCIA = "No encontramos coincidencia con los datos ingresados";

    private final PortalService portalService;
    private final CotizacionService cotizacionService;
    private final SolicitudCotizacionService solicitudCotizacionService;
    private final PortalTokenService tokenService;
    private final ConsultaDocumentoService consultaDocumentoService;
    private final ConsultaDocumentoRateLimit consultaRateLimit;
    private final EmpresaProperties empresa;

    @PostMapping("/api/public/portal/identificar")
    public ResponseEntity<?> identificar(@RequestBody PortalIdentificarRequest req, HttpServletRequest http) {
        return portalService
                .identificarModoA(req.getDocumento(), req.getTipoDocumento(), req.getPlaca(), ip(http))
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(sesion(c)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", SIN_COINCIDENCIA)));
    }

    @PostMapping("/api/public/portal/registro")
    public ResponseEntity<?> registrar(@RequestBody PortalRegistroRequest req, HttpServletRequest http) {
        try {
            Cliente cliente = portalService.registrarCliente(req, ip(http));
            return ResponseEntity.ok(sesion(cliente));
        } catch (Exception e) {
            String mensaje = (e instanceof com.autogestion.exception.BusinessException be)
                    ? be.getMessage() : "No se pudo completar el registro. Verifica los datos e intenta de nuevo.";
            return ResponseEntity.status(
                    (e instanceof com.autogestion.exception.BusinessException be && be.getEstado() != null)
                            ? be.getEstado() : HttpStatus.BAD_REQUEST
            ).body(Map.of("mensaje", mensaje));
        }
    }

    /**
     * Consulta pública usada SOLO durante el registro. Devuelve el nombre cuando
     * hay una fuente configurada. Si no hay fuente, responde verificado=false con
     * motivo=SIN_FUENTE: el registro continúa en modo manual.
     *
     * Límite por IP para que no sirva como oráculo de enumeración de DNIs.
     */
    @GetMapping("/api/public/portal/documento/consultar")
    public ResponseEntity<?> consultarDocumento(
            @RequestParam String tipo,
            @RequestParam String numero,
            HttpServletRequest http) {
        if (!consultaRateLimit.permitir(ip(http))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "mensaje", "Demasiadas consultas de documento. Espera unos minutos e intenta de nuevo.",
                    "motivo", ConsultaDocumentoResponseDTO.MOTIVO_LIMITE));
        }
        try {
            return ResponseEntity.ok(consultaDocumentoService.consultar(tipo, numero));
        } catch (Exception e) {
            return ResponseEntity.ok(ConsultaDocumentoResponseDTO.vacio(ConsultaDocumentoResponseDTO.MOTIVO_ERROR));
        }
    }

    /** Datos públicos del taller para la guía y el soporte (sin datos de clientes). */
    @GetMapping("/api/public/portal/info")
    public PortalInfoDTO info() {
        return new PortalInfoDTO(
                empresa.getNombreComercial(),
                empresa.getRazonSocial(),
                empresa.getTelefono(),
                empresa.getEmail(),
                empresa.getWhatsapp(),
                empresa.getDireccion());
    }

    /** Resumen del cliente logueado. El id sale del token, nunca del request. */
    @GetMapping("/api/portal/me")
    public PortalResumenDTO me(Authentication auth) {
        return portalService.resumen(PortalTokenService.clienteIdDe(auth));
    }

    /** El cliente actualiza su celular y correo; nombre y documento no se tocan. */
    @PatchMapping("/api/portal/me/contacto")
    public PortalResumenDTO actualizarContacto(@RequestBody PortalContactoRequest req, Authentication auth) {
        return portalService.actualizarContacto(PortalTokenService.clienteIdDe(auth), req);
    }

    /** Baja de la cuenta: borra sus datos personales y cierra la sesión del portal. */
    @DeleteMapping("/api/portal/cuenta")
    public ResponseEntity<Void> eliminarCuenta(@Valid @RequestBody com.autogestion.dto.PortalBajaRequest req,
                                              Authentication auth) {
        portalService.eliminarCuenta(PortalTokenService.clienteIdDe(auth), req);
        return ResponseEntity.noContent().build();
    }

    /** Cotizaciones del cliente autenticado (solo lectura). */
    @GetMapping("/api/portal/cotizaciones")
    public List<CotizacionResponseDTO> cotizaciones(Authentication auth) {
        return cotizacionService.listarPorCliente(PortalTokenService.clienteIdDe(auth));
    }

    @GetMapping("/api/portal/cotizaciones/{id}")
    public CotizacionResponseDTO cotizacion(@PathVariable Long id, Authentication auth) {
        return cotizacionService.obtenerDeCliente(PortalTokenService.clienteIdDe(auth), id);
    }

    /** El cliente decide sobre una cotización PENDIENTE de su propia cuenta. */
    @PostMapping("/api/portal/cotizaciones/{id}/aprobar")
    public CotizacionResponseDTO aprobarCotizacion(@PathVariable Long id, Authentication auth) {
        return cotizacionService.aprobarDeCliente(PortalTokenService.clienteIdDe(auth), id);
    }

    @PostMapping("/api/portal/cotizaciones/{id}/rechazar")
    public CotizacionResponseDTO rechazarCotizacion(@PathVariable Long id, Authentication auth) {
        return cotizacionService.rechazarDeCliente(PortalTokenService.clienteIdDe(auth), id);
    }

    /** Comprobantes emitidos a nombre del cliente (solo los suyos). */
    @GetMapping("/api/portal/comprobantes")
    public List<PortalComprobanteDTO> comprobantes(Authentication auth) {
        return portalService.comprobantes(PortalTokenService.clienteIdDe(auth));
    }

    /** Mismo PDF que usa el personal, pero solo si el comprobante es del cliente del token. */
    @GetMapping("/api/portal/comprobantes/{id}/pdf")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long id, Authentication auth) {
        var c = portalService.comprobanteDeCliente(PortalTokenService.clienteIdDe(auth), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + c.getTipo() + "-" + c.getFolio() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(TicketPdf.generar(c));
    }

    /** Historial de visitas cerradas, con los servicios realizados. */
    @GetMapping("/api/portal/historial")
    public List<PortalHistorialDTO> historial(Authentication auth) {
        return portalService.historial(PortalTokenService.clienteIdDe(auth));
    }

    /** OTs entregadas que el cliente todavía puede calificar. */
    @GetMapping("/api/portal/encuestas/pendientes")
    public List<PortalEncuestaPendienteDTO> encuestasPendientes(Authentication auth) {
        return portalService.encuestasPendientes(PortalTokenService.clienteIdDe(auth));
    }

    @GetMapping("/api/portal/encuestas")
    public List<PortalEncuestaDTO> encuestas(Authentication auth) {
        return portalService.encuestasRespondidas(PortalTokenService.clienteIdDe(auth));
    }

    @PostMapping("/api/portal/encuestas")
    public ResponseEntity<PortalEncuestaDTO> responderEncuesta(
            @Valid @RequestBody PortalEncuestaRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.responderEncuesta(PortalTokenService.clienteIdDe(auth), req));
    }

    /** Solicitudes del cliente: avisan al taller que revise el vehículo y prepare la cotización. */
    @GetMapping("/api/portal/solicitudes-cotizacion")
    public List<SolicitudCotizacionResponseDTO> solicitudesCotizacion(Authentication auth) {
        return solicitudCotizacionService.listarDeCliente(PortalTokenService.clienteIdDe(auth));
    }

    @PostMapping("/api/portal/solicitudes-cotizacion")
    public ResponseEntity<SolicitudCotizacionResponseDTO> crearSolicitudCotizacion(
            @Valid @RequestBody SolicitudCotizacionRequest req, Authentication auth) {
        SolicitudCotizacionResponseDTO creada = solicitudCotizacionService.crear(
                PortalTokenService.clienteIdDe(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    private Map<String, Object> sesion(Cliente c) {
        return Map.of(
                "token", tokenService.generar(c.getId()),
                "nombre", c.nombreFiscal(),
                "expiraEnMin", tokenService.getMinutos());
    }

    /**
     * Detrás de Nginx, X-Real-IP lo escribe el proxy con la IP del socket y no
     * se puede falsificar desde el navegador. X-Forwarded-For se usa solo como
     * respaldo y se toma el ÚLTIMO valor: el primero lo puede enviar el cliente
     * y serviría para saltarse el límite de consultas.
     */
    private static String ip(HttpServletRequest req) {
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] partes = xff.split(",");
            return partes[partes.length - 1].trim();
        }
        return req.getRemoteAddr();
    }
}
