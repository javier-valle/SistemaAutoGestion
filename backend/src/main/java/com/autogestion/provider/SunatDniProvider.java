package com.autogestion.provider;

import com.autogestion.dto.ConsultaDocumentoResponseDTO;
import com.autogestion.entity.PadronRuc;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.repository.PadronRucRepository;
import com.autogestion.util.documento.ReglaRuc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Nombres de una persona natural a partir de su DNI usando el Padrón Reducido
 * RUC de SUNAT que ya carga la aplicación para consultar RUC.
 *
 * Cómo funciona: toda persona natural con RUC tiene un RUC que empieza en 10,
 * sigue con sus 8 dígitos de DNI y termina en el dígito verificador. Buscamos
 * ese RUC en la tabla local {@code padron_ruc}; si existe, la razón social es
 * el nombre publicado por SUNAT.
 *
 * Ventajas:
 *  - Sin API key, sin token y sin llamadas a terceros: el dato ya está en la BD.
 *  - Fuente oficial y pública (SUNAT), no un sitio que scrapea a RENIEC.
 *
 * Límites que el código no oculta:
 *  - Solo cubre personas que tengan RUC 10 (no todos los DNI lo tienen).
 *  - No es RENIEC ni una verificación de identidad: el nombre puede estar
 *    desactualizado y la persona no ha demostrado ser quien dice ser.
 *  - Si el padrón no se importó, el proveedor se declara no disponible y el
 *    portal sigue con la siguiente fuente o con el modo manual.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class SunatDniProvider implements ProveedorDocumento {

    private static final long TTL_DISPONIBILIDAD_MS = 5L * 60 * 1000;

    private final PadronRucRepository repository;

    private volatile boolean hayPadron;
    private volatile long ultimaComprobacion;

    @Override
    public boolean soporta(TipoDocumento tipo) {
        return tipo == TipoDocumento.DNI;
    }

    @Override
    public boolean disponible() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimaComprobacion > TTL_DISPONIBILIDAD_MS) {
            try {
                hayPadron = repository.count() > 0;
            } catch (Exception e) {
                hayPadron = false;
                log.warn("No se pudo comprobar el padrón SUNAT para DNI: {}", e.getMessage());
            }
            ultimaComprobacion = ahora;
        }
        return hayPadron;
    }

    @Override
    public Optional<ConsultaDocumentoResponseDTO> consultar(TipoDocumento tipo, String numero) {
        String dni = numero == null ? "" : numero.trim();
        if (!dni.matches("\\d{8}")) return Optional.empty();
        // Si el padrón no está cargado, no vale la pena consultar la tabla.
        if (!disponible()) return Optional.empty();

        String base = "10" + dni;
        int dv = ReglaRuc.digitoVerificador(base);
        if (dv < 0) return Optional.empty();
        String ruc = base + dv;

        return repository.findById(ruc)
                .filter(p -> p.getRazonSocial() != null && !p.getRazonSocial().isBlank())
                .map(p -> aDto(p, dni));
    }

    private static ConsultaDocumentoResponseDTO aDto(PadronRuc p, String dni) {
        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setTipoDocumento("DNI");
        dto.setDocumento(dni);
        dto.setNombre(p.getRazonSocial().trim());
        dto.setRazonSocial(p.getRazonSocial().trim());
        dto.setDireccion(p.getDireccion());
        dto.setEstado(p.getEstado());
        dto.setCondicion(p.getCondicion());
        dto.setFuente("SUNAT · Padrón Reducido (RUC 10)");
        return dto;
    }
}
