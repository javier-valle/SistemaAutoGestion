package com.autogestion.dto;

import lombok.Data;

/**
 * Resultado de consultar un documento en una fuente de datos.
 * Ojo: "verificado" significa que una fuente externa devolvió el nombre,
 * NO que el usuario haya demostrado ser esa persona. La verificación de
 * identidad oficial (biometría/RENIEC) es otro trámite.
 */
@Data
public class ConsultaDocumentoResponseDTO {

    /** La fuente devolvió datos. */
    public static final String MOTIVO_OK = "OK";
    /** No hay ninguna fuente configurada/activa para ese tipo de documento. */
    public static final String MOTIVO_SIN_FUENTE = "SIN_FUENTE";
    /** La fuente respondió, pero no tiene ese documento. */
    public static final String MOTIVO_NO_ENCONTRADO = "NO_ENCONTRADO";
    /** La fuente falló (red, CORS del proveedor, 5xx, timeout). */
    public static final String MOTIVO_ERROR = "ERROR";
    /** Demasiadas consultas desde el mismo origen. */
    public static final String MOTIVO_LIMITE = "LIMITE";
    /** Formato de documento inválido: ni siquiera se consultó. */
    public static final String MOTIVO_INVALIDO = "INVALIDO";

    private String tipoDocumento;
    private String documento;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String razonSocial;
    private String direccion;
    private String estado;
    private String condicion;
    /** true = los datos salieron de una fuente externa; false = el usuario debe declararlos. */
    private boolean verificado;
    /** Nombre legible de la fuente que respondió (nunca se inventa). */
    private String fuente;
    /** Uno de los MOTIVO_* de esta clase. */
    private String motivo;

    /** Sin datos: el portal pasa al modo manual. */
    public static ConsultaDocumentoResponseDTO vacio() {
        return vacio(MOTIVO_SIN_FUENTE);
    }

    public static ConsultaDocumentoResponseDTO vacio(String motivo) {
        ConsultaDocumentoResponseDTO dto = new ConsultaDocumentoResponseDTO();
        dto.setMotivo(motivo);
        dto.setVerificado(false);
        return dto;
    }
}
