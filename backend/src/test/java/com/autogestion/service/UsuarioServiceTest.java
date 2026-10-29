package com.autogestion.service;

import com.autogestion.dto.UsuarioRequest;
import com.autogestion.entity.Usuario;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reglas que protegen al equipo: sin duplicados, sin quedarse sin admin
 * y sin desactivar a quien tiene trabajo en curso.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioRequest mecanicoNuevo() {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombres("Luis Alberto");
        r.setApellidos("Ramírez Torres");
        r.setTipoDocumento("DNI");
        r.setDocumento("40258963");
        r.setTelefono("962458713");
        r.setEmail("lramirez@sanmartin.pe");
        r.setRol("MECANICO");
        r.setEspecialidad("MOTOR");
        r.setPassword("secreta123");
        return r;
    }

    private Usuario admin(Long id, boolean activo) {
        return Usuario.builder().id(id).nombre("Admin").rol("ADMIN").activo(activo).build();
    }

    @Test
    void emailDuplicadoSeRechaza() {
        when(usuarioRepository.existsByEmail("lramirez@sanmartin.pe")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.crear(mecanicoNuevo()));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void mecanicoSinEspecialidadSeRechaza() {
        UsuarioRequest r = mecanicoNuevo();
        r.setEspecialidad("");


        assertFalse(r.isEspecialidadRequerida());
    }

    @Test
    void noPuedesDesactivarteATiMismo() {
        Usuario yo = admin(1L, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(yo));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.cambiarActivo(1L, false, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    void noPuedesDejarAlSistemaSinAdmin() {
        Usuario ultimo = admin(1L, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ultimo));
        when(usuarioRepository.countByRolAndActivo("ADMIN", true)).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.cambiarActivo(1L, false, 2L));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    void mecanicoConOTEnProcesoNoSeDesactiva() {
        Usuario mec = Usuario.builder().id(5L).nombre("Luis").rol("MECANICO").activo(true).build();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(mec));
        when(ordenTrabajoRepository.countByMecanicoIdAndEstadoNotIn(eq(5L), anyList())).thenReturn(2L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> usuarioService.cambiarActivo(5L, false, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        assertTrue(ex.getMessage().contains("OT en proceso") && ex.getSugerencia().contains("Reasigna"));
    }

    @Test
    void mecanicoLibreSiSeDesactiva() {
        Usuario mec = Usuario.builder().id(5L).nombre("Luis").rol("MECANICO").activo(true).build();
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(mec));
        when(ordenTrabajoRepository.countByMecanicoIdAndEstadoNotIn(eq(5L), anyList())).thenReturn(0L);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        usuarioService.cambiarActivo(5L, false, 1L);
        assertFalse(mec.getActivo());
    }

    @Test
    void resumenDeMecanicosTraeCarga() {
        Usuario mec = Usuario.builder().id(5L).nombre("Luis Alberto Ramírez Torres")
                .rol("MECANICO").activo(true)
                .especialidad(com.autogestion.entity.Especialidad.MOTOR).build();
        when(usuarioRepository.findByRolAndActivo("MECANICO", true)).thenReturn(List.of(mec));
        when(ordenTrabajoRepository.countByMecanicoIdAndEstadoNotIn(eq(5L), anyList())).thenReturn(3L);
        when(ordenTrabajoRepository.countFinalizadasDesde(eq(5L), any())).thenReturn(7L);
        var lista = usuarioService.mecanicos(true);
        assertEquals(1, lista.size());
        assertEquals(3L, lista.get(0).getOtActivas());
        assertEquals(7L, lista.get(0).getOtFinalizadasMes());
        assertEquals("Motor", lista.get(0).getEspecialidadEtiqueta());
    }
}
