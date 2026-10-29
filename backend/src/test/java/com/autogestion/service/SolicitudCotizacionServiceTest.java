package com.autogestion.service;

import com.autogestion.dto.SolicitudCotizacionRequest;
import com.autogestion.dto.SolicitudCotizacionResponseDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.SolicitudCotizacionRepository;
import com.autogestion.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudCotizacionServiceTest {

    private final SolicitudCotizacionRepository repo = mock(SolicitudCotizacionRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VehiculoRepository vehiculos = mock(VehiculoRepository.class);
    private final SolicitudCotizacionService servicio = new SolicitudCotizacionService(repo, clientes, vehiculos);

    private Cliente cliente(Long id, String nombre) {
        return Cliente.builder().id(id).nombre(nombre).tipoDocumento(TipoDocumento.DNI).build();
    }

    private Vehiculo vehiculo(Long id, String placa, Cliente duenio) {
        return Vehiculo.builder().id(id).placa(placa).cliente(duenio).build();
    }

    private SolicitudCotizacionRequest request(Long vehiculoId, String descripcion) {
        SolicitudCotizacionRequest r = new SolicitudCotizacionRequest();
        r.setVehiculoId(vehiculoId);
        r.setDescripcion(descripcion);
        return r;
    }

    @Test
    void creaSolicitudParaUnVehiculoDelCliente() {
        Cliente duenio = cliente(1L, "Ana Torres");
        Vehiculo auto = vehiculo(2L, "ABC-123", duenio);
        when(clientes.findById(1L)).thenReturn(Optional.of(duenio));
        when(vehiculos.findById(2L)).thenReturn(Optional.of(auto));
        when(repo.save(any(SolicitudCotizacion.class))).thenAnswer(inv -> {
            SolicitudCotizacion s = inv.getArgument(0);
            s.setId(9L);
            return s;
        });

        SolicitudCotizacionResponseDTO dto = servicio.crear(1L,
                request(2L, "  Ruido al frenar   y vibración en la dirección.  "));

        assertEquals(9L, dto.getId());
        assertEquals("ABC-123", dto.getVehiculoPlaca());
        assertEquals("PENDIENTE", dto.getEstado());
        assertEquals("Ruido al frenar y vibración en la dirección.", dto.getDescripcion());
        verify(repo).save(any(SolicitudCotizacion.class));
    }

    @Test
    void noPermiteUsarElVehiculoDeOtroCliente() {
        Cliente otro = cliente(2L, "Otro Dueño");
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ana Torres")));
        when(vehiculos.findById(5L)).thenReturn(Optional.of(vehiculo(5L, "XYZ-999", otro)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.crear(1L, request(5L, "Ruido fuerte al frenar.")));

        assertEquals(HttpStatus.FORBIDDEN, e.getEstado());
        verify(repo, never()).save(any());
    }

    @Test
    void evitaDosSolicitudesPendientesParaLaMismaPlaca() {
        Cliente duenio = cliente(1L, "Ana Torres");
        when(clientes.findById(1L)).thenReturn(Optional.of(duenio));
        when(vehiculos.findById(2L)).thenReturn(Optional.of(vehiculo(2L, "ABC-123", duenio)));
        when(repo.existsByVehiculoIdAndEstado(2L, EstadoSolicitudCotizacion.PENDIENTE)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.crear(1L, request(2L, "Ruido fuerte al frenar.")));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertTrue(e.getMessage().contains("ABC-123"));
        verify(repo, never()).save(any());
    }

    @Test
    void atenderSoloFuncionaConSolicitudPendiente() {
        SolicitudCotizacion s = SolicitudCotizacion.builder()
                .id(3L).cliente(cliente(1L, "Ana Torres")).vehiculo(vehiculo(2L, "ABC-123", cliente(1L, "Ana Torres")))
                .descripcion("Ruido al frenar.").estado(EstadoSolicitudCotizacion.ATENDIDA)
                .creadoEn(LocalDateTime.now()).actualizadoEn(LocalDateTime.now()).build();
        when(repo.findById(3L)).thenReturn(Optional.of(s));

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.atender(3L, "Revisado."));

        assertEquals(HttpStatus.CONFLICT, e.getEstado());
    }

    @Test
    void elClienteSoloVeSusSolicitudes() {
        assertTrue(servicio.listarDeCliente(null).isEmpty());
        verifyNoInteractions(repo);
    }
}
