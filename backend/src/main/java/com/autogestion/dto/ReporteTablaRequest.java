package com.autogestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Una tabla tal como se ve en pantalla, para exportarla con el mismo formato
 * profesional de los reportes. El cliente no calcula totales: solo envía los
 * valores y su tipo; el backend decide cómo pintarlos.
 */
@Data
public class ReporteTablaRequest {

    @NotBlank(message = "El título de la tabla es obligatorio.")
    @Size(max = 120, message = "El título no puede pasar de 120 caracteres.")
    private String titulo;

    @Size(max = 300, message = "La nota no puede pasar de 300 caracteres.")
    private String nota;

    @NotEmpty(message = "La tabla necesita al menos una columna.")
    @Size(max = 30, message = "La tabla no puede tener más de 30 columnas.")
    @Valid
    private List<Columna> columnas;

    @NotEmpty(message = "La tabla no tiene filas para exportar.")
    @Size(max = 20000, message = "La tabla supera el máximo de 20000 filas.")
    private List<List<Object>> filas;

    @Data
    public static class Columna {

        @NotBlank(message = "Cada columna necesita un título.")
        @Size(max = 80, message = "El título de la columna no puede pasar de 80 caracteres.")
        private String titulo;

        /** TEXTO, ENTERO, DINERO, DECIMAL o PROMEDIO. Por defecto TEXTO. */
        private String tipo;

        private boolean totaliza;
    }
}
