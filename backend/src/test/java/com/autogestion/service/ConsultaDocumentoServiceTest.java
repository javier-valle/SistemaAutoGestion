package com.autogestion.service;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.provider.ProveedorDocumento;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Consulta de documento sin API key: proveedores en orden, caché y modo manual. */
class ConsultaDocumentoServiceTest {

    private static final String RUC_VALIDO = "20123456786";

    /** Proveedor de prueba: devuelve lo que se le pida y cuenta las llamadas. */
    private static class Stub implements ProveedorDocumento {
        final AtomicInteger llamadas = new AtomicInteger();
        final TipoDocumento tipo;
        final String nombre;
        final boolean falla;

        Stub(TipoDocumento tipo, String nombre, boolean falla) {
            this.tipo = tipo;
            this.nombre = nombre;
            this.falla = falla;
        }

        @Override public boolean soporta(TipoDocumento t) { return t == tipo; }

        @Override
        public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento t, String numero) {
            llamadas.incrementAndGet();
            if (falla) throw new IllegalStateException("fuente caída");
            if (nombre == null) return Optional.empty();
            ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
            dto.setNombre(nombre);
            dto.setRazonSocial(nombre);
            return Optional.of(dto);
        }
    }

    @Test
    void sinProveedoresDevuelveVacioNoVerificado() {
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of());
        ConsultaDocumentoResponseDTO r = s.consultar("DNI", "87654321");
        assertFalse(r.isVerificado());
        assertNull(r.getNombre());
    }

    @Test
    void proveedorQueFallaNoRompeYPasaAlSiguiente() {
        Stub roto = new Stub(TipoDocumento.RUC, null, true);
        Stub bueno = new Stub(TipoDocumento.RUC, "EMPRESA DEMO SAC", false);
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of(roto, bueno));

        ConsultaDocumentoResponseDTO r = s.consultar("RUC", RUC_VALIDO);

        assertTrue(r.isVerificado());
        assertEquals("EMPRESA DEMO SAC", r.getNombre());
        assertEquals(RUC_VALIDO, r.getDocumento());
        assertEquals(1, roto.llamadas.get());
    }

    @Test
    void todosFallanDevuelveVacio() {
        ConsultaDocumentoService s = new ConsultaDocumentoService(
                List.of(new Stub(TipoDocumento.RUC, null, true), new Stub(TipoDocumento.RUC, null, false)));
        assertFalse(s.consultar("RUC", RUC_VALIDO).isVerificado());
    }

    @Test
    void cacheaPositivosYNegativos() {
        Stub encontrado = new Stub(TipoDocumento.RUC, "EMPRESA DEMO SAC", false);
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of(encontrado));
        s.consultar("RUC", RUC_VALIDO);
        s.consultar("RUC", RUC_VALIDO);
        assertEquals(1, encontrado.llamadas.get());

        Stub vacio = new Stub(TipoDocumento.RUC, null, false);
        ConsultaDocumentoService s2 = new ConsultaDocumentoService(List.of(vacio));
        ConsultaDocumentoResponseDTO r1 = s2.consultar("RUC", RUC_VALIDO);
        ConsultaDocumentoResponseDTO r2 = s2.consultar("RUC", RUC_VALIDO);
        assertEquals(1, vacio.llamadas.get(), "el no encontrado se cachea unos minutos");
        assertEquals(ConsultaDocumentoResponseDTO.MOTIVO_NO_ENCONTRADO, r1.getMotivo());
        assertEquals(r1.getMotivo(), r2.getMotivo());
    }

    @Test
    void proveedorSinConfigurarSeInformaComoSinFuente() {
        Stub sinToken = new Stub(TipoDocumento.DNI, "NO DEBE USARSE", false) {
            @Override public boolean disponible() { return false; }
        };
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of(sinToken));

        ConsultaDocumentoResponseDTO r = s.consultar("DNI", "87654321");

        assertFalse(r.isVerificado());
        assertEquals(ConsultaDocumentoResponseDTO.MOTIVO_SIN_FUENTE, r.getMotivo());
        assertEquals(0, sinToken.llamadas.get());
    }

    @Test
    void documentoInvalidoIndicaMotivoInvalido() {
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of());
        assertEquals(ConsultaDocumentoResponseDTO.MOTIVO_INVALIDO,
                s.consultar("DNI", "123").getMotivo());
    }

    @Test
    void documentoInvalidoNoConsultaNingunaFuente() {
        Stub p = new Stub(TipoDocumento.RUC, "X", false);
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of(p));
        assertFalse(s.consultar("RUC", "123").isVerificado());
        assertFalse(s.consultar("XXX", RUC_VALIDO).isVerificado());
        assertEquals(0, p.llamadas.get());
    }

    @Test
    void proveedorSoloSeUsaParaSuTipo() {
        Stub soloRuc = new Stub(TipoDocumento.RUC, "X", false);
        ConsultaDocumentoService s = new ConsultaDocumentoService(List.of(soloRuc));
        assertFalse(s.consultar("DNI", "87654321").isVerificado());
        assertEquals(0, soloRuc.llamadas.get());
    }
}
