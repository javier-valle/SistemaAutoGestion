package com.autogestion.dto;

/**
 * Datos públicos del taller para la guía y el centro de soporte.
 * Solo información comercial/configurada: nunca datos de clientes.
 */
public record PortalInfoDTO(
        String nombreComercial,
        String razonSocial,
        String telefono,
        String email,
        String whatsapp,
        String direccion) { }
