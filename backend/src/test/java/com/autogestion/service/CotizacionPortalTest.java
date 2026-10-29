package com.autogestion.service;

import com.autogestion.dto.CotizacionResponseDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** El portal solo ve cotizaciones del cliente del token; nunca por id suelto. */
class CotizacionPortalTest {

    private CotizacionService servicio(CotizacionRepository repo,
                                       CotizacionServicioRepository servicios,
                                       CotizacionProductoRepository productos) {
        return new CotizacionService(repo, servicios, productos,
                mock(DiagnosticoRepository.class), mock(ServicioRepository.class),
                mock(ProductoRepository.class), mock(RecepcionRepository.class));
    }

    @Test
    void listaCotizacionesDelClienteConPlacaItemsYTotal() {
        CotizacionRepository repo = mock(CotizacionRepository.class);
        CotizacionServicioRepository servicios = mock(CotizacionServicioRepository.class);
        CotizacionProductoRepository productos = mock(CotizacionProductoRepository.class);

        Cliente cliente = Cliente.builder().id(1L).nombre("Cliente Demo").build();
        Vehiculo vehiculo = Vehiculo.builder().id(2L).placa("ABC-123").cliente(cliente).build();
        Recepcion recepcion = Recepcion.builder().id(3L).vehiculo(vehiculo).build();
        Usuario mecanico = Usuario.builder().id(4L).nombres("Ana").apellidos("Torres").build();
        Diagnostico diagnostico = Diagnostico.builder()
                .id(5L).recepcion(recepcion).mecanico(mecanico)
                .descripcion("Ruido en frenos").build();
        Cotizacion cotizacion = Cotizacion.builder()
                .id(6L).diagnostico(diagnostico).total(new BigDecimal("250.00"))
                .estado(EstadoCotizacion.PENDIENTE).fecha(LocalDateTime.now()).build();

        Servicio servicio = Servicio.builder().id(7L).nombre("Cambio de frenos").build();
        CotizacionServicio cs = CotizacionServicio.builder()
                .id(8L).cotizacion(cotizacion).servicio(servicio).precio(new BigDecimal("100.00")).build();
        Producto producto = Producto.builder().id(9L).nombre("Pastillas").build();
        CotizacionProducto cp = CotizacionProducto.builder()
                .id(10L).cotizacion(cotizacion).producto(producto)
                .cantidadEstimada(1).precioUnitario(new BigDecimal("150.00")).build();

        when(repo.findByClienteId(1L)).thenReturn(List.of(cotizacion));
        when(servicios.findByCotizacionId(6L)).thenReturn(List.of(cs));
        when(productos.findByCotizacionId(6L)).thenReturn(List.of(cp));

        List<CotizacionResponseDTO> lista = servicio(repo, servicios, productos).listarPorCliente(1L);

        assertEquals(1, lista.size());
        CotizacionResponseDTO dto = lista.get(0);
        assertEquals(6L, dto.getId());
        assertEquals("ABC-123", dto.getVehiculoPlaca());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals(new BigDecimal("250.00"), dto.getTotal());
        assertEquals(1, dto.getServicios().size());
        assertEquals("Cambio de frenos", dto.getServicios().get(0).getServicioNombre());
        assertEquals(1, dto.getProductos().size());
        assertEquals("Pastillas", dto.getProductos().get(0).getProductoNombre());
    }

    @Test
    void noExponeUnaCotizacionDeOtroCliente() {
        CotizacionRepository repo = mock(CotizacionRepository.class);
        when(repo.findByIdAndClienteId(99L, 1L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio(repo, mock(CotizacionServicioRepository.class), mock(CotizacionProductoRepository.class))
                        .obtenerDeCliente(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, e.getEstado());
    }

    @Test
    void clienteNuloNoConsultaLaBase() {
        CotizacionRepository repo = mock(CotizacionRepository.class);
        assertTrue(servicio(repo, mock(CotizacionServicioRepository.class), mock(CotizacionProductoRepository.class))
                .listarPorCliente(null).isEmpty());
        verifyNoInteractions(repo);
    }
}
