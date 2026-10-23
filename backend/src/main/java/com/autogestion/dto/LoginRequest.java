package com.autogestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "El email es obligatorio")
    private String email;
    @NotBlank(message = "La clave es obligatoria")
    private String password;
}
