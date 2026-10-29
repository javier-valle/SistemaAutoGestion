package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * DNI desde un servicio REST compatible con RENIEC (por defecto Decolecta,
 * ex apis.net.pe: Authorization: Bearer sk_...).
 *
 * RENIEC no publica una API abierta sin credenciales. Por eso:
 *  - Con token configurado, consulta la API externa.
 *  - Sin token, el proveedor se declara NO DISPONIBLE y el portal pasa al
 *    modo manual. No se consulta "a ciegas" ni se usan datos de ejemplo.
 *
 * La URL admite el marcador {numero}; si no lo tiene, se agrega ?numero=...
 * (así funciona tanto api.decolecta.com/v1/reniec/dni?numero=... como
 *  cualquier endpoint del tipo /dni/{numero}).
 */
@Component
@Order(20)
@Slf4j
public class ReniecDniProvider implements ProveedorDocumento {

    private final RestClient client;
    private final String url;
    private final String token;
    private final boolean requiereToken;
    private final String nombreFuente;

    public ReniecDniProvider(
            @Value("${reniec.dni.url:https://api.decolecta.com/v1/reniec/dni}") String url,
            @Value("${reniec.dni.token:}") String token,
            @Value("${reniec.dni.timeout-ms:4000}") long timeoutMs,
            @Value("${reniec.dni.requiere-token:true}") boolean requiereToken,
            @Value("${reniec.dni.fuente:RENIEC vía proveedor externo}") String nombreFuente) {
        this.url = url == null ? "" : url.trim();
        this.token = token == null ? "" : token.trim();
        this.requiereToken = requiereToken;
        this.nombreFuente = nombreFuente;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.DNI;
    }

    @Override
    public boolean disponible() {
        return !url.isBlank() && (!requiereToken || !token.isEmpty());
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        var request = client.get()
                .uri(construirUri(numero))
                .header("Accept", "application/json");
        if (!token.isEmpty()) {
            request = request.header("Authorization", "Bearer " + token);
        }
        JsonNode body = request
                .retrieve()

                .onStatus(s -> s.value() == 404 || s.value() == 422, (req, res) -> { })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("RENIEC HTTP " + res.getStatusCode().value());
                })
                .body(JsonNode.class);
        if (body == null || body.isNull()) return Optional.empty();

        JsonNode data = body.has("data") ? body.get("data") : body;
        if (data != null && data.isArray()) {
            data = data.isEmpty() ? null : data.get(0);
        }
        if (data == null || !data.isObject()) return Optional.empty();

        String nombres = primero(data, "nombres", "nombresCompletos", "first_name", "names", "name");
        String paterno = primero(data, "apellidoPaterno", "apellido_paterno", "first_last_name", "paternal_surname");
        String materno = primero(data, "apellidoMaterno", "apellido_materno", "second_last_name", "maternal_surname");
        String apellidos = primero(data, "apellidos", "surnames", "last_names");
        String completo = primero(data, "nombreCompleto", "nombre_completo", "full_name", "nombresCompletos");

        if (completo.isEmpty()) {
            if (apellidos.isEmpty()) {
                apellidos = String.join(" ", paterno, materno).trim();
            }

            completo = String.join(" ", apellidos, nombres).trim().replaceAll("\\s+", " ");
        }
        if (completo.isEmpty()) {

            log.warn("RENIEC respondió sin un nombre reconocible. Campos recibidos: {}",
                    String.join(",", (Iterable<String>) data::fieldNames));
            return Optional.empty();
        }

        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setNombre(completo);
        dto.setApellidoPaterno(vacioANull(paterno));
        dto.setApellidoMaterno(vacioANull(materno));
        dto.setFuente(nombreFuente);
        return Optional.of(dto);
    }

    /** Construye la URL sin romper query params existentes y sin inyectar texto del usuario. */
    private String construirUri(String numero) {
        if (url.contains("{numero}") || url.contains("{dni}")) {
            return url.replace("{numero}", numero).replace("{dni}", numero);
        }
        return UriComponentsBuilder.fromUriString(url)
                .replaceQueryParam("numero", numero)
                .build()
                .toUriString();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String primero(JsonNode n, String... campos) {
        for (String c : campos) {
            String v = texto(n, c);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n.get(campo);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }
}
