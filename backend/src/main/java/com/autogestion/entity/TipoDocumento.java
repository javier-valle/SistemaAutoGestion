package com.autogestion.entity;

/** Tipo de documento de identidad (catálogo 06 SUNAT entre paréntesis). */
public enum TipoDocumento {
    DNI,
    RUC,
    CE,
    PASAPORTE;

    public static TipoDocumento desde(String codigo) {
        if (codigo == null) return null;
        try {
            return TipoDocumento.valueOf(codigo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
