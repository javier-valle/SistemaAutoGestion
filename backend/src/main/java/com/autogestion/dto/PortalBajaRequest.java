package com.autogestion.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmación explícita para dar de baja la cuenta del portal. */
public class PortalBajaRequest {

    @NotBlank(message = "Escribe ELIMINAR para confirmar la baja de tu cuenta.")
    private String confirmacion;

    public String getConfirmacion() {
        return confirmacion;
    }

    public void setConfirmacion(String confirmacion) {
        this.confirmacion = confirmacion;
    }
}
