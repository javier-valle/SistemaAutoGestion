package com.autogestion.entity;

/**
 * Género del cliente. Es opcional: muchas fichas se llenan solo con lo
 * mínimo y el dato no identifica a la persona (sigue siendo (tipoDocumento,
 * documento) la identidad). Se usa para personalizar la vista del cliente.
 */
public enum Genero {
    MASCULINO,
    FEMENINO,
    OTROS;

    /** Acepta "MASCULINO", "FEMENINO", "OTROS" (con/sin acentos, minúsculas). */
    public static Genero desde(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim().toUpperCase()
                .replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
                .replace('Ó', 'O').replace('Ú', 'U');
        switch (t) {
            case "MASCULINO": case "M": case "HOMBRE": return MASCULINO;
            case "FEMENINO": case "F": case "MUJER": return FEMENINO;
            case "OTROS": case "O": case "OTRO": return OTROS;
            default: return null;
        }
    }
}
