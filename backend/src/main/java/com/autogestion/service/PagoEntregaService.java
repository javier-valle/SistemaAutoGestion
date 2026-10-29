package com.autogestion.service;

import com.autogestion.dto.PagoEntregaResponseDTO;
import com.autogestion.dto.PagoRequest;
import com.autogestion.entity.Comprobante;
import com.autogestion.entity.EstadoOT;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PagoEntregaService {

    private final PagoEntregaRepository pagoEntregaRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;

    @Transactional
    public PagoEntregaResponseDTO registrarPago(PagoRequest request) {
        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(request.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));
        return toResponseDTO(registrarPago(ot, request.getMonto()));
    }

    /** Cobra la OT. Solo se cobra una orden FINALIZADA: no se cobra trabajo abierto. */
    @Transactional
    public PagoEntrega registrarPago(OrdenTrabajo ot, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto debe ser mayor a 0.", "monto", null);
        }
        exigirFinalizada(ot);

        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId())
                .orElseGet(() -> PagoEntrega.builder().ordenTrabajo(ot).build());
        if (pago.getFechaPago() != null) {
            throw new BusinessException("Esta OT ya tiene un pago registrado el " + pago.getFechaPago() + ".",
                    "ordenTrabajoId", null, HttpStatus.CONFLICT);
        }
        pago.setMonto(monto);
        pago.setFechaPago(AppTime.ahora());
        return pagoEntregaRepository.save(pago);
    }

    /** Pago ligado al comprobante recién emitido: el monto cobrado queda en el snapshot. */
    @Transactional
    public PagoEntrega registrarPagoDeComprobante(OrdenTrabajo ot, BigDecimal monto, Comprobante comprobante) {
        exigirFinalizada(ot);
        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId())
                .orElseGet(() -> PagoEntrega.builder().ordenTrabajo(ot).build());
        pago.setMonto(monto);
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(AppTime.ahora());
        }
        pago.setComprobante(comprobante);
        return pagoEntregaRepository.save(pago);
    }

    @Transactional
    public PagoEntregaResponseDTO registrarEntrega(Long ordenTrabajoId) {
        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(ordenTrabajoId)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));
        return toResponseDTO(registrarEntrega(ot));
    }

    /**
     * Entrega física del vehículo: exige pago previo y cierra la OT (fechaFin)
     * y la recepción en una sola pasada.
     */
    @Transactional
    public PagoEntrega registrarEntrega(OrdenTrabajo ot) {
        exigirFinalizada(ot);

        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId())
                .orElseThrow(() -> new BusinessException("Debe registrar pago antes de entrega.", "ordenTrabajoId",
                        "Cobra la OT primero.", HttpStatus.CONFLICT));
        if (pago.getFechaPago() == null) {
            throw new BusinessException("Debe registrar pago antes de entrega.", "ordenTrabajoId",
                    "Cobra la OT primero.", HttpStatus.CONFLICT);
        }
        if (pago.getFechaEntrega() != null) {
            throw new BusinessException("Esta OT ya fue entregada.", "ordenTrabajoId",
                    null, HttpStatus.CONFLICT);
        }

        pago.setFechaEntrega(AppTime.ahora());
        if (ot.getFechaFin() == null) {
            ot.setFechaFin(pago.getFechaEntrega());
        }

        var recepcion = ot.getCotizacion().getDiagnostico().getRecepcion();
        recepcion.setEstado(EstadoRecepcion.ENTREGADA);

        return pagoEntregaRepository.save(pago);
    }

    /**
     * Al anular, el pago deja de apuntar al documento sin validez. El comprobante
     * anulado sigue existiendo como historial y la OT puede emitir otro.
     */
    @Transactional
    public void desvincularComprobante(Comprobante comprobante) {
        if (comprobante == null || comprobante.getOrdenTrabajo() == null) return;
        pagoEntregaRepository.findByOrdenTrabajoId(comprobante.getOrdenTrabajo().getId())
                .filter(p -> p.getComprobante() != null
                        && p.getComprobante().getId().equals(comprobante.getId()))
                .ifPresent(p -> {
                    p.setComprobante(null);
                    pagoEntregaRepository.save(p);
                });
    }

    /** Entrega idempotente para el flujo de emisión: no falla si ya estaba entregada. */
    @Transactional
    public PagoEntrega asegurarEntrega(OrdenTrabajo ot) {
        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId())
                .orElseThrow(() -> new BusinessException("Debe registrar pago antes de entrega.", "ordenTrabajoId",
                        "Cobra la OT primero.", HttpStatus.CONFLICT));
        return pago.getFechaEntrega() != null ? pago : registrarEntrega(ot);
    }

    private void exigirFinalizada(OrdenTrabajo ot) {
        if (ot.getEstado() != EstadoOT.FINALIZADA) {
            throw new BusinessException("La OT #" + ot.getId() + " aún no está finalizada.", "ordenTrabajoId",
                    "Finaliza la OT antes de cobrar o entregar.", HttpStatus.CONFLICT);
        }
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
