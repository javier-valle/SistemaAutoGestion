package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** openruc.com: RUC gratis sin clave, con "not_found" tratado como no encontrado. */
class OpenRucProviderTest {

    private HttpServer server;
    private final AtomicInteger llamadas = new AtomicInteger();
    private volatile int status = 200;
    private volatile String cuerpo = "{\"ruc\":\"20131312955\","
            + "\"razon_social\":\"SUPERINTENDENCIA NACIONAL DE ADUANAS Y DE ADMINISTRACION TRIBUTARIA - SUNAT\","
            + "\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\","
            + "\"direccion\":\"AV. GARCILASO DE LA VEGA NRO 1472\",\"ubigeo\":\"150101\"}";

    @BeforeEach
    void levantar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ruc/", exchange -> {
            llamadas.incrementAndGet();
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void apagar() {
        if (server != null) server.stop(0);
    }

    private OpenRucProvider provider() {
        return provider(true);
    }

    private OpenRucProvider provider(boolean habilitado) {
        return new OpenRucProvider(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api/ruc", habilitado, 2000);
    }

    @Test
    void devuelveRazonSocialYEstadoDelRuc() {
        ConsultaDocumentoResponseDTO r = provider()
                .consultar(TipoDocumento.RUC, "20131312955").orElseThrow();

        assertEquals("20131312955", r.getDocumento());
        assertEquals("SUPERINTENDENCIA NACIONAL DE ADUANAS Y DE ADMINISTRACION TRIBUTARIA - SUNAT",
                r.getRazonSocial());
        assertEquals(r.getRazonSocial(), r.getNombre());
        assertEquals("ACTIVO", r.getEstado());
        assertEquals("HABIDO", r.getCondicion());
        assertEquals("AV. GARCILASO DE LA VEGA NRO 1472", r.getDireccion());
        assertTrue(r.getFuente().startsWith("openruc.com"));
        assertEquals(1, llamadas.get());
    }

    @Test
    void errorNotFoundDelSitioEsUnRucInexistenteNoUnaCaida() {
        cuerpo = "{\"error\":\"not_found\"}";
        assertTrue(provider().consultar(TipoDocumento.RUC, "20552108845").isEmpty());
    }

    @Test
    void respuesta404NoEsErrorTecnico() {
        status = 404;
        cuerpo = "{\"error\":\"not_found\"}";
        assertTrue(provider().consultar(TipoDocumento.RUC, "20552108845").isEmpty());
    }

    @Test
    void errorDelServidorSePropagaParaProbarOtraFuente() {
        status = 500;
        assertThrows(IllegalStateException.class,
                () -> provider().consultar(TipoDocumento.RUC, "20131312955"));
    }

    @Test
    void rucInvalidoNoConsulta() {
        assertTrue(provider().consultar(TipoDocumento.RUC, "2013").isEmpty());
        assertEquals(0, llamadas.get());
    }

    @Test
    void deshabilitadoNoConsulta() {
        OpenRucProvider p = provider(false);
        assertFalse(p.disponible());
        assertTrue(p.consultar(TipoDocumento.RUC, "20131312955").isEmpty());
        assertEquals(0, llamadas.get());
    }
}
