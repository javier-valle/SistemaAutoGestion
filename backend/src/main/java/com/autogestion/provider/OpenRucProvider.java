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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/**
 * RUC gratis desde openruc.com (API pública, sin clave), igual que la
 * herramienta "example DNI and RUC search" pero ejecutada en el servidor.
 *
 * Se usa cuando el Padrón Reducido RUC no está importado o no tiene ese RUC.
 * El navegador no puede llamarlo directamente por CORS; el backend sí.
 *
 * Configuración:
 *   openruc.ruc.habilitado  (true por defecto)
 *   openruc.ruc.url         (por defecto https://openruc.com/api/ruc)
 *   openruc.ruc.timeout-ms  (8000 por defecto)
 *
 * Respuesta esperada: {ruc, razon_social, estado, condicion, direccion, ubigeo}
 * o {"error":"not_found"} cuando el RUC no existe.
 */
@Component
@Order(20)
@Slf4j
public class OpenRucProvider implements ProveedorDocumento {

    private final RestClient client;
    private final String url;
    private final boolean habilitado;

    public OpenRucProvider(
            @Value("${openruc.ruc.url:https://openruc.com/api/ruc}") String url,
            @Value("${openruc.ruc.habilitado:true}") boolean habilitado,
            @Value("${openruc.ruc.timeout-ms:8000}") long timeoutMs) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.habilitado = habilitado;
        long timeout = timeoutMs <= 0 ? 8000 : timeoutMs;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeout)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeout));
        this.client = RestClient.builder().requestFactory(factory).build();
        log.info("Proveedor RUC openruc.com {}", disponible() ? "habilitado (fuente gratuita)" : "deshabilitado");
    }

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.RUC;
    }

    @Override
    public boolean disponible() {
        return habilitado && !url.isBlank();
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        String ruc = numero == null ? "" : numero.trim();
        if (!ruc.matches("\\d{11}") || !disponible()) return Optional.empty();

        JsonNode body = client.get()
                .uri(url + "/" + ruc)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (compatible; AutogestionTaller/1.0)")
                .retrieve()
                // 404 = el RUC no existe en la fuente: no es una caída técnica.
                .onStatus(s -> s.value() == 404, (req, res) -> { })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("openruc HTTP " + res.getStatusCode().value());
                })
                .body(JsonNode.class);

        if (body == null || body.isNull()) return Optional.empty();
        JsonNode data = body.has("data") && body.get("data").isObject() ? body.get("data") : body;
        if (!texto(data, "ruc").isEmpty() && !texto(data, "ruc").equals(ruc)) return Optional.empty();

        String razon = primero(data, "razon_social", "razonSocial", "nombre");
        if (razon.isEmpty()) {
            // {"error":"not_found"} o cualquier respuesta sin razón social.
            if (data.hasNonNull("error")) log.debug("openruc sin datos para {}", enmascarar(ruc));
            return Optional.empty();
        }

        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setTipoDocumento("RUC");
        dto.setDocumento(ruc);
        dto.setNombre(razon);
        dto.setRazonSocial(razon);
        dto.setEstado(vacioANull(primero(data, "estado")));
        dto.setCondicion(vacioANull(primero(data, "condicion")));
        dto.setDireccion(vacioANull(primero(data, "direccion")));
        dto.setFuente("openruc.com · SUNAT (no oficial)");
        return Optional.of(dto);
    }

    private static String primero(JsonNode n, String... campos) {
        for (String c : campos) {
            String v = texto(n, c);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private static String texto(JsonNode n, String campo) {
        if (n == null) return "";
        JsonNode v = n.get(campo);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String enmascarar(String ruc) {
        return ruc == null || ruc.length() <= 4 ? "***" : ruc.substring(0, 2) + "********";
    }
}
