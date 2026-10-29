package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * DNI desde Scrall (tercero, no oficial) usando su endpoint reniec_basico.
 *
 * Por qué existe: el padrón SUNAT solo cubre personas con RUC 10. Scrall
 * ofrece un plan API Básico gratuito para nombres y apellidos, pero exige
 * registro y una clave; NO se usa el endpoint sin autenticación aunque en
 * algún momento responda, porque sus términos exigen usuario autenticado.
 *
 * Configuración:
 *   SCRALL_API_KEY   clave del panel de Scrall (obligatoria)
 *   SCRALL_DNI_URL   por defecto https://api.scrall.net/v1/reniec/basico
 *
 * La clave se envía como "Authorization: Basic <clave>", tal como documenta
 * Scrall. Nunca se registra en logs ni se devuelve al navegador.
 */
@Component
@Order(15)
@Slf4j
public class ScrallDniProvider implements ProveedorDocumento {

    private final RestClient client;
    private final String url;
    private final String token;

    public ScrallDniProvider(
            @Value("${scrall.dni.url:https://api.scrall.net/v1/reniec/basico}") String url,
            @Value("${scrall.dni.token:}") String token,
            @Value("${scrall.dni.timeout-ms:4000}") long timeoutMs) {
        this.url = url == null ? "" : url.trim();
        this.token = token == null ? "" : token.trim();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.client = RestClient.builder().requestFactory(factory).build();
        log.info("Proveedor DNI Scrall {}", disponible()
                ? "habilitado (plan API Básico)"
                : "deshabilitado: falta SCRALL_API_KEY");
    }

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.DNI;
    }

    @Override
    public boolean disponible() {
        return !url.isBlank() && !token.isBlank();
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        String dni = numero == null ? "" : numero.trim();
        if (!dni.matches("\\d{8}")) return Optional.empty();
        if (!disponible()) return Optional.empty();

        JsonNode body = client.post()
                .uri(url)
                .header("Authorization", "Basic " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("dni", dni))
                .retrieve()
                // 404/422 = el DNI no existe en la fuente: no es una caída técnica.
                .onStatus(s -> s.value() == 404 || s.value() == 422, (req, res) -> { })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("Scrall HTTP " + res.getStatusCode().value());
                })
                .body(JsonNode.class);

        if (body == null || body.isNull()) return Optional.empty();
        JsonNode data = body.has("result") ? body.get("result") : body;
        if (data == null || !data.isObject()) return Optional.empty();

        String nombres = primero(data, "D_PRENOMBRES", "preNombres", "nombres", "nombresCompletos");
        String paterno = primero(data, "D_APEPATERNO", "apePaterno", "apellidoPaterno", "apellido_paterno");
        String materno = primero(data, "D_APEMATERNO", "apeMaterno", "apellidoMaterno", "apellido_materno");
        String completo = primero(data, "nombreCompleto", "nombre_completo", "full_name");

        if (completo.isEmpty()) {
            completo = String.join(" ", paterno, materno, nombres).trim().replaceAll("\\s+", " ");
        }
        if (completo.isEmpty()) {
            log.warn("Scrall respondió sin un nombre reconocible. Campos recibidos: {}",
                    String.join(",", (Iterable<String>) data::fieldNames));
            return Optional.empty();
        }

        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setTipoDocumento("DNI");
        dto.setDocumento(dni);
        dto.setNombre(completo);
        dto.setApellidoPaterno(vacioANull(paterno));
        dto.setApellidoMaterno(vacioANull(materno));
        dto.setFuente("Scrall · RENIEC básico");
        return Optional.of(dto);
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String primero(JsonNode n, String... campos) {
        for (String c : campos) {
            JsonNode v = n.get(c);
            if (v != null && !v.isNull()) {
                String s = v.asText("").trim();
                if (!s.isEmpty()) return s;
            }
        }
        return "";
    }
}
