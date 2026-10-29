package com.autogestion.service;

import com.autogestion.dto.PagoEntregaResponseDTO;
import com.autogestion.dto.PagoRequest;
import com.autogestion.entity.EstadoRecepcion;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.PagoEntrega;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.PagoEntregaRepository;
import com.autogestion.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PagoEntregaService {

    private final PagoEntregaRepository pagoEntregaRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;

    @Transactional
    public PagoEntregaResponseDTO registrarPago(PagoRequest request) {
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a 0.", "monto", null);
        }

        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(request.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));

        Optional<PagoEntrega> existente = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId());
        if (existente.isPresent()) {
            throw new BusinessException("Ya existe un registro de pago para esta OT.", "ordenTrabajoId",
                    null, HttpStatus.CONFLICT);
        }

        PagoEntrega pago = PagoEntrega.builder()
                .ordenTrabajo(ot)
                .monto(request.getMonto())
                .fechaPago(AppTime.ahora())
                .build();
        pago = pagoEntregaRepository.save(pago);
        return toResponseDTO(pago);
    }

    @Transactional
    public PagoEntregaResponseDTO registrarEntrega(Long ordenTrabajoId) {
        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(ordenTrabajoId)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));

        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId())
                .orElseThrow(() -> new BusinessException("Debe registrar pago antes de entrega.", "ordenTrabajoId",
                        "Cobra la OT primero.", HttpStatus.CONFLICT));

        if (pago.getFechaEntrega() != null) {
            throw new BusinessException("Esta OT ya fue entregada.", "ordenTrabajoId",
                    null, HttpStatus.CONFLICT);
        }

        pago.setFechaEntrega(AppTime.ahora());

        var recepcion = ot.getCotizacion().getDiagnostico().getRecepcion();
        recepcion.setEstado(EstadoRecepcion.ENTREGADA);

        pago = pagoEntregaRepository.save(pago);
        return toResponseDTO(pago);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerMonto(Long ordenTrabajoId) {
        OrdenTrabajo ot = ordenTrabajoRepository.findById(ordenTrabajoId)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));
        return ot.getCotizacion().getTotal();
    }

    @Transactional(readOnly = true)
    public Optional<PagoEntregaResponseDTO> obtenerPorOT(Long ordenTrabajoId) {
        return pagoEntregaRepository.findByOrdenTrabajoId(ordenTrabajoId)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public PagoEntregaResponseDTO toResponseDTO(PagoEntrega pago) {
        return PagoEntregaResponseDTO.builder()
                .id(pago.getId())
                .ordenTrabajoId(pago.getOrdenTrabajo().getId())
                .monto(pago.getMonto())
                .fechaPago(pago.getFechaPago())
                .fechaEntrega(pago.getFechaEntrega())
                .build();
    }
}