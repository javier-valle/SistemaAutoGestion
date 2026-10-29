package com.autogestion.entity;

/** Estados de una solicitud de cotización hecha por el cliente. */
public enum EstadoSolicitudCotizacion {
    PENDIENTE,
    ATENDIDA,
    RECHAZADA;

    public static EstadoSolicitudCotizacion desde(String codigo) {
        if (codigo == null) return null;
        try {
            return EstadoSolicitudCotizacion.valueOf(codigo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
