package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.TipoDocumento;

import java.util.Optional;

/**
 * Fuente de datos de documentos.
 * Las implementaciones nunca deben lanzar excepciones por "no encontrado":
 * devuelven Optional.empty() y el servicio prueba con la siguiente fuente.
 */
public interface ProveedorDocumento {

    boolean soporta(TipoDocumento tipo);

    /**
     * false = la fuente no está configurada (por ejemplo, le falta el token).
     * El servicio la salta sin hacer la llamada externa y lo informa como
     * "sin fuente" en vez de "no encontrado".
     */
    default boolean disponible() {
        return true;
    }

    Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero);
}
