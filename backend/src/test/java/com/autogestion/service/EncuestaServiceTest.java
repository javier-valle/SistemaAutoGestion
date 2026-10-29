package com.autogestion.service;

import com.autogestion.entity.Cliente;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.EncuestaSatisfaccion;
import com.autogestion.entity.EstadoCotizacion;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.entity.Vehiculo;
import com.autogestion.repository.EncuestaSatisfaccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncuestaServiceTest {

    @Mock
    private EncuestaSatisfaccionRepository encuestaSatisfaccionRepository;

    @InjectMocks
    private EncuestaService encuestaService;

    @Test
    void listaLosComentariosConClienteOrdenYVehiculo() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Ana Torres")
                .tipoDocumento(TipoDocumento.DNI).documento("45123698").build();
        Vehiculo vehiculo = Vehiculo.builder().id(2L).placa("ABC-123")
                .marca("Kia").modelo("Rio").anio(2020).cliente(cliente).build();
        Recepcion recepcion = Recepcion.builder().id(3L).vehiculo(vehiculo)
                .fechaIngreso(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
        Diagnostico diagnostico = Diagnostico.builder().id(4L).recepcion(recepcion).build();
        Cotizacion cotizacion = Cotizacion.builder().id(5L).diagnostico(diagnostico)
                .total(new BigDecimal("120.00")).estado(EstadoCotizacion.APROBADA).build();
        OrdenTrabajo orden = OrdenTrabajo.builder().id(6L).cotizacion(cotizacion).build();
        EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder().id(7L)
                .cliente(cliente).ordenTrabajo(orden).puntuacion(5)
                .comentario("  Muy buena atención  ")
                .creadoEn(LocalDateTime.of(2026, 10, 29, 18, 0)).build();

        when(encuestaSatisfaccionRepository.findAllByOrderByCreadoEnDesc())
                .thenReturn(List.of(encuesta));

        var lista = encuestaService.listar();

        assertEquals(1, lista.size());
        var dto = lista.get(0);
        assertEquals("Ana Torres", dto.cliente());
        assertEquals("45123698", dto.documento());
        assertEquals(6L, dto.ordenTrabajoId());
        assertEquals("ABC-123", dto.placa());
        assertEquals("Kia Rio 2020", dto.vehiculo());
        assertEquals(5, dto.puntuacion());
        assertEquals("  Muy buena atención  ", dto.comentario());
    }

    @Test
    void unaEncuestaSinOrdenNoRompeElListado() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Ana Torres").build();
        EncuestaSatisfaccion encuesta = EncuestaSatisfaccion.builder().id(8L)
                .cliente(cliente).puntuacion(4).comentario("Bien").build();
        when(encuestaSatisfaccionRepository.findAllByOrderByCreadoEnDesc())
                .thenReturn(List.of(encuesta));

        var dto = encuestaService.listar().get(0);

        assertNull(dto.ordenTrabajoId());
        assertNull(dto.placa());
        assertEquals("Bien", dto.comentario());
    }
}
