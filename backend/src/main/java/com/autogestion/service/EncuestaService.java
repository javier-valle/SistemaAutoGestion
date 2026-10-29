package com.autogestion.service;

import com.autogestion.dto.EncuestaAdminDTO;
import com.autogestion.entity.EncuestaSatisfaccion;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Vehiculo;
import com.autogestion.repository.EncuestaSatisfaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta de las encuestas que los clientes dejan en el portal. */
@Service
@RequiredArgsConstructor
public class EncuestaService {

    private final EncuestaSatisfaccionRepository encuestaSatisfaccionRepository;

    @Transactional(readOnly = true)
    public List<EncuestaAdminDTO> listar() {
        return encuestaSatisfaccionRepository.findAllByOrderByCreadoEnDesc().stream()
                .map(this::aDto)
                .toList();
    }

    private EncuestaAdminDTO aDto(EncuestaSatisfaccion e) {
        OrdenTrabajo ot = e.getOrdenTrabajo();
        Vehiculo v = ot == null ? null : ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo();
        return new EncuestaAdminDTO(
                e.getId(),
                e.getCliente() == null ? null : e.getCliente().nombreFiscal(),
                e.getCliente() == null ? null : e.getCliente().getDocumento(),
                ot == null ? null : ot.getId(),
                v == null ? null : v.getPlaca(),
                v == null ? null : vehiculoTexto(v),
                e.getPuntuacion(),
                e.getComentario(),
                e.getCreadoEn());
    }

    private static String vehiculoTexto(Vehiculo v) {
        String marca = v.getMarca() == null ? "" : v.getMarca().trim();
        String modelo = v.getModelo() == null ? "" : v.getModelo().trim();
        String nombre = (marca + " " + modelo).trim();
        if (v.getAnio() != null) {
            nombre = (nombre + " " + v.getAnio()).trim();
        }
        return nombre.isBlank() ? v.getPlaca() : nombre;
    }
}
