package com.autogestion.service;

import com.autogestion.dto.SolicitudCotizacionRequest;
import com.autogestion.dto.SolicitudCotizacionResponseDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.SolicitudCotizacionRepository;
import com.autogestion.repository.VehiculoRepository;
import com.autogestion.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Solicitudes de cotización del portal.
 *
 * El cliente solo pide una revisión: no fija precios ni crea una cotización
 * contable. El taller recibe la solicitud, registra la recepción, hace el
 * diagnóstico y recién entonces arma la cotización real.
 */
@Service
@RequiredArgsConstructor
public class SolicitudCotizacionService {

    /** Evita que un cliente llene la bandeja del taller con avisos repetidos. */
    private static final int MAX_PENDIENTES_POR_CLIENTE = 3;

    private final SolicitudCotizacionRepository solicitudRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    @Transactional
    public SolicitudCotizacionResponseDTO crear(Long clienteId, SolicitudCotizacionRequest req) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException("Cliente no encontrado.", HttpStatus.NOT_FOUND));

        Vehiculo vehiculo = vehiculoRepository.findById(req.getVehiculoId())
                .orElseThrow(() -> new BusinessException("El vehículo elegido no existe.", "vehiculoId",
                        "Elige un vehículo de tu lista.", HttpStatus.NOT_FOUND));

        if (!vehiculo.getCliente().getId().equals(clienteId)) {
            // No se revela a quién pertenece la placa.
            throw new BusinessException("Ese vehículo no está registrado a tu nombre.", "vehiculoId",
                    "Elige un vehículo de tu lista o consulta con el taller.", HttpStatus.FORBIDDEN);
        }

        String descripcion = req.getDescripcion() == null ? "" : req.getDescripcion().trim().replaceAll("\\s+", " ");
        if (descripcion.length() < 10) {
            throw new BusinessException("Cuéntanos un poco más: mínimo 10 caracteres.", "descripcion",
                    "Ejemplo: ruido al frenar y vibración en la dirección.");
        }

        if (solicitudRepository.existsByVehiculoIdAndEstado(vehiculo.getId(), EstadoSolicitudCotizacion.PENDIENTE)) {
            throw new BusinessException("Ya tienes una solicitud pendiente para la placa " + vehiculo.getPlaca()
                    + ". El taller la revisará y te responderá aquí.", HttpStatus.CONFLICT);
        }

        long pendientes = solicitudRepository.countByClienteIdAndEstado(clienteId, EstadoSolicitudCotizacion.PENDIENTE);
        if (pendientes >= MAX_PENDIENTES_POR_CLIENTE) {
            throw new BusinessException("Tienes " + MAX_PENDIENTES_POR_CLIENTE
                    + " solicitudes pendientes. Espera la respuesta del taller antes de crear otra.",
                    HttpStatus.CONFLICT);
        }

        SolicitudCotizacion solicitud = SolicitudCotizacion.builder()
                .cliente(cliente)
                .vehiculo(vehiculo)
                .descripcion(descripcion)
                .estado(EstadoSolicitudCotizacion.PENDIENTE)
                .creadoEn(AppTime.ahora())
                .actualizadoEn(AppTime.ahora())
                .build();

        return aDTO(solicitudRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudCotizacionResponseDTO> listarDeCliente(Long clienteId) {
        if (clienteId == null) return List.of();
        return solicitudRepository.findByClienteIdOrderByCreadoEnDesc(clienteId).stream()
                .map(this::aDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudCotizacionResponseDTO> listar(String estado) {
        if (estado == null || estado.isBlank()) {
            return solicitudRepository.findAllByOrderByCreadoEnDesc().stream().map(this::aDTO).toList();
        }
        EstadoSolicitudCotizacion filtro = EstadoSolicitudCotizacion.desde(estado);
        if (filtro == null) return List.of();
        return solicitudRepository.findByEstadoOrderByCreadoEnDesc(filtro).stream().map(this::aDTO).toList();
    }

    @Transactional
    public SolicitudCotizacionResponseDTO atender(Long id, String nota) {
        return resolver(id, EstadoSolicitudCotizacion.ATENDIDA, nota);
    }

    @Transactional
    public SolicitudCotizacionResponseDTO rechazar(Long id, String nota) {
        return resolver(id, EstadoSolicitudCotizacion.RECHAZADA, nota);
    }

    private SolicitudCotizacionResponseDTO resolver(Long id, EstadoSolicitudCotizacion nuevo, String nota) {
        SolicitudCotizacion solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada.", HttpStatus.NOT_FOUND));

        if (solicitud.getEstado() != EstadoSolicitudCotizacion.PENDIENTE) {
            throw new BusinessException("Esta solicitud ya fue atendida.", HttpStatus.CONFLICT);
        }

        String limpia = nota == null ? null : nota.trim().replaceAll("\\s+", " ");
        if (limpia != null && limpia.length() > 500) {
            throw new BusinessException("La nota no puede superar 500 caracteres.", "nota",
                    "Resume la respuesta en una o dos frases.");
        }

        solicitud.setEstado(nuevo);
        solicitud.setNotaTaller(limpia == null || limpia.isEmpty() ? null : limpia);
        solicitud.setActualizadoEn(AppTime.ahora());
        return aDTO(solicitudRepository.save(solicitud));
    }

    private SolicitudCotizacionResponseDTO aDTO(SolicitudCotizacion s) {
        return SolicitudCotizacionResponseDTO.builder()
                .id(s.getId())
                .vehiculoId(s.getVehiculo().getId())
                .vehiculoPlaca(s.getVehiculo().getPlaca())
                .clienteNombre(s.getCliente().nombreFiscal())
                .descripcion(s.getDescripcion())
                .estado(s.getEstado().name())
                .notaTaller(s.getNotaTaller())
                .creadoEn(s.getCreadoEn())
                .actualizadoEn(s.getActualizadoEn())
                .build();
    }
}
