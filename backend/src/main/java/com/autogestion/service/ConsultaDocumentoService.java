package com.autogestion.service;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.provider.ProveedorDocumento;
import com.autogestion.util.DocumentoValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consulta de documentos con las fuentes configuradas.
 *
 * Importante: esta clase NO inventa nombres ni usa datos de ejemplo. Si no hay
 * una fuente disponible (por ejemplo, falta el token de RENIEC), responde
 * motivo=SIN_FUENTE y el portal deja que el usuario declare sus datos,
 * marcando el documento como NO verificado.
 *
 * Diferencias que el código respeta:
 *  - formato válido (DocumentoValidator) ≠ documento existente
 *  - documento existente ≠ identidad verificada
 *  - nombre devuelto por una fuente externa ≠ verificación oficial de identidad
 */
@Service
@Slf4j
public class ConsultaDocumentoService {

    /** Un resultado con datos se conserva 24 h: el nombre de una persona no cambia a cada rato. */
    private static final long TTL_POSITIVO_MS = 24L * 60 * 60 * 1000;
    /** "No encontrado" se conserva 5 min: evita repetir la misma consulta fallida. */
    private static final long TTL_NEGATIVO_MS = 5L * 60 * 1000;
    /** Error de la fuente: se reintenta pronto, pero no en cada tecla. */
    private static final long TTL_ERROR_MS = 60L * 1000;
    /** Sin fuente configurada: no cambia hasta reiniciar, se conserva 10 min. */
    private static final long TTL_SIN_FUENTE_MS = 10L * 60 * 1000;
    private static final int CACHE_MAX = 5000;

    private final List<ProveedorDocumento> proveedores;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public ConsultaDocumentoService(List<ProveedorDocumento> proveedores) {
        this.proveedores = proveedores;
    }

    public ConsultaDocumentoResponseDTO consultar(String tipoDocumento, String numero) {
        TipoDocumento tipo = TipoDocumento.desde(tipoDocumento);
        String doc = DocumentoValidator.normalizar(numero);

        if (tipo == null || !DocumentoValidator.valido(tipo, doc)) {
            ConsultaDocumentoResponseDTO invalido = ConsultaDocumentoResponseDTO.vacio(ConsultaDocumentoResponseDTO.MOTIVO_INVALIDO);
            invalido.setTipoDocumento(tipo == null ? null : tipo.name());
            invalido.setDocumento(doc);
            return invalido;
        }

        String clave = tipo + ":" + doc;
        Cached enCache = cache.get(clave);
        if (enCache != null && enCache.expira > System.currentTimeMillis()) {
            return enCache.respuesta;
        }

        boolean algunaFuenteDisponible = false;
        boolean algunaFuenteFallo = false;

        for (ProveedorDocumento p : proveedores) {
            if (!p.soporta(tipo)) continue;
            if (!p.disponible()) {
                log.debug("Proveedor {} sin configurar para {}", p.getClass().getSimpleName(), tipo);
                continue;
            }
            algunaFuenteDisponible = true;
            try {
                Optional<ConsultaDocumentoResponseDTO> r = p.consultar(tipo, doc);
                if (r.isPresent()) {
                    ConsultaDocumentoResponseDTO dto = r.get();
                    dto.setVerificado(true);
                    dto.setMotivo(ConsultaDocumentoResponseDTO.MOTIVO_OK);
                    dto.setTipoDocumento(tipo.name());
                    dto.setDocumento(doc);
                    guardar(clave, dto, TTL_POSITIVO_MS);
                    log.info("Documento {} resuelto por {}", enmascarar(doc), p.getClass().getSimpleName());
                    return dto;
                }
            } catch (Exception e) {
                algunaFuenteFallo = true;

                log.warn("Proveedor {} falló con documento {}: {}",
                        p.getClass().getSimpleName(), enmascarar(doc), e.getMessage());
            }
        }

        if (!algunaFuenteDisponible) {
            ConsultaDocumentoResponseDTO r = ConsultaDocumentoResponseDTO.vacio(ConsultaDocumentoResponseDTO.MOTIVO_SIN_FUENTE);
            r.setTipoDocumento(tipo.name());
            r.setDocumento(doc);
            guardar(clave, r, TTL_SIN_FUENTE_MS);
            log.info("Documento {}: no hay fuente configurada para {} (modo manual)", enmascarar(doc), tipo);
            return r;
        }

        ConsultaDocumentoResponseDTO r = ConsultaDocumentoResponseDTO.vacio(
                algunaFuenteFallo ? ConsultaDocumentoResponseDTO.MOTIVO_ERROR : ConsultaDocumentoResponseDTO.MOTIVO_NO_ENCONTRADO);
        r.setTipoDocumento(tipo.name());
        r.setDocumento(doc);
        guardar(clave, r, algunaFuenteFallo ? TTL_ERROR_MS : TTL_NEGATIVO_MS);
        log.info("Documento {} sin datos ({}); el usuario deberá declararlos", enmascarar(doc), r.getMotivo());
        return r;
    }

    private void guardar(String clave, ConsultaDocumentoResponseDTO respuesta, long ttlMs) {
        if (cache.size() >= CACHE_MAX) cache.clear();
        cache.put(clave, new Cached(System.currentTimeMillis() + ttlMs, respuesta));
    }

    private static String enmascarar(String doc) {
        return doc == null || doc.length() <= 3 ? "***" : doc.substring(0, 3) + "****";
    }

    private record Cached(long expira, ConsultaDocumentoResponseDTO respuesta) { }
}
