package com.autogestion.service;

import com.autogestion.dto.PagoRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Comprobante;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.EstadoOT;
import com.autogestion.entity.EstadoRecepcion;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.PagoEntrega;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.PagoEntregaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Un solo camino: solo se cobra una OT FINALIZADA y la entrega cierra la
 * orden (fechaFin) y la recepción, sin depender de por qué endpoint se llame.
 */
@ExtendWith(MockitoExtension.class)
class PagoEntregaServiceTest {

    @Mock private PagoEntregaRepository pagoEntregaRepository;
    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;

    @InjectMocks
    private PagoEntregaService pagoEntregaService;

    private OrdenTrabajo ot(EstadoOT estado) {
        Cliente c = Cliente.builder().id(1L).nombre("Juan").build();
        Vehiculo v = Vehiculo.builder().id(2L).placa("ABC-123").cliente(c).build();
        Recepcion r = Recepcion.builder().id(3L).vehiculo(v)
                .estado(EstadoRecepcion.EN_TRABAJO).problemaReportado("Frenos").build();
        Diagnostico d = Diagnostico.builder().id(4L).recepcion(r).build();
        Cotizacion cot = Cotizacion.builder().id(5L).diagnostico(d).total(new BigDecimal("630.00")).build();
        return OrdenTrabajo.builder().id(7L).cotizacion(cot).estado(estado).build();
    }

    private PagoEntrega pago(OrdenTrabajo ot, LocalDateTime fechaPago) {
        return PagoEntrega.builder().id(9L).ordenTrabajo(ot).monto(new BigDecimal("630.00"))
                .fechaPago(fechaPago).build();
    }

    @Test
    void pagoExigeOTFinalizada() {
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot(EstadoOT.EN_PRUEBA)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoEntregaService.registrarPago(new PagoRequest(7L, new BigDecimal("630.00"))));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(pagoEntregaRepository, never()).save(any());
    }

    @Test
    void pagoEnOTFinalizadaGuardaMontoYFecha() {
        OrdenTrabajo ot = ot(EstadoOT.FINALIZADA);
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot));
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L)).thenReturn(Optional.empty());
        when(pagoEntregaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = pagoEntregaService.registrarPago(new PagoRequest(7L, new BigDecimal("630.00")));

        assertEquals(new BigDecimal("630.00"), dto.getMonto());
        assertNotNull(dto.getFechaPago());
        verify(pagoEntregaRepository).save(any(PagoEntrega.class));
    }

    @Test
    void pagoDuplicadoSeRechaza() {
        OrdenTrabajo ot = ot(EstadoOT.FINALIZADA);
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot));
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L))
                .thenReturn(Optional.of(pago(ot, LocalDateTime.now())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoEntregaService.registrarPago(new PagoRequest(7L, new BigDecimal("630.00"))));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    void entregaSinPagoSeRechaza() {
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot(EstadoOT.FINALIZADA)));
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoEntregaService.registrarEntrega(7L));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    void entregaExigeOTFinalizada() {
        OrdenTrabajo ot = ot(EstadoOT.EN_PROCESO);
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoEntregaService.registrarEntrega(7L));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        assertEquals(EstadoRecepcion.EN_TRABAJO,
                ot.getCotizacion().getDiagnostico().getRecepcion().getEstado());
    }

    @Test
    void desvincularComprobanteDejaElPagoSinDocumentoAnulado() {
        OrdenTrabajo ot = ot(EstadoOT.FINALIZADA);
        Comprobante comp = Comprobante.builder().id(9L).ordenTrabajo(ot).build();
        PagoEntrega pago = pago(ot, LocalDateTime.now());
        pago.setComprobante(comp);
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L)).thenReturn(Optional.of(pago));
        when(pagoEntregaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        pagoEntregaService.desvincularComprobante(comp);

        assertNull(pago.getComprobante());
        verify(pagoEntregaRepository).save(pago);
    }

    @Test
    void desvincularNoTocaElPagoDeOtroComprobante() {
        OrdenTrabajo ot = ot(EstadoOT.FINALIZADA);
        Comprobante actual = Comprobante.builder().id(20L).ordenTrabajo(ot).build();
        Comprobante anulado = Comprobante.builder().id(21L).ordenTrabajo(ot).build();
        PagoEntrega pago = pago(ot, LocalDateTime.now());
        pago.setComprobante(actual);
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L)).thenReturn(Optional.of(pago));

        pagoEntregaService.desvincularComprobante(anulado);

        assertEquals(20L, pago.getComprobante().getId());
        verify(pagoEntregaRepository, never()).save(any());
    }

    @Test
    void entregaCierraOTYRecepcion() {
        OrdenTrabajo ot = ot(EstadoOT.FINALIZADA);
        PagoEntrega pago = pago(ot, LocalDateTime.now().minusHours(1));
        when(ordenTrabajoRepository.bloquearPorId(7L)).thenReturn(Optional.of(ot));
        when(pagoEntregaRepository.findByOrdenTrabajoId(7L)).thenReturn(Optional.of(pago));
        when(pagoEntregaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = pagoEntregaService.registrarEntrega(7L);

        assertNotNull(dto.getFechaEntrega());
        assertEquals(EstadoRecepcion.ENTREGADA, ot.getCotizacion().getDiagnostico().getRecepcion().getEstado());
        assertEquals(EstadoOT.FINALIZADA, ot.getEstado());
        assertNotNull(ot.getFechaFin());
    }
}
