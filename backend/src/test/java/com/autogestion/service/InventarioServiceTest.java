package com.autogestion.service;

import com.autogestion.dto.MovimientoInventarioRequest;
import com.autogestion.entity.InventarioMovimiento;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Producto;
import com.autogestion.entity.Proveedor;
import com.autogestion.entity.TipoMovimiento;
import com.autogestion.entity.Usuario;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.InventarioMovimientoRepository;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.ProductoRepository;
import com.autogestion.repository.ProveedorRepository;
import com.autogestion.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cada movimiento deja rastro: stock antes/después, costo, quién y por qué.
 */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private InventarioMovimientoRepository inventarioMovimientoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private InventarioService inventarioService;

    private Producto producto() {
        return Producto.builder().id(1L).nombre("Filtro").tipo("REPUESTO")
                .precioUnitario(new BigDecimal("25.00")).costoUnitario(new BigDecimal("15.00"))
                .stockActual(10).stockMinimo(2).build();
    }

    private MovimientoInventarioRequest req(String tipo, int cantidad) {
        MovimientoInventarioRequest r = new MovimientoInventarioRequest();
        r.setProductoId(1L);
        r.setTipo(tipo);
        r.setCantidad(cantidad);
        return r;
    }

    @Test
    void compraActualizaStockCostoYProveedor() {
        Producto p = producto();
        Proveedor prov = Proveedor.builder().id(7L).nombre("Repuestos Sol").build();
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(p));
        when(proveedorRepository.findById(7L)).thenReturn(Optional.of(prov));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(
                Usuario.builder().id(3L).nombre("Pedro Quispe").build()));
        when(inventarioMovimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MovimientoInventarioRequest r = req("ENTRADA", 20);
        r.setCostoUnitario(new BigDecimal("14.50"));
        r.setProveedorId(7L);
        r.setDocumentoRef("F001-99");
        var dto = inventarioService.registrarMovimiento(r, 3L);

        assertEquals(30, p.getStockActual());
        assertEquals(new BigDecimal("14.50"), p.getCostoUnitario());
        assertEquals(Integer.valueOf(10), dto.getStockAntes());
        assertEquals(Integer.valueOf(30), dto.getStockDespues());
        assertEquals("Repuestos Sol", dto.getProveedorNombre());
        assertEquals("Pedro Quispe", dto.getUsuarioNombre());
        assertEquals("F001-99", dto.getDocumentoRef());
    }

    @Test
    void entradaSinCostoSeRechaza() {
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(producto()));
        assertThrows(BusinessException.class,
                () -> inventarioService.registrarMovimiento(req("ENTRADA", 5), 3L));
        verify(inventarioMovimientoRepository, never()).save(any());
    }

    @Test
    void mermaSinMotivoSeRechaza() {
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(producto()));
        assertThrows(BusinessException.class,
                () -> inventarioService.registrarMovimiento(req("MERMA", 2), 3L));
        verify(inventarioMovimientoRepository, never()).save(any());
    }

    @Test
    void mermaConMotivoDescuentaYAudita() {
        Producto p = producto();
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(p));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(
                Usuario.builder().id(3L).nombre("Pedro Quispe").build()));
        when(inventarioMovimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MovimientoInventarioRequest r = req("MERMA", 2);
        r.setMotivo("Vencido en almacén");
        var dto = inventarioService.registrarMovimiento(r, 3L);

        assertEquals(8, p.getStockActual());
        assertEquals(TipoMovimiento.MERMA.name(), dto.getTipo());
        assertEquals("Vencido en almacén", dto.getMotivo());
        assertEquals(new BigDecimal("15.00"), dto.getCostoUnitario());
    }

    @Test
    void consumoGuardaOTCostoYUsuario() {
        Producto p = producto();
        OrdenTrabajo ot = OrdenTrabajo.builder().id(9L).build();
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(p));
        when(ordenTrabajoRepository.findById(9L)).thenReturn(Optional.of(ot));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(
                Usuario.builder().id(3L).nombre("Luis Ramírez").build()));
        when(inventarioMovimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        InventarioMovimiento mov = inventarioService.registrarConsumoOT(9L, 1L, 4, 3L);

        assertEquals(6, p.getStockActual());
        assertEquals(TipoMovimiento.CONSUMO, mov.getTipo());
        assertEquals(9L, mov.getOrdenTrabajo().getId());
        assertEquals(3L, mov.getUsuario().getId());
        assertEquals(new BigDecimal("15.00"), mov.getCostoUnitario());
        assertEquals(Integer.valueOf(10), mov.getStockAntes());
        assertEquals(Integer.valueOf(6), mov.getStockDespues());
    }

    @Test
    void consumoSinStockSeRechazaSinTocarNada() {
        when(productoRepository.bloquearPorId(1L)).thenReturn(Optional.of(producto()));
        when(ordenTrabajoRepository.findById(9L)).thenReturn(Optional.of(OrdenTrabajo.builder().id(9L).build()));
        assertThrows(BusinessException.class,
                () -> inventarioService.registrarConsumoOT(9L, 1L, 99, 3L));
        verify(inventarioMovimientoRepository, never()).save(any());
    }
}
