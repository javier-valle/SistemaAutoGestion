package com.autogestion.service;

import com.autogestion.dto.ProductoUsadoRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.OtProductoUsado;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Producto;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.Usuario;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.CotizacionRepository;
import com.autogestion.repository.InventarioMovimientoRepository;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.OtProductoUsadoRepository;
import com.autogestion.repository.PagoEntregaRepository;
import com.autogestion.repository.ProductoRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * El consumo de stock existe una sola vez y la reasignación respeta reglas.
 */
@ExtendWith(MockitoExtension.class)
class OrdenTrabajoServiceTest {

    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock private OtProductoUsadoRepository otProductoUsadoRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private PagoEntregaService pagoEntregaService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private InventarioMovimientoRepository inventarioMovimientoRepository;
    @Mock private RecepcionRepository recepcionRepository;
    @Mock private PagoEntregaRepository pagoEntregaRepository;
    @Mock private InventarioService inventarioService;

    @InjectMocks
    private OrdenTrabajoService ordenTrabajoService;

    private OrdenTrabajo otAbierta() {
        Cliente c = Cliente.builder().id(1L).nombre("Juan").build();
        Vehiculo v = Vehiculo.builder().id(2L).placa("ABC-123").cliente(c).build();
        Recepcion r = Recepcion.builder().id(3L).vehiculo(v).problemaReportado("Frenos").build();
        Diagnostico d = Diagnostico.builder().id(4L).recepcion(r).build();
        Cotizacion cot = Cotizacion.builder().id(5L).diagnostico(d).total(new BigDecimal("630.00")).build();
        Usuario mec = Usuario.builder().id(6L).nombre("Luis Ramírez").rol("MECANICO").activo(true).build();
        return OrdenTrabajo.builder().id(7L).cotizacion(cot).mecanico(mec).estado(com.autogestion.entity.EstadoOT.PENDIENTE).build();
    }

    @Test
    void consumoDelegaEnInventarioUnaSolaVez() {
        OrdenTrabajo ot = otAbierta();
        Producto p = Producto.builder().id(1L).nombre("Filtro").stockActual(10).stockMinimo(2).build();
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(ot));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(otProductoUsadoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ordenTrabajoService.registrarProductoUsado(7L, new ProductoUsadoRequest(1L, 3), 99L);


        verify(inventarioService, times(1)).registrarConsumoOT(7L, 1L, 3, 99L);
        verify(otProductoUsadoRepository, times(1)).save(any(OtProductoUsado.class));
        verify(inventarioMovimientoRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
    }

    @Test
    void consumoCantidadInvalidaSeRechaza() {
        OrdenTrabajo ot = otAbierta();
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(ot));

        assertThrows(BusinessException.class, () ->
                ordenTrabajoService.registrarProductoUsado(7L, new ProductoUsadoRequest(1L, 0), 99L));
        verify(inventarioService, never()).registrarConsumoOT(any(), any(), any(), any());
        verify(otProductoUsadoRepository, never()).save(any());
    }

    @Test
    void estadoInvalidoSeRechazaConMensajeClaro() {
        OrdenTrabajo ot = otAbierta();
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(ot));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ordenTrabajoService.cambiarEstado(7L, "VOLANDO"));
        assertEquals("estado", ex.getCampo());
        verify(ordenTrabajoRepository, never()).save(any());
    }

    @Test
    void reasignarOTCerradaSeBloquea() {
        OrdenTrabajo ot = otAbierta();
        ot.setEstado(com.autogestion.entity.EstadoOT.FINALIZADA);
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(ot));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ordenTrabajoService.reasignarMecanico(7L, 9L));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    void reasignarANoMecanicoSeBloquea() {
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(otAbierta()));
        Usuario otro = Usuario.builder().id(9L).nombre("Pedro").rol("ALMACENERO").activo(true).build();
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(otro));
        assertThrows(BusinessException.class,
                () -> ordenTrabajoService.reasignarMecanico(7L, 9L));
    }

    @Test
    void reasignarOkDevuelveNombreCompleto() {
        OrdenTrabajo ot = otAbierta();
        when(ordenTrabajoRepository.findById(7L)).thenReturn(Optional.of(ot));
        Usuario nuevo = Usuario.builder().id(9L).nombres("Jorge Enrique").apellidos("Castillo Vargas")
                .nombre("Jorge Enrique Castillo Vargas").rol("MECANICO").activo(true).build();
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(nuevo));
        when(ordenTrabajoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var dto = ordenTrabajoService.reasignarMecanico(7L, 9L);
        assertEquals("Jorge Enrique Castillo Vargas", dto.getMecanicoNombre());
        assertEquals("Frenos", dto.getProblemaReportado());
        assertEquals("ABC-123", dto.getVehiculoPlaca());
    }
}
