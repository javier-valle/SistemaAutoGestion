package com.autogestion.service;

import com.autogestion.dto.CotizacionRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.EstadoCotizacion;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.CotizacionProductoRepository;
import com.autogestion.repository.CotizacionRepository;
import com.autogestion.repository.CotizacionServicioRepository;
import com.autogestion.repository.DiagnosticoRepository;
import com.autogestion.repository.ProductoRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.ServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Un diagnóstico puede renegociarse (rechazada y luego otra), pero no puede
 * tener dos cotizaciones vivas al mismo tiempo.
 */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private CotizacionServicioRepository cotizacionServicioRepository;
    @Mock private CotizacionProductoRepository cotizacionProductoRepository;
    @Mock private DiagnosticoRepository diagnosticoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private RecepcionRepository recepcionRepository;

    @InjectMocks
    private CotizacionService cotizacionService;

    private CotizacionRequest request() {
        CotizacionRequest r = new CotizacionRequest();
        r.setDiagnosticoId(4L);
        return r;
    }

    private Diagnostico diagnostico() {
        Cliente c = Cliente.builder().id(1L).nombre("Juan").build();
        Vehiculo v = Vehiculo.builder().id(2L).placa("ABC-123").cliente(c).build();
        Recepcion r = Recepcion.builder().id(3L).vehiculo(v).build();
        return Diagnostico.builder().id(4L).descripcion("Frenos").recepcion(r).build();
    }

    @Test
    void noPermiteDosCotizacionesVigentesParaElMismoDiagnostico() {
        when(diagnosticoRepository.findById(4L)).thenReturn(Optional.of(diagnostico()));
        when(cotizacionRepository.existsByDiagnosticoIdAndEstadoIn(eq(4L), anyCollection()))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> cotizacionService.crear(request()));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        assertEquals("diagnosticoId", ex.getCampo());
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void permiteCotizarTrasUnaRechazada() {
        when(diagnosticoRepository.findById(4L)).thenReturn(Optional.of(diagnostico()));
        when(cotizacionRepository.existsByDiagnosticoIdAndEstadoIn(eq(4L), anyCollection()))
                .thenReturn(false);
        when(cotizacionRepository.save(any())).thenAnswer(i -> {
            Cotizacion c = i.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(cotizacionServicioRepository.findByCotizacionId(any())).thenReturn(List.of());
        when(cotizacionProductoRepository.findByCotizacionId(any())).thenReturn(List.of());

        var dto = cotizacionService.crear(request());

        assertEquals(5L, dto.getId());
        assertEquals(EstadoCotizacion.PENDIENTE.name(), dto.getEstado());
        verify(cotizacionRepository, atLeastOnce()).save(any());
    }

    @Test
    void laVigenciaSoloCuentaEstadosAbiertos() {
        when(diagnosticoRepository.findById(4L)).thenReturn(Optional.of(diagnostico()));
        when(cotizacionRepository.existsByDiagnosticoIdAndEstadoIn(eq(4L), anyCollection()))
                .thenReturn(false);
        when(cotizacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cotizacionServicioRepository.findByCotizacionId(any())).thenReturn(List.of());
        when(cotizacionProductoRepository.findByCotizacionId(any())).thenReturn(List.of());

        cotizacionService.crear(request());

        verify(cotizacionRepository).existsByDiagnosticoIdAndEstadoIn(eq(4L),
                eq(List.of(EstadoCotizacion.PENDIENTE, EstadoCotizacion.EN_DIAGNOSTICO,
                        EstadoCotizacion.APROBADA)));
    }
}
