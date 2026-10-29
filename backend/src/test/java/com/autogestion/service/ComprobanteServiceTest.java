package com.autogestion.service;

import com.autogestion.entity.Cliente;
import com.autogestion.entity.Comprobante;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.EstadoComprobante;
import com.autogestion.entity.FormaPago;
import com.autogestion.entity.MetodoPago;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.TipoComprobante;
import com.autogestion.entity.Usuario;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ComprobanteRepository;
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
 * Anular conserva el comprobante como historial (estado + motivo) y suelta el
 * pago, para que no quede amarrado a un documento sin validez.
 */
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock private ComprobanteRepository comprobanteRepository;
    @Mock private PagoEntregaService pagoEntregaService;

    @InjectMocks
    private ComprobanteService comprobanteService;

    private Comprobante comprobante() {
        Cliente c = Cliente.builder().id(1L).nombre("Juan").build();
        Vehiculo v = Vehiculo.builder().id(2L).placa("ABC-123").cliente(c).build();
        Recepcion r = Recepcion.builder().id(3L).vehiculo(v).build();
        Diagnostico d = Diagnostico.builder().id(4L).recepcion(r).build();
        Cotizacion cot = Cotizacion.builder().id(5L).diagnostico(d).total(new BigDecimal("118.00")).build();
        OrdenTrabajo ot = OrdenTrabajo.builder().id(7L).cotizacion(cot).build();
        Usuario emisor = Usuario.builder().id(8L).nombre("Admin").build();
        return Comprobante.builder()
                .id(9L)
                .ordenTrabajo(ot)
                .tipo(TipoComprobante.BOLETA)
                .codigoSunat("03")
                .serie("B001")
                .numero(1)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .formaPago(FormaPago.CONTADO)
                .metodoPago(MetodoPago.EFECTIVO)
                .emisorRuc("20123456786")
                .emisorRazonSocial("TALLER SAN MARTIN S.A.C.")
                .emisorDireccion("Av. Universitaria 123")
                .clienteTipoDoc("DNI")
                .clienteNumDoc("45123698")
                .clienteNombre("Juan Pérez")
                .totalGravado(new BigDecimal("100.00"))
                .igv(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .totalLetras("SON: CIENTO DIECIOCHO Y 00/100 SOLES")
                .estado(EstadoComprobante.EMITIDO)
                .emitidoPor(emisor)
                .build();
    }

    @Test
    void anularDesvinculaElPagoYConservaMotivo() {
        Comprobante c = comprobante();
        when(comprobanteRepository.findById(9L)).thenReturn(Optional.of(c));
        when(comprobanteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = comprobanteService.anular(9L, "  Duplicado  ");

        assertEquals("ANULADO", dto.getEstado());
        assertEquals("Duplicado", dto.getMotivoAnulacion());
        verify(pagoEntregaService).desvincularComprobante(c);
    }

    @Test
    void anularExigeMotivoYNoTocaElPago() {
        when(comprobanteRepository.findById(9L)).thenReturn(Optional.of(comprobante()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> comprobanteService.anular(9L, "   "));

        assertEquals("motivo", ex.getCampo());
        verify(pagoEntregaService, never()).desvincularComprobante(any());
        verify(comprobanteRepository, never()).save(any());
    }

    @Test
    void anularDosVecesSeRechaza() {
        Comprobante c = comprobante();
        c.setEstado(EstadoComprobante.ANULADO);
        when(comprobanteRepository.findById(9L)).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> comprobanteService.anular(9L, "Duplicado"));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(pagoEntregaService, never()).desvincularComprobante(any());
    }
}
