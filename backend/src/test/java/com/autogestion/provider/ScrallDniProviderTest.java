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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Proveedor Scrall: POST autenticado, parseo y degradación sin clave. */
class ScrallDniProviderTest {

    private HttpServer server;
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private volatile int status = 200;

    @BeforeEach
    void levantar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/reniec/basico", exchange -> {
            autorizacion.set(exchange.getRequestHeaders().getFirst("Authorization"));
            cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = status == 200
                    ? "{\"message\":\"found data\",\"result\":{\"D_DIGITOVERIFICACION\":4,"
                      + "\"D_NUDNI\":\"12345678\",\"D_APEPATERNO\":\"QUISPE\","
                      + "\"D_APEMATERNO\":\"ROJAS\",\"D_PRENOMBRES\":\"MARIA ELENA\","
                      + "\"D_FENACIMIENTO\":\"26/05/1990\"}}"
                    : "{\"message\":\"error\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
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

    private ScrallDniProvider provider(String token) {
        return new ScrallDniProvider(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/reniec/basico",
                token, 2000);
    }

    @Test
    void enviaPostConBasicYDevuelveNombreCompleto() {
        ConsultaDocumentoResponseDTO r = provider("clave-prueba")
                .consultar(TipoDocumento.DNI, "12345678").orElseThrow();

        assertEquals("QUISPE ROJAS MARIA ELENA", r.getNombre());
        assertEquals("QUISPE", r.getApellidoPaterno());
        assertEquals("ROJAS", r.getApellidoMaterno());
        assertEquals("Scrall · RENIEC básico", r.getFuente());
        assertEquals("Basic clave-prueba", autorizacion.get());
        assertTrue(cuerpo.get().contains("\"dni\":\"12345678\""));
    }

    @Test
    void sinClaveNoEstaDisponibleYNoLlamaAlServicio() {
        ScrallDniProvider p = provider("");
        assertFalse(p.disponible());
        assertTrue(p.consultar(TipoDocumento.DNI, "12345678").isEmpty());
        assertNull(autorizacion.get());
    }

    @Test
    void errorHttpSePropagaParaQueElServicioPruebeOtraFuente() {
        status = 401;
        assertThrows(IllegalStateException.class,
                () -> provider("clave-mala").consultar(TipoDocumento.DNI, "12345678"));
    }

    @Test
    void dniInvalidoNoLlamaAlServicio() {
        assertTrue(provider("clave").consultar(TipoDocumento.DNI, "123").isEmpty());
        assertNull(autorizacion.get());
    }
}
