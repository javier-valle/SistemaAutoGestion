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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * dniperu.com: el flujo del ejemplo (calentar sesión, token y parseo del mensaje)
 * ejecutado en el backend, contra un servidor local que imita al sitio.
 */
class DniPeruProviderTest {

    private HttpServer server;
    private final List<String> ajax = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean exigirToken = true;
    private volatile String mensaje =
            "Número de DNI: 70716281\nNombres: CARLOS JAVIER\nApellido Paterno: VALLE\n"
                    + "Apellido Materno: COBEÑAS\nCódigo de Verificación: 9";
    private volatile int status = 200;

    @BeforeEach
    void levantar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/buscar-dni-nombres-apellidos/", exchange -> {
            byte[] html = "<html>ok</html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().add("Set-Cookie", "sesion=abc; Path=/");
            exchange.sendResponseHeaders(200, html.length);
            exchange.getResponseBody().write(html);
            exchange.close();
        });
        server.createContext("/wp-admin/admin-ajax.php", exchange -> {
            String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ajax.add(cuerpo);
            String json;
            if (status != 200) {
                json = "{\"error\":\"boom\"}";
            } else if (cuerpo.contains("action=cc_get_tokens")) {
                json = "{\"success\":true,\"data\":{\"tokens\":[{\"cc_token\":\"tok-1\",\"cc_sig\":\"sig-1\"}]}}";
            } else if (exigirToken && !cuerpo.contains("cc_token=tok-1")) {
                json = "{\"success\":false,\"data\":{\"code\":\"token_required\",\"message\":\"token required\"}}";
            } else {
                json = "{\"success\":true,\"data\":{\"message\":\"" + escapar(mensaje) + "\"}}";
            }
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

    private DniPeruProvider provider() {
        return provider(true);
    }

    private DniPeruProvider provider(boolean habilitado) {
        return new DniPeruProvider("http://127.0.0.1:" + server.getAddress().getPort(), habilitado, 2000);
    }

    @Test
    void resuelveElDniConTokenYDevuelveLosNombres() {
        ConsultaDocumentoResponseDTO r = provider()
                .consultar(TipoDocumento.DNI, "70716281").orElseThrow();

        assertEquals("VALLE COBEÑAS CARLOS JAVIER", r.getNombre());
        assertEquals("VALLE", r.getApellidoPaterno());
        assertEquals("COBEÑAS", r.getApellidoMaterno());
        assertTrue(r.getFuente().startsWith("dniperu.com"));
        assertEquals(3, ajax.size(), "calienta sesión, pide token y reintenta");
        assertTrue(ajax.get(0).contains("action=buscar_nombres"));
        assertTrue(ajax.get(1).contains("action=cc_get_tokens"));
        assertTrue(ajax.get(2).contains("cc_token=tok-1"));
    }

    @Test
    void siElPrimeroYaTraeDatosNoPideToken() {
        exigirToken = false;
        ConsultaDocumentoResponseDTO r = provider()
                .consultar(TipoDocumento.DNI, "70716281").orElseThrow();

        assertEquals("VALLE COBEÑAS CARLOS JAVIER", r.getNombre());
        assertEquals(1, ajax.size());
    }

    @Test
    void respuestaSinNombresEsNoEncontradoYNoEsUnError() {
        exigirToken = false;
        mensaje = "Número de DNI: 00000000";
        assertTrue(provider().consultar(TipoDocumento.DNI, "00000000").isEmpty());
    }

    @Test
    void dniInvalidoNoHaceNingunaLlamada() {
        assertTrue(provider().consultar(TipoDocumento.DNI, "123").isEmpty());
        assertTrue(ajax.isEmpty());
    }

    @Test
    void deshabilitadoNoConsultaNiAunqueElDniSeaValido() {
        DniPeruProvider p = provider(false);
        assertFalse(p.disponible());
        assertTrue(p.consultar(TipoDocumento.DNI, "70716281").isEmpty());
        assertTrue(ajax.isEmpty());
    }

    @Test
    void errorDelSitioSePropagaParaQueElServicioPruebeOtraFuente() {
        status = 500;
        assertThrows(IllegalStateException.class,
                () -> provider().consultar(TipoDocumento.DNI, "70716281"));
    }

    @Test
    void parseaElMensajeConAcentosYFinDeLineaWindows() {
        DniPeruProvider.Datos d = DniPeruProvider.parsearMensaje(
                "Número de DNI: 12345678\r\nNombres: MARÍA ELENA\r\nApellido Paterno: QUISPE\r\n"
                        + "Apellido Materno: ROJAS\r\nCódigo de Verificación: 9");

        assertNotNull(d);
        assertEquals("MARÍA ELENA", d.nombres());
        assertEquals("QUISPE", d.apellidoPaterno());
        assertEquals("ROJAS", d.apellidoMaterno());
        assertEquals("QUISPE ROJAS MARÍA ELENA", d.nombreCompleto());
        assertNull(DniPeruProvider.parsearMensaje("Número de DNI: 12345678"));
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
