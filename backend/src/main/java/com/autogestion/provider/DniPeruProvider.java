package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DNI gratis desde dniperu.com (tercero, NO oficial), igual que la herramienta
 * "example DNI and RUC search" pero ejecutado en el servidor.
 *
 * Por qué vive aquí y no en el navegador: el sitio de origen no envía cabeceras
 * CORS, así que el navegador bloquea la llamada (el mensaje "Solicitud de origen
 * cruzado bloqueada" / "La fuente de consulta no respondió"). El backend no está
 * sujeto a CORS: hace la petición, guarda la cookie de sesión y devuelve solo los
 * datos al portal.
 *
 * Cómo funciona el sitio:
 *  1. Se calienta la sesión con un GET a la página pública (crea cookies).
 *  2. POST admin-ajax.php con action=buscar_nombres y el DNI.
 *  3. Si responde code=token_required, se piden los tokens (action=cc_get_tokens)
 *     y se repite el POST con cc_token + cc_sig.
 *  4. Los nombres vienen dentro de data.message como líneas "Clave: valor".
 *
 * Límites que el código no oculta:
 *  - Es un scraping de un tercero: puede cambiar o dejar de responder.
 *  - No es RENIEC ni una verificación de identidad.
 *  - No se registra el documento completo ni los nombres en los logs.
 */
@Component
@Order(30)
@Slf4j
public class DniPeruProvider implements ProveedorDocumento {

    private static final String RUTA_BUSQUEDA = "/buscar-dni-nombres-apellidos/";
    private static final String RUTA_AJAX = "/wp-admin/admin-ajax.php";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    /** El HTML/JSON del sitio es pequeño; se corta por si devuelve algo enorme. */
    private static final int MAX_RESPUESTA = 200_000;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final String baseUrl;
    private final String ajaxUrl;
    private final String referer;
    private final long timeoutMs;
    private final boolean habilitado;

    /** Cookie de sesión del sitio: se crea una vez y se reutiliza. */
    private volatile boolean sesionLista;

    public DniPeruProvider(
            @Value("${dniperu.dni.base-url:https://dniperu.com}") String baseUrl,
            @Value("${dniperu.dni.habilitado:true}") boolean habilitado,
            @Value("${dniperu.dni.timeout-ms:8000}") long timeoutMs) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.ajaxUrl = this.baseUrl + RUTA_AJAX;
        this.referer = this.baseUrl + RUTA_BUSQUEDA;
        this.habilitado = habilitado;
        this.timeoutMs = timeoutMs <= 0 ? 8000 : timeoutMs;
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        log.info("Proveedor DNI dniperu.com {}", disponible() ? "habilitado (fuente gratuita)" : "deshabilitado");
    }

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.DNI;
    }

    @Override
    public boolean disponible() {
        return habilitado && !baseUrl.isBlank();
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        String dni = numero == null ? "" : numero.trim();
        if (!dni.matches("\\d{8}") || !disponible()) return Optional.empty();

        JsonNode respuesta = post(camposBusqueda(dni, null, null));
        if (pideToken(respuesta)) {
            JsonNode tokens = post(camposToken());
            JsonNode token = tokens.path("data").path("tokens");
            token = token.isArray() ? (token.isEmpty() ? token : token.get(0)) : tokens.path("data");
            String ccToken = texto(token, "cc_token");
            String ccSig = texto(token, "cc_sig");
            if (ccToken.isEmpty() || ccSig.isEmpty()) {
                throw new IllegalStateException("dniperu no entregó token");
            }
            respuesta = post(camposBusqueda(dni, ccToken, ccSig));
        }

        String mensaje = texto(respuesta.path("data"), "message");
        if (!respuesta.path("success").asBoolean(false)) {
            // "No encontrado" no es una caída técnica: el servicio sigue con la
            // siguiente fuente y, si no hay ninguna, el portal pasa a modo manual.
            if (pareceNoEncontrado(mensaje) || mensaje.isBlank()) return Optional.empty();
            throw new IllegalStateException("dniperu respondió success=false");
        }

        Datos datos = parsearMensaje(mensaje);
        if (datos == null) {
            log.warn("dniperu respondió sin nombres reconocibles para {}", enmascarar(dni));
            return Optional.empty();
        }

        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setTipoDocumento("DNI");
        dto.setDocumento(dni);
        dto.setNombre(datos.nombreCompleto());
        dto.setApellidoPaterno(vacioANull(datos.apellidoPaterno()));
        dto.setApellidoMaterno(vacioANull(datos.apellidoMaterno()));
        dto.setFuente("dniperu.com · consulta pública (no oficial)");
        return Optional.of(dto);
    }

    /** Pide la página pública para obtener las cookies que el AJAX exige. */
    private void calentarSesion() {
        if (sesionLista) return;
        synchronized (this) {
            if (sesionLista) return;
            HttpRequest request = HttpRequest.newBuilder(URI.create(referer))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "es-PE,es;q=0.9")
                    .GET()
                    .build();
            try {
                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 400) {
                    throw new IllegalStateException("dniperu página HTTP " + response.statusCode());
                }
                sesionLista = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("dniperu interrumpido");
            } catch (IOException e) {
                throw new IllegalStateException("dniperu sin conexión: " + e.getClass().getSimpleName());
            }
        }
    }

    private JsonNode post(String formulario) {
        calentarSesion();
        HttpRequest request = HttpRequest.newBuilder(URI.create(ajaxUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "es-PE,es;q=0.9")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", baseUrl)
                .header("Referer", referer)
                .POST(HttpRequest.BodyPublishers.ofString(formulario, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 403 || status == 429 || status == 503) {
                // Reto/cookie vencida: la próxima consulta vuelve a calentar la sesión.
                sesionLista = false;
                throw new IllegalStateException("dniperu HTTP " + status);
            }
            if (status >= 400) {
                throw new IllegalStateException("dniperu HTTP " + status);
            }
            String cuerpo = response.body() == null ? "" : response.body();
            if (cuerpo.length() > MAX_RESPUESTA) cuerpo = cuerpo.substring(0, MAX_RESPUESTA);
            try {
                return json.readTree(cuerpo);
            } catch (Exception e) {
                throw new IllegalStateException("dniperu no devolvió JSON");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("dniperu interrumpido");
        } catch (IOException e) {
            throw new IllegalStateException("dniperu sin conexión: " + e.getClass().getSimpleName());
        }
    }

    private String camposBusqueda(String dni, String ccToken, String ccSig) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("action", "buscar_nombres");
        campos.put("dni4", dni);
        campos.put("cc_trap_note_83e2", "");
        if (ccToken != null) {
            campos.put("cc_token", ccToken);
            campos.put("cc_sig", ccSig == null ? "" : ccSig);
        }
        return formulario(campos);
    }

    private String camposToken() {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("action", "cc_get_tokens");
        campos.put("context", "buscar_nombres");
        campos.put("cc_trap_note_83e2", "");
        return formulario(campos);
    }

    private static String formulario(Map<String, String> campos) {
        StringBuilder sb = new StringBuilder();
        campos.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static boolean pideToken(JsonNode respuesta) {
        if (respuesta == null) return false;
        JsonNode datos = respuesta.path("data");
        if ("token_required".equalsIgnoreCase(texto(datos, "code"))) return true;
        String mensaje = texto(datos, "message");
        return mensaje.toLowerCase().contains("token") && !respuesta.path("success").asBoolean(false);
    }

    private static boolean pareceNoEncontrado(String mensaje) {
        String m = mensaje == null ? "" : sinAcentos(mensaje.toLowerCase());
        return m.contains("no se encontr") || m.contains("no encontrad") || m.contains("no existe")
                || m.contains("no registra") || m.contains("sin resultado");
    }

    /**
     * Convierte el mensaje del sitio ("Clave: valor" por línea) en datos.
     * Devuelve null si no hay nombres: así el servicio distingue "no encontrado".
     */
    static Datos parsearMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) return null;
        Map<String, String> campos = new LinkedHashMap<>();
        for (String linea : mensaje.split("\\R")) {
            int corte = linea.indexOf(':');
            if (corte <= 0) continue;
            String clave = sinAcentos(linea.substring(0, corte).trim().toLowerCase());
            String valor = linea.substring(corte + 1).trim();
            if (!valor.isEmpty()) campos.put(clave, valor);
        }
        String nombres = campos.getOrDefault("nombres", "");
        String paterno = campos.getOrDefault("apellido paterno", "");
        String materno = campos.getOrDefault("apellido materno", "");
        if (nombres.isEmpty() && paterno.isEmpty() && materno.isEmpty()) return null;
        return new Datos(nombres, paterno, materno);
    }

    private static String sinAcentos(String s) {
        String normal = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normal.replaceAll("\\p{M}+", "");
    }

    private static String texto(JsonNode nodo, String campo) {
        if (nodo == null) return "";
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? "" : valor.asText("").trim();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String enmascarar(String doc) {
        return doc == null || doc.length() <= 3 ? "***" : doc.substring(0, 3) + "****";
    }

    /** Nombres tal como los entrega la fuente, antes de armar el nombre completo. */
    record Datos(String nombres, String apellidoPaterno, String apellidoMaterno) {

        /** Orden de presentación en Perú: apellidos y luego nombres. */
        String nombreCompleto() {
            return String.join(" ", apellidoPaterno, apellidoMaterno, nombres)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
}
