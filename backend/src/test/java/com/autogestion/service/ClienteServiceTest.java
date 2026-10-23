package com.autogestion.service;

import com.autogestion.dto.ClienteDetalleDTO;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Comprobante;
import com.autogestion.entity.EstadoComprobante;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.TipoComprobante;
import com.autogestion.entity.Vehiculo;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.ComprobanteRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * El panel de clientes muestra visitas reales y desactivar no borra nada.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private RecepcionRepository recepcionRepository;
    @Mock
    private ComprobanteRepository comprobanteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente() {
        return Cliente.builder().id(1L).nombre("Juan Pérez").documento("45123698")
                .telefono("951234567").email("juan@gmail.com").activo(true).build();
    }

    @Test
    void desactivarNoBorraSoloApaga() {
        Cliente c = cliente();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(clienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Cliente r = clienteService.cambiarActivo(1L, false);
        assertFalse(r.getActivo());
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    void resumenCuentaVisitasVehiculosYComprobantes() {
        Cliente c = cliente();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        Vehiculo v = Vehiculo.builder().id(5L).placa("ABC-123").marca("Toyota").cliente(c).build();
        when(vehiculoRepository.findByClienteId(1L)).thenReturn(List.of(v));
        Recepcion r1 = Recepcion.builder().id(11L).vehiculo(v).estado(com.autogestion.entity.EstadoRecepcion.FINALIZADA)
                .fechaIngreso(LocalDateTime.of(2026, 9, 1, 10, 0)).problemaReportado("Frenos").build();
        Recepcion r2 = Recepcion.builder().id(12L).vehiculo(v).estado(com.autogestion.entity.EstadoRecepcion.PENDIENTE)
                .fechaIngreso(LocalDateTime.of(2026, 9, 20, 10, 0)).problemaReportado("Aceite").build();
        when(recepcionRepository.porCliente(1L)).thenReturn(List.of(r2, r1));
        Comprobante comp = Comprobante.builder().id(100L).serie("B001").numero(1)
                .tipo(TipoComprobante.BOLETA).estado(EstadoComprobante.EMITIDO)
                .total(new BigDecimal("630.00")).fechaEmision(LocalDateTime.of(2026, 9, 21, 12, 0)).build();
        when(comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(1L)).thenReturn(List.of(comp));

        ClienteDetalleDTO d = clienteService.resumen(1L);

        assertEquals(2L, d.getVisitas());
        assertEquals(LocalDateTime.of(2026, 9, 20, 10, 0), d.getUltimaVisita());
        assertEquals(1, d.getVehiculos().size());
        assertEquals("ABC-123", d.getVehiculos().get(0).getPlaca());
        assertEquals(2, d.getRecepciones().size());
        assertEquals(1, d.getComprobantes().size());
        assertEquals("B001-00000001", d.getComprobantes().get(0).getFolio());
    }

    /** El género se guarda tipado y el resumen lo devuelve como texto para el front. */
    @Test
    void generoSeGuardaYVuelveEnElResumen() {
        com.autogestion.dto.ClienteRequest req = new com.autogestion.dto.ClienteRequest();
        req.setNombre("María López");
        req.setDocumento("40258741");
        req.setGenero("FEMENINO");
        when(clienteRepository.existsByTipoDocumentoAndDocumento(any(), any())).thenReturn(false);
        when(clienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Cliente creado = clienteService.crear(req);
        assertEquals(com.autogestion.entity.Genero.FEMENINO, creado.getGenero());

        Cliente c = cliente();
        c.setGenero(com.autogestion.entity.Genero.MASCULINO);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(vehiculoRepository.findByClienteId(1L)).thenReturn(List.of());
        when(recepcionRepository.porCliente(1L)).thenReturn(List.of());
        assertEquals("MASCULINO", clienteService.resumen(1L).getGenero());
    }

    /** Género vacío o desconocido = sin especificar (null), nunca un valor raro en BD. */
    @Test
    void generoVacioODesconocidoEsNull() {
        assertNull(com.autogestion.entity.Genero.desde(null));
        assertNull(com.autogestion.entity.Genero.desde("  "));
        assertNull(com.autogestion.entity.Genero.desde("NO_SEI"));
        assertEquals(com.autogestion.entity.Genero.MASCULINO, com.autogestion.entity.Genero.desde("masculino"));
        assertEquals(com.autogestion.entity.Genero.FEMENINO, com.autogestion.entity.Genero.desde(" Femenino "));
        assertEquals(com.autogestion.entity.Genero.OTROS, com.autogestion.entity.Genero.desde("OTROS"));
    }
}
