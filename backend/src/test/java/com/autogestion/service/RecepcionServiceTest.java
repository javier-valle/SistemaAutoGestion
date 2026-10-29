package com.autogestion.service;

import com.autogestion.dto.RecepcionCompletaRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * La identidad es (tipoDocumento, documento). El teléfono y el email
 * jamás fusionan personas, y una placa no cambia de dueño sola.
 */
@ExtendWith(MockitoExtension.class)
class RecepcionServiceTest {

    @Mock
    private RecepcionRepository recepcionRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private RecepcionService recepcionService;

    private RecepcionCompletaRequest base() {
        RecepcionCompletaRequest r = new RecepcionCompletaRequest();
        r.setClienteNombre("Pedro Paredes");
        r.setClienteTipoDocumento("DNI");
        r.setClienteDocumento("87654321");
        r.setClienteTelefono("951234567");
        r.setVehiculoPlaca("XYZ-999");
        r.setProblemaReportado("Ruido al frenar");
        return r;
    }

    @Test
    void mismoTelefonoDistintoDocumentoNoFusiona() {
        when(clienteRepository.findByTipoDocumentoAndDocumento(TipoDocumento.DNI, "87654321"))
                .thenReturn(Optional.empty());
        Cliente nuevo = Cliente.builder().id(10L).nombre("Pedro Paredes").documento("87654321").build();
        when(clienteRepository.save(any(Cliente.class))).thenReturn(nuevo);
        Vehiculo v = Vehiculo.builder().id(20L).placa("XYZ-999").cliente(nuevo).build();
        when(vehiculoRepository.findByPlaca("XYZ-999")).thenReturn(Optional.empty());
        when(vehiculoRepository.save(any(Vehiculo.class))).thenReturn(v);
        when(recepcionRepository.save(any(Recepcion.class))).thenAnswer(i -> i.getArgument(0));

        recepcionService.crearCompleto(base());


        verify(clienteRepository, never()).findByTelefono(any());
        verify(clienteRepository, never()).findByEmail(any());
        verify(clienteRepository).save(argThat((Cliente c) -> "87654321".equals(c.getDocumento())));
    }

    @Test
    void placaDeOtroDuenoSeBloquea() {
        Cliente yo = Cliente.builder().id(1L).nombre("Yo").documento("87654321").build();
        Cliente otro = Cliente.builder().id(99L).nombre("Otro").documento("11223344").build();
        when(clienteRepository.findByTipoDocumentoAndDocumento(TipoDocumento.DNI, "87654321"))
                .thenReturn(Optional.of(yo));
        Vehiculo suyo = Vehiculo.builder().id(5L).placa("XYZ-999").cliente(otro).build();
        when(vehiculoRepository.findByPlaca("XYZ-999")).thenReturn(Optional.of(suyo));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recepcionService.crearCompleto(base()));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(recepcionRepository, never()).save(any());
    }

    @Test
    void conClienteIdYVehiculoIdNoBuscaNiCreaNada() {
        Cliente yo = Cliente.builder().id(1L).nombre("Yo").documento("87654321").build();
        Vehiculo mio = Vehiculo.builder().id(5L).placa("XYZ-999").cliente(yo).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(yo));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(mio));
        when(recepcionRepository.save(any(Recepcion.class))).thenAnswer(i -> i.getArgument(0));

        RecepcionCompletaRequest r = base();
        r.setClienteId(1L);
        r.setVehiculoId(5L);
        recepcionService.crearCompleto(r);

        verify(clienteRepository, never()).findByTipoDocumentoAndDocumento(any(), any());
        verify(clienteRepository, never()).save(any());
        verify(vehiculoRepository, never()).save(any());
        verify(recepcionRepository).save(argThat((Recepcion x) -> x.getVehiculo().getId().equals(5L)));
    }

    @Test
    void wizardConIdsNoExigeNombreNiPlaca() {


        Cliente yo = Cliente.builder().id(1L).nombre("Yo").documento("87654321").build();
        Vehiculo mio = Vehiculo.builder().id(5L).placa("XYZ-999").cliente(yo).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(yo));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(mio));
        when(recepcionRepository.save(any(Recepcion.class))).thenAnswer(i -> {
            Recepcion r = i.getArgument(0);
            r.setId(99L);
            return r;
        });

        RecepcionCompletaRequest r = new RecepcionCompletaRequest();
        r.setClienteId(1L);
        r.setVehiculoId(5L);
        r.setProblemaReportado("Ruido al frenar");
        var dto = recepcionService.crearCompleto(r);

        assertEquals(99L, dto.getId());
        assertEquals("XYZ-999", dto.getVehiculoPlaca());
        verify(clienteRepository, never()).save(any());
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void sinClienteIdSiExigeNombre() {
        RecepcionCompletaRequest r = new RecepcionCompletaRequest();
        r.setVehiculoPlaca("XYZ-999");
        r.setProblemaReportado("Ruido al frenar");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recepcionService.crearCompleto(r));
        assertEquals("clienteNombre", ex.getCampo());
        verify(recepcionRepository, never()).save(any());
    }

    @Test
    void vehiculoIdDeOtroClienteSeBloquea() {
        Cliente yo = Cliente.builder().id(1L).nombre("Yo").documento("87654321").build();
        Cliente otro = Cliente.builder().id(99L).nombre("Otro").build();
        Vehiculo suyo = Vehiculo.builder().id(5L).placa("XYZ-999").cliente(otro).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(yo));
        when(vehiculoRepository.findById(5L)).thenReturn(Optional.of(suyo));

        RecepcionCompletaRequest r = base();
        r.setClienteId(1L);
        r.setVehiculoId(5L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> recepcionService.crearCompleto(r));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(recepcionRepository, never()).save(any());
    }

    @Test
    void clienteYVehiculoPropiosSeReutilizan() {
        Cliente yo = Cliente.builder().id(1L).nombre("Yo").documento("87654321").build();
        when(clienteRepository.findByTipoDocumentoAndDocumento(TipoDocumento.DNI, "87654321"))
                .thenReturn(Optional.of(yo));
        Vehiculo mio = Vehiculo.builder().id(5L).placa("XYZ-999").cliente(yo).build();
        when(vehiculoRepository.findByPlaca("XYZ-999")).thenReturn(Optional.of(mio));
        when(recepcionRepository.save(any(Recepcion.class))).thenAnswer(i -> i.getArgument(0));

        recepcionService.crearCompleto(base());

        verify(clienteRepository, never()).save(any());
        verify(vehiculoRepository, never()).save(any());
        verify(recepcionRepository).save(argThat((Recepcion r) -> r.getVehiculo().getId().equals(5L)));
    }
}
