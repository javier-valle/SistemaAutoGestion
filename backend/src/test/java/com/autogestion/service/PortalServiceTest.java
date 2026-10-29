package com.autogestion.service;

import com.autogestion.dto.ComprobanteResponseDTO;
import com.autogestion.dto.PortalEncuestaRequest;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock private PortalAccesoLogRepository portalAccesoLogRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock private ConsentimientoRepository consentimientoRepository;
    @Mock private ConsultaDocumentoService consultaDocumentoService;
    @Mock private ComprobanteRepository comprobanteRepository;
    @Mock private CotizacionServicioRepository cotizacionServicioRepository;
    @Mock private EncuestaSatisfaccionRepository encuestaSatisfaccionRepository;
    @Mock private ComprobanteService comprobanteService;
    @Mock private CitaRepository citaRepository;
    @Mock private SolicitudCotizacionRepository solicitudCotizacionRepository;
    @Mock private RecepcionRepository recepcionRepository;

    @InjectMocks
    private PortalService portalService;

    private Cliente cliente;
    private Vehiculo vehiculo;
    private Cotizacion cotizacion;
    private OrdenTrabajo orden;

    @BeforeEach
    void preparar() {
        cliente = Cliente.builder().id(1L).nombre("Ana Torres").activo(true).build();
        vehiculo = Vehiculo.builder().id(2L).placa("ABC-123").marca("Kia").modelo("Rio").anio(2020)
                .cliente(cliente).build();
        Recepcion recepcion = Recepcion.builder().id(3L).vehiculo(vehiculo)
                .fechaIngreso(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
        Diagnostico diagnostico = Diagnostico.builder().id(4L).recepcion(recepcion).build();
        cotizacion = Cotizacion.builder().id(5L).diagnostico(diagnostico)
                .total(new BigDecimal("120.00")).estado(EstadoCotizacion.APROBADA).build();
        orden = OrdenTrabajo.builder().id(6L).cotizacion(cotizacion).estado(EstadoOT.FINALIZADA)
                .fechaFin(LocalDateTime.of(2026, 10, 21, 17, 0)).build();
        lenient().when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    }

    @Test
    void historialTraeServiciosYComprobanteDeLaVisita() {
        when(ordenTrabajoRepository.findByClienteId(1L)).thenReturn(List.of(orden));
        when(comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(1L)).thenReturn(List.of(
                Comprobante.builder().id(9L).ordenTrabajo(orden).estado(EstadoComprobante.EMITIDO)
                        .serie("B001").numero(7).total(new BigDecimal("120.00")).build()));
        when(cotizacionServicioRepository.findByCotizacionId(5L)).thenReturn(List.of(
                CotizacionServicio.builder().servicio(Servicio.builder().nombre("Cambio de aceite").build())
                        .precio(new BigDecimal("120.00")).build()));

        var historial = portalService.historial(1L);

        assertEquals(1, historial.size());
        var visita = historial.get(0);
        assertEquals("B001-00000007", visita.comprobanteFolio());
        assertEquals("ABC-123", visita.placa());
        assertEquals("Kia Rio 2020", visita.vehiculo());
        assertEquals(List.of("Cambio de aceite"), visita.servicios());
        assertEquals(new BigDecimal("120.00"), visita.total());
    }

    @Test
    void historialSoloMuestraOrdenesEntregadas() {
        OrdenTrabajo abierta = OrdenTrabajo.builder().id(7L).cotizacion(cotizacion)
                .estado(EstadoOT.EN_PROCESO).build();
        when(ordenTrabajoRepository.findByClienteId(1L)).thenReturn(List.of(abierta, orden));
        when(comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(1L)).thenReturn(List.of());
        when(cotizacionServicioRepository.findByCotizacionId(5L)).thenReturn(List.of());

        var historial = portalService.historial(1L);

        assertEquals(1, historial.size());
        assertEquals(6L, historial.get(0).ordenTrabajoId());
    }

    @Test
    void comprobanteDeOtroClienteNoSeExpone() {
        when(comprobanteService.obtener(9L)).thenReturn(ComprobanteResponseDTO.builder()
                .id(9L).clienteId(99L).build());

        BusinessException e = assertThrows(BusinessException.class,
                () -> portalService.comprobanteDeCliente(1L, 9L));

        assertEquals(HttpStatus.NOT_FOUND, e.getEstado());
    }

    @Test
    void encuestaSeGuardaParaLaOrdenEntregadaDelCliente() {
        when(ordenTrabajoRepository.findById(6L)).thenReturn(Optional.of(orden));
        when(encuestaSatisfaccionRepository.existsByOrdenTrabajoIdAndClienteId(6L, 1L)).thenReturn(false);
        when(encuestaSatisfaccionRepository.save(any())).thenAnswer(inv -> {
            EncuestaSatisfaccion e = inv.getArgument(0);
            e.setId(11L);
            return e;
        });

        var dto = portalService.responderEncuesta(1L, solicitud(5, "  Muy buena atención  "));

        assertEquals(11L, dto.id());
        assertEquals(5, dto.puntuacion());
        assertEquals("Muy buena atención", dto.comentario());
        assertEquals("ABC-123", dto.placa());
        verify(encuestaSatisfaccionRepository).save(argThat(e ->
                e.getCliente().getId().equals(1L) && e.getOrdenTrabajo().getId().equals(6L)));
    }

    @Test
    void encuestaRechazaOrdenDeOtroCliente() {
        Cliente otro = Cliente.builder().id(50L).nombre("Otro").activo(true).build();
        vehiculo.setCliente(otro);
        when(ordenTrabajoRepository.findById(6L)).thenReturn(Optional.of(orden));

        BusinessException e = assertThrows(BusinessException.class,
                () -> portalService.responderEncuesta(1L, solicitud(4, null)));

        assertEquals(HttpStatus.NOT_FOUND, e.getEstado());
        verify(encuestaSatisfaccionRepository, never()).save(any());
    }

    @Test
    void encuestaRechazaOrdenNoEntregada() {
        orden.setEstado(EstadoOT.EN_PROCESO);
        when(ordenTrabajoRepository.findById(6L)).thenReturn(Optional.of(orden));

        BusinessException e = assertThrows(BusinessException.class,
                () -> portalService.responderEncuesta(1L, solicitud(3, null)));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        verify(encuestaSatisfaccionRepository, never()).save(any());
    }

    @Test
    void encuestaNoSeDuplica() {
        when(ordenTrabajoRepository.findById(6L)).thenReturn(Optional.of(orden));
        when(encuestaSatisfaccionRepository.existsByOrdenTrabajoIdAndClienteId(6L, 1L)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> portalService.responderEncuesta(1L, solicitud(5, "Otra vez")));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        verify(encuestaSatisfaccionRepository, never()).save(any());
    }

    @Test
    void pendientesExcluyeLaOrdenYaCalificada() {
        when(ordenTrabajoRepository.findByClienteId(1L)).thenReturn(List.of(orden));
        when(encuestaSatisfaccionRepository.existsByOrdenTrabajoIdAndClienteId(6L, 1L)).thenReturn(true);

        assertTrue(portalService.encuestasPendientes(1L).isEmpty());
    }

    @Test
    void bajaSinConfirmacionNoBorraNiAnonimiza() {
        com.autogestion.dto.PortalBajaRequest req = new com.autogestion.dto.PortalBajaRequest();
        req.setConfirmacion("quizas");

        BusinessException e = assertThrows(BusinessException.class,
                () -> portalService.eliminarCuenta(1L, req));

        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
        assertEquals("confirmacion", e.getCampo());
        verify(encuestaSatisfaccionRepository, never()).deleteByClienteId(any());
        verify(clienteRepository, never()).save(any());
        assertEquals(Boolean.TRUE, cliente.getActivo());
        assertEquals("Ana Torres", cliente.getNombre());
    }

    @Test
    void bajaBorraDatosDelPortalYAnonimizaConservandoElHistorial() {
        when(vehiculoRepository.findByClienteId(1L)).thenReturn(List.of(vehiculo));
        when(recepcionRepository.countByVehiculoId(2L)).thenReturn(1L);
        com.autogestion.dto.PortalBajaRequest req = new com.autogestion.dto.PortalBajaRequest();
        req.setConfirmacion("eliminar");

        portalService.eliminarCuenta(1L, req);

        verify(encuestaSatisfaccionRepository).deleteByClienteId(1L);
        verify(consentimientoRepository).deleteByClienteId(1L);
        verify(citaRepository).deleteByClienteId(1L);
        verify(solicitudCotizacionRepository).deleteByClienteId(1L);
        verify(portalAccesoLogRepository).deleteByClienteId(1L);
        verify(vehiculoRepository, never()).delete(any());
        verify(clienteRepository).save(cliente);

        assertEquals("Cliente dado de baja", cliente.getNombre());
        assertNull(cliente.getDocumento());
        assertFalse(cliente.getActivo());
        assertEquals(1L, cliente.getId());
    }

    @Test
    void bajaEliminaLosVehiculosSinVisitasRegistradas() {
        when(vehiculoRepository.findByClienteId(1L)).thenReturn(List.of(vehiculo));
        when(recepcionRepository.countByVehiculoId(2L)).thenReturn(0L);
        com.autogestion.dto.PortalBajaRequest req = new com.autogestion.dto.PortalBajaRequest();
        req.setConfirmacion("ELIMINAR");

        portalService.eliminarCuenta(1L, req);

        verify(vehiculoRepository).delete(vehiculo);
    }

    private static PortalEncuestaRequest solicitud(int puntuacion, String comentario) {
        PortalEncuestaRequest req = new PortalEncuestaRequest();
        req.setOrdenTrabajoId(6L);
        req.setPuntuacion(puntuacion);
        req.setComentario(comentario);
        return req;
    }
}
