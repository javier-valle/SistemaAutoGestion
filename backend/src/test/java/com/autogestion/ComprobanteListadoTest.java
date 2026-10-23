package com.autogestion;

import com.autogestion.dto.ComprobanteEmitirRequest;
import com.autogestion.dto.CotizacionCompletaRequest;
import com.autogestion.dto.DiagnosticoRequest;
import com.autogestion.dto.OrdenTrabajoRequest;
import com.autogestion.dto.RecepcionCompletaRequest;
import com.autogestion.repository.UsuarioRepository;
import com.autogestion.service.ComprobanteService;
import com.autogestion.service.CotizacionService;
import com.autogestion.service.DiagnosticoService;
import com.autogestion.service.OrdenTrabajoService;
import com.autogestion.service.RecepcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dos errores que solo aparecían en producción (H2 no los reprodujo antes):
 *
 * 1. filtrar() es SQL nativo: Spring Data mete el Sort en el ORDER BY sin
 *    traducir, así que "sort=fechaEmision,desc" terminaba en
 *    "ORDER BY c.fechaEmision" y PostgreSQL respondía
 *    'column c.fechaemision does not exist'.
 * 2. El front enviaba tipoDoc = "BOLETA" (el tipo del comprobante, no del
 *    documento) y el backend rechazaba con un mensaje que se contradecía:
 *    "El DNI debe tener 8 dígitos numéricos (llevas 8)".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ComprobanteListadoTest {

    @Autowired
    private RecepcionService recepcionService;
    @Autowired
    private DiagnosticoService diagnosticoService;
    @Autowired
    private CotizacionService cotizacionService;
    @Autowired
    private OrdenTrabajoService ordenTrabajoService;
    @Autowired
    private ComprobanteService comprobanteService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void listarOrdenadoPorFechaEmisionNoFalla() {
        Long adminId = usuarioRepository.findByEmail("admin@sanmartin.pe").orElseThrow().getId();
        Long otId = flujoHastaFinalizar(adminId, "LIST-001");

        emitir(otId, "DNI", "87654321");

        // El mismo sort que manda el front (?sort=fechaEmision,desc)
        var page = comprobanteService.listar(null, null, desde(), hasta(), null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaEmision")));
        assertFalse(page.isEmpty(), "Debe listar el comprobante emitido");

        // Y también sin sort: el orden por defecto tiene que ser determinista
        var sinOrden = comprobanteService.listar(null, null, desde(), hasta(), null,
                PageRequest.of(0, 20));
        assertFalse(sinOrden.isEmpty());
    }

    @Test
    void boletaConTipoDocDesconocidoSeDeduceDelNumero() {
        Long adminId = usuarioRepository.findByEmail("admin@sanmartin.pe").orElseThrow().getId();
        Long otId = flujoHastaFinalizar(adminId, "LIST-002");

        // Así lo enviaba el front: el tipo del COMPROBANTE, no el del documento
        var comp = emitir(otId, "BOLETA", "87654321");
        assertEquals("DNI", comp.clienteTipoDoc());
        assertEquals("87654321", comp.clienteNumDoc());
    }

    @Test
    void boletaConRucDiceQueCorrespondeFactura() {
        Long adminId = usuarioRepository.findByEmail("admin@sanmartin.pe").orElseThrow().getId();
        Long otId = flujoHastaFinalizar(adminId, "LIST-003");

        var e = assertThrows(com.autogestion.exception.BusinessException.class,
                () -> emitir(otId, "BOLETA", "20123456786"));
        assertTrue(e.getMessage().contains("factura"), e.getMessage());
    }

    /* ---------------- helpers ---------------- */

    private record Comp(String clienteTipoDoc, String clienteNumDoc) { }

    private Comp emitir(Long otId, String tipoDoc, String numDoc) {
        ComprobanteEmitirRequest req = new ComprobanteEmitirRequest();
        req.setOrdenTrabajoId(otId);
        req.setTipo("BOLETA");
        req.setTipoDoc(tipoDoc);
        req.setNumDoc(numDoc);
        req.setNombre("Cliente Listado");
        req.setMetodoPago("EFECTIVO");
        req.setFormaPago("CONTADO");
        var dto = comprobanteService.emitir(req,
                usuarioRepository.findByEmail("admin@sanmartin.pe").orElseThrow().getId());
        return new Comp(dto.getClienteTipoDoc(), dto.getClienteNumDoc());
    }

    /** Recepción -> diagnóstico -> cotización aprobada -> OT FINALIZADA. */
    private Long flujoHastaFinalizar(Long adminId, String placa) {
        Long mecId = usuarioRepository.findByEmail("mecanico1@sanmartin.pe").orElseThrow().getId();

        RecepcionCompletaRequest rec = new RecepcionCompletaRequest();
        rec.setClienteNombre("Cliente Listado");
        rec.setClienteTipoDocumento("DNI");
        rec.setClienteDocumento("87654321");
        rec.setVehiculoPlaca(placa);
        rec.setVehiculoMarca("Toyota");
        rec.setProblemaReportado("Prueba de listado de comprobantes");
        var recepcion = recepcionService.crearCompleto(rec);

        DiagnosticoRequest diag = new DiagnosticoRequest();
        diag.setRecepcionId(recepcion.getId());
        diag.setMecanicoId(mecId);
        diag.setDescripcion("Cambio de aceite");
        var d = diagnosticoService.crear(diag);

        CotizacionCompletaRequest cot = new CotizacionCompletaRequest();
        cot.setDiagnosticoId(d.getId());
        cot.setServicios(List.of(new CotizacionCompletaRequest.ServicioItem(1L, new java.math.BigDecimal("80.00"))));
        var c = cotizacionService.crearCompleta(cot);
        cotizacionService.aprobar(c.getId());

        OrdenTrabajoRequest otReq = new OrdenTrabajoRequest();
        otReq.setCotizacionId(c.getId());
        otReq.setMecanicoId(mecId);
        var ot = ordenTrabajoService.crear(otReq);
        ordenTrabajoService.cambiarEstado(ot.getId(), "EN_PROCESO");
        ordenTrabajoService.cambiarEstado(ot.getId(), "EN_PRUEBA");
        ordenTrabajoService.cambiarEstado(ot.getId(), "FINALIZADA");
        return ot.getId();
    }

    private LocalDateTime desde() { return LocalDate.of(2000, 1, 1).atStartOfDay(); }
    private LocalDateTime hasta() { return LocalDateTime.now().plusDays(1); }
}
