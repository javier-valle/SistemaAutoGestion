package com.autogestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RecepcionCompletaRequest {

    /** Si viene, se usa el cliente existente (verificado) en vez de buscar/crear. */
    private Long clienteId;

    /** Si viene, se usa el vehículo existente (verificando que sea del cliente). */
    private Long vehiculoId;

    /**
     * Solo obligatorio cuando NO viene clienteId (cliente nuevo).
     * Se valida en el servicio para dar error con campo y sugerencia.
     */
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String clienteNombre;

    /** DNI, RUC, CE o PASAPORTE. Opcional: por defecto DNI. */
    private String clienteTipoDocumento;

    private String clienteDocumento;

    @Pattern(regexp = "^$|^9\\d{8}$", message = "El celular debe empezar con 9 y tener 9 dígitos")
    private String clienteTelefono;

    @Email(message = "El email no tiene formato válido")
    private String clienteEmail;

    private String clienteRazonSocial;

    private String clienteDireccion;

    /** Género del cliente nuevo: MASCULINO, FEMENINO u OTROS (opcional). */
    private String clienteGenero;

    /**
     * Solo obligatoria cuando NO viene vehiculoId. Se valida en el servicio.
     */
    @Pattern(regexp = "^[A-Za-z0-9]{3}-?[A-Za-z0-9]{3}$",
            message = "Placa peruana: 3 letras/números, guion opcional y 3 más (ej. ABC-123)")
    private String vehiculoPlaca;

    private String vehiculoMarca;

    private String vehiculoModelo;

    private Integer vehiculoAnio;

    @NotNull(message = "Describe el problema reportado")
    @Size(min = 5, max = 2000, message = "Describe el problema con al menos 5 letras")
    private String problemaReportado;

    /** VACIO, CUARTO, MEDIO, TRES_CUARTOS, LLENO */
    private String nivelCombustible;

    @Size(max = 500, message = "Daños previos muy largos")
    private String danosPrevios;

    @Size(max = 500, message = "Accesorios muy largos")
    private String accesorios;

    private Integer kilometraje;
}
