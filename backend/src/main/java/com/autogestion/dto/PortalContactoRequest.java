package com.autogestion.dto;

import lombok.Data;

/**
 * Datos de contacto que el cliente puede actualizar por su cuenta.
 * Nombre y documento NO se editan desde el portal: los administra el taller
 * (el documento identifica la cuenta y no debe poder cambiarse solo).
 */
@Data
public class PortalContactoRequest {
    private String telefono;
    private String email;
}
