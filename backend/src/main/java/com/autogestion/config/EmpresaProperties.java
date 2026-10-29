package com.autogestion.config;

import com.autogestion.util.DocumentoValidator;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Datos del emisor (el taller) y reglas fiscales. Se leen de
 * application*.properties y se congelan en cada comprobante.
 * Si el RUC configurado no pasa el dígito verificador, la app no arranca:
 * mejor fallar aquí con mensaje claro que emitir comprobantes inválidos.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.empresa")
public class EmpresaProperties {

    private String ruc;
    private String razonSocial;
    private String nombreComercial;
    private String direccion;
    private String ubigeo;
    private String telefono;
    private String email;
    /** Número de WhatsApp de atención (vacío = no se muestra el canal). */
    private String whatsapp;
    private BigDecimal igv = new BigDecimal("0.18");
    private Boolean preciosIncluyenIgv = true;
    private BigDecimal boletaUmbralDni = new BigDecimal("700.00");

    @PostConstruct
    public void validar() {
        if (!DocumentoValidator.rucValido(ruc)) {
            throw new IllegalStateException(
                    "app.empresa.ruc inválido ('" + ruc + "'): debe ser un RUC de 11 dígitos "
                    + "con prefijo 10/15/16/17/20 y dígito verificador correcto. "
                    + "Revisa application.properties o la variable APP_EMPRESA_RUC.");
        }
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalStateException("app.empresa.razon-social es obligatoria.");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalStateException("app.empresa.direccion es obligatoria.");
        }
    }
}
