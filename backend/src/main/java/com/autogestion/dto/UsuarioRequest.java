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
 * Alta y edición de personal. En edición, `password` es opcional
 * (vacío = no cambiar); al crear es obligatoria (mínimo 8).
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class UsuarioRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Nombres muy largos")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Apellidos muy largos")
    private String apellidos;

    private String tipoDocumento;

    private String documento;

    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe empezar con 9 y tener 9 dígitos")
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String email;

    /** ADMIN, MECANICO, ALMACENERO o RECEPCIONISTA */
    @NotBlank(message = "El rol es obligatorio")
    private String rol;

    /** Obligatoria si el rol es MECANICO */
    private String especialidad;

    private String password;

    @AssertTrue(message = "Documento inválido para el tipo elegido")
    public boolean isDocumentoValido() {
        if (documento == null || documento.isBlank()) return true;
        String tipo = (tipoDocumento == null || tipoDocumento.isBlank()) ? "DNI" : tipoDocumento;
        TipoDocumento td = TipoDocumento.desde(tipo);
        if (td == null) return false;
        return DocumentoValidator.valido(td, documento);
    }

    @AssertTrue(message = "El mecánico necesita especialidad")
    public boolean isEspecialidadRequerida() {
        if (!"MECANICO".equalsIgnoreCase(rol)) return true;
        return especialidad != null && !especialidad.isBlank();
    }
}
