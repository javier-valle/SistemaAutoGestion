package com.autogestion.dto;

import com.autogestion.entity.TipoDocumento;
import com.autogestion.util.DocumentoValidator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Alta y edición de clientes. La identidad de una persona es
 * (tipoDocumento, documento): el teléfono y el email NO identifican.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;

    /** DNI, RUC, CE o PASAPORTE */
    private String tipoDocumento;

    private String documento;

    private String razonSocial;

    private String direccion;

    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe empezar con 9 y tener 9 dígitos")
    private String telefono;

    @Email(message = "El email no tiene formato válido")
    @Size(max = 150, message = "El email es muy largo")
    private String email;

    /** MASCULINO, FEMENINO u OTROS (opcional; el vacío = sin especificar). */
    private String genero;

    @AssertTrue(message = "Tipo de documento inválido: usa DNI, RUC, CE o PASAPORTE")
    public boolean isTipoDocumentoValido() {
        if (tipoDocumento == null || tipoDocumento.isBlank()) return true;
        return TipoDocumento.desde(tipoDocumento) != null;
    }

    @AssertTrue(message = "Documento inválido para el tipo elegido")
    public boolean isDocumentoValido() {
        String tipo = (tipoDocumento == null || tipoDocumento.isBlank()) ? "DNI" : tipoDocumento;
        TipoDocumento td = TipoDocumento.desde(tipo);
        if (td == null) return true;
        if (documento == null || documento.isBlank()) return false;
        return DocumentoValidator.valido(td, documento);
    }

    @AssertTrue(message = "Con RUC la razón social y la dirección fiscal son obligatorias")
    public boolean isRazonSocialRequerida() {
        String tipo = (tipoDocumento == null || tipoDocumento.isBlank()) ? "DNI" : tipoDocumento;
        if (!"RUC".equalsIgnoreCase(tipo)) return true;
        return razonSocial != null && !razonSocial.isBlank()
                && direccion != null && !direccion.isBlank();
    }

    @AssertTrue(message = "Género inválido: usa MASCULINO, FEMENINO u OTROS")
    public boolean isGeneroValido() {
        return genero == null || genero.isBlank()
                || com.autogestion.entity.Genero.desde(genero) != null;
    }

    /** Tipo efectivo (por defecto DNI) ya normalizado. */
    public TipoDocumento tipoEfectivo() {
        if (tipoDocumento == null || tipoDocumento.isBlank()) return TipoDocumento.DNI;
        return TipoDocumento.desde(tipoDocumento);
    }

    /** Documento limpio: sin espacios ni guiones, CE/pasaporte en mayúsculas. */
    public String documentoNormalizado() {
        return DocumentoValidator.normalizar(documento);
    }
}
