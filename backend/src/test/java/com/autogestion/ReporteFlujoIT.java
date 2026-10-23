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
import com.autogestion.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El caso que debe pasar: un vehículo de principio a fin deja huella
 * en clientes-por-día, ingresos e indicadores. Si algo del flujo se rompe,
 * los reportes mienten y este test lo grita.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ReporteFlujoIT {

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
    private ReporteService reporteService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void flujoCompletoAlimentaReportes() {
        Long adminId = usuarioRepository.findByEmail("admin@sanmartin.pe").orElseThrow().getId();
        Long mecId = usuarioRepository.findByEmail("mecanico1@sanmartin.pe").orElseThrow().getId();

        RecepcionCompletaRequest rec = new RecepcionCompletaRequest();
        rec.setClienteNombre("Cliente Reporte");
        rec.setClienteTipoDocumento("DNI");
        rec.setClienteDocumento("87654321");
        rec.setVehiculoPlaca("REP-001");
        rec.setVehiculoMarca("Toyota");
        rec.setProblemaReportado("Prueba de reporte de taller");
        var recepcion = recepcionService.crearCompleto(rec);

        DiagnosticoRequest diag = new DiagnosticoRequest();
        diag.setRecepcionId(recepcion.getId());
        diag.setMecanicoId(mecId);
        diag.setDescripcion("Cambio de aceite y revisión");
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

        ComprobanteEmitirRequest em = new ComprobanteEmitirRequest();
        em.setOrdenTrabajoId(ot.getId());
        em.setTipo("BOLETA");
        em.setTipoDoc("DNI");
        em.setNumDoc("87654321");
        em.setNombre("Cliente Reporte");
        em.setMetodoPago("EFECTIVO");
        em.setFormaPago("CONTADO");
        var comp = comprobanteService.emitir(em, adminId);
        assertEquals("B001", comp.getSerie());

        LocalDate hoy = LocalDate.now(ReporteService.ZONA);
        var dias = reporteService.clientesPorDia(hoy, hoy.plusDays(1));
        assertFalse(dias.isEmpty(), "El día de hoy debe aparecer");
        assertTrue(dias.get(0).clientesAtendidos() >= 1);
        assertTrue(dias.get(0).recepciones() >= 1);

        var ing = reporteService.obtenerIndicadores();
        assertEquals(1L, ing.getOtCompletadasMes());
        assertEquals(80.0, ing.getIngresosMes());

        var resumen = reporteService.comprobantesResumen(hoy, hoy.plusDays(1));
        assertEquals(1, resumen.size());
        assertEquals("BOLETA", resumen.get(0).tipo());
        assertEquals(0, new BigDecimal("80.00").compareTo(resumen.get(0).total()));

        var rend = reporteService.rendimientoMecanicos(hoy, hoy.plusDays(1));
        assertTrue(rend.stream().anyMatch(m -> m.finalizadas() >= 1));
    }
}
