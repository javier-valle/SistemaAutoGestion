package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.PadronRuc;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.repository.PadronRucRepository;
import com.autogestion.util.documento.ReglaRuc;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** DNI→RUC 10 usando el padrón SUNAT local: sin API key y sin datos de ejemplo. */
class SunatDniProviderTest {

    private static final String DNI = "12345678";

    private static String rucEsperado(String dni) {
        String base = "10" + dni;
        return base + ReglaRuc.digitoVerificador(base);
    }

    private static PadronRuc persona(String ruc) {
        return PadronRuc.builder()
                .ruc(ruc)
                .razonSocial("QUISPE ROJAS MARIA ELENA")
                .estado("ACTIVO")
                .condicion("HABIDO")
                .direccion("AV. LOS OLIVOS 123")
                .build();
    }

    @Test
    void resuelveElNombreConElRuc10DelDni() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        when(repo.count()).thenReturn(1L);
        when(repo.findById(rucEsperado(DNI))).thenReturn(Optional.of(persona(rucEsperado(DNI))));

        SunatDniProvider p = new SunatDniProvider(repo);
        ConsultaDocumentoResponseDTO r = p.consultar(TipoDocumento.DNI, DNI).orElseThrow();

        assertEquals(DNI, r.getDocumento());
        assertEquals("QUISPE ROJAS MARIA ELENA", r.getNombre());
        assertEquals("SUNAT · Padrón Reducido (RUC 10)", r.getFuente());
        assertEquals("ACTIVO", r.getEstado());
        assertEquals("HABIDO", r.getCondicion());
        assertTrue(p.disponible());
    }

    @Test
    void sinPadronImportadoNoEstaDisponibleYNoConsulta() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        when(repo.count()).thenReturn(0L);

        SunatDniProvider p = new SunatDniProvider(repo);

        assertFalse(p.disponible());
        assertTrue(p.consultar(TipoDocumento.DNI, DNI).isEmpty());
        verify(repo, never()).findById(anyString());
    }

    @Test
    void dniSinRuc10DevuelveVacio() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        when(repo.count()).thenReturn(1L);
        when(repo.findById(anyString())).thenReturn(Optional.empty());

        assertTrue(new SunatDniProvider(repo).consultar(TipoDocumento.DNI, DNI).isEmpty());
    }

    @Test
    void dniInvalidoNoTocaLaBase() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        SunatDniProvider p = new SunatDniProvider(repo);

        assertTrue(p.consultar(TipoDocumento.DNI, "123").isEmpty());
        assertTrue(p.consultar(TipoDocumento.DNI, "1234567A").isEmpty());
        assertTrue(p.consultar(TipoDocumento.DNI, null).isEmpty());
        verifyNoInteractions(repo);
    }

    @Test
    void razonSocialVaciaNoSePresentaComoResultado() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        String ruc = rucEsperado(DNI);
        when(repo.findById(ruc)).thenReturn(Optional.of(
                PadronRuc.builder().ruc(ruc).razonSocial("   ").build()));

        assertTrue(new SunatDniProvider(repo).consultar(TipoDocumento.DNI, DNI).isEmpty());
    }

    @Test
    void soloAplicaIADniYCacheaLaComprobacionDelPadron() {
        PadronRucRepository repo = mock(PadronRucRepository.class);
        when(repo.count()).thenReturn(1L);
        SunatDniProvider p = new SunatDniProvider(repo);

        assertTrue(p.soporta(TipoDocumento.DNI));
        assertFalse(p.soporta(TipoDocumento.RUC));

        assertTrue(p.disponible());
        assertTrue(p.disponible());
        verify(repo, times(1)).count();
    }
}
