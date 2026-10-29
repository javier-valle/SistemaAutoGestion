package com.autogestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fila del Padrón Reducido RUC de SUNAT (dato público), cargado en la BD local. */
@Entity
@Table(name = "padron_ruc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PadronRuc {

    @Id
    @Column(length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 300)
    private String razonSocial;

    @Column(length = 50)
    private String estado;

    @Column(length = 50)
    private String condicion;

    @Column(length = 500)
    private String direccion;
}
