package com.autogestion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error de regla de negocio explicado para humanos: qué pasó, en qué campo
 * y cómo corregirlo. El frontend lo muestra tal cual, sin tecnicismos.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String campo;
    private final String sugerencia;
    private final HttpStatus estado;

    public BusinessException(String mensaje, String campo, String sugerencia) {
        this(mensaje, campo, sugerencia, HttpStatus.BAD_REQUEST);
    }

    /** Atajo para errores sin campo asociado (ej. credenciales, estados). */
    public BusinessException(String mensaje, HttpStatus estado) {
        this(mensaje, null, null, estado);
    }

    /** Legado: mismo status que el RuntimeException al que reemplaza (400). */
    public BusinessException(String mensaje) {
        this(mensaje, null, null, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String mensaje, String campo, String sugerencia, HttpStatus estado) {
        super(mensaje);
        this.campo = campo;
        this.sugerencia = sugerencia;
        this.estado = estado;
    }
}
