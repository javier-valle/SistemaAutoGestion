package com.autogestion.dto;

import com.autogestion.entity.TipoDocumento;
import com.autogestion.util.DocumentoValidator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PortalRegistroRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;

    @NotBlank(message = "El documento es obligatorio")
    private String documento;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String razonSocial;

    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe empezar con 9 y tener 9 dígitos")
    private String telefono;

    @Email(message = "El email no tiene formato válido")
    private String email;

    private String placa;
    private String marca;
    private String modelo;
    private Integer anio;

    private String direccionFiscal;

    @AssertTrue(message = "Debes aceptar las políticas de privacidad")
    private boolean consentimiento = false;

    @AssertTrue(message = "Tipo de documento inválido")
    public boolean isTipoDocumentoValido() {
        return TipoDocumento.desde(tipoDocumento) != null;
    }

    @AssertTrue(message = "Documento inválido para el tipo elegido")
    public boolean isDocumentoValido() {
        TipoDocumento td = TipoDocumento.desde(tipoDocumento);
        return td != null && DocumentoValidator.valido(td, documento);
    }

    @AssertTrue(message = "Con RUC la razón social y la dirección fiscal son obligatorias")
    public boolean isRazonSocialRequerida() {
        if (!"RUC".equalsIgnoreCase(tipoDocumento)) return true;
        return razonSocial != null && !razonSocial.isBlank()
                && direccionFiscal != null && !direccionFiscal.isBlank();
    }

    public TipoDocumento tipoEfectivo() {
        return TipoDocumento.desde(tipoDocumento);
    }

    public String documentoNormalizado() {
        return DocumentoValidator.normalizar(documento);
    }
}
