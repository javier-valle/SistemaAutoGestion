package com.autogestion.service;

import com.autogestion.config.EmpresaProperties;
import com.autogestion.dto.ComprobanteEmitirRequest;
import com.autogestion.dto.ComprobanteResponseDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import com.autogestion.service.comprobante.AdquirenteResuelto;
import com.autogestion.service.comprobante.ComprobanteMapper;
import com.autogestion.service.comprobante.ConstructorDetalle;
import com.autogestion.service.comprobante.OrdenSeguro;
import com.autogestion.service.comprobante.ResolutorAdquirente;
import com.autogestion.util.AppTime;
import com.autogestion.util.NumeroALetras;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Emite boletas y facturas con la estructura de un comprobante SUNAT real
 * (cabecera, detalle, IGV 18 %, total en letras, folio serie-numero).
 * NO firma XML ni envía a SUNAT: es representación académica imprimible.
 *
 * SOLID: solo orquesta la transacción y delega cada responsabilidad:
 * adquirente ({@link ResolutorAdquirente}), detalle y totales
 * ({@link ConstructorDetalle}), forma del DTO ({@link ComprobanteMapper})
 * y ORDER BY blindado ({@link OrdenSeguro}). Depende de repositorios
 * (abstracciones) y especialistas, no de detalles concretos (DIP).
 */
@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private static final Map<String, String> COLUMNAS_ORDEN = Map.of(
            "fechaEmision", "fecha_emision",
            "id", "id",
            "numero", "numero",
            "total", "total");

    private final ComprobanteRepository comprobanteRepository;
    private final SerieComprobanteRepository serieRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaProperties empresa;
    private final PagoEntregaService pagoEntregaService;
    private final ResolutorAdquirente adquirente;
    private final ConstructorDetalle detalle;

    /** Una sola transacción: serie, comprobante, pago y entrega salen juntos o nada sale. */
    @Transactional
    public ComprobanteResponseDTO emitir(ComprobanteEmitirRequest req, Long usuarioId) {
        TipoComprobante tipo = TipoComprobante.desde(req.getTipo());
        if (tipo == null) {
            throw new BusinessException("Elige boleta o factura.", "tipo",
                    "Boleta es para consumidor final; factura para empresas con RUC.");
        }
        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(req.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("La orden de trabajo no existe.", "ordenTrabajoId",
                        "Vuelve a OT finalizadas y elige una.", HttpStatus.NOT_FOUND));
        if (ot.getEstado() != EstadoOT.FINALIZADA) {
            throw new BusinessException("La OT #" + ot.getId() + " aún no está finalizada.", "ordenTrabajoId",
                    "Solo se cobra una OT en estado FINALIZADA.", HttpStatus.CONFLICT);
        }
        if (comprobanteRepository.findByOrdenTrabajoIdAndEstado(ot.getId(), EstadoComprobante.EMITIDO).isPresent()) {
            throw new BusinessException("La OT #" + ot.getId() + " ya tiene comprobante vigente.", "ordenTrabajoId",
                    "Anúlalo primero si necesitas emitir otro.", HttpStatus.CONFLICT);
        }
        MetodoPago metodo = MetodoPago.desde(req.getMetodoPago());
        if (metodo == null) {
            throw new BusinessException("Elige cómo pagó: efectivo, tarjeta, Yape, Plin o transferencia.",
                    "metodoPago", "El método queda impreso en la constancia.");
        }
        FormaPago forma = FormaPago.desde(req.getFormaPago());
        if (forma == null) {
            throw new BusinessException("Forma de pago inválida.", "formaPago", "Usa CONTADO o CREDITO.");
        }
        if (forma == FormaPago.CREDITO && tipo != TipoComprobante.FACTURA) {
            throw new BusinessException("El crédito solo aplica en factura.", "formaPago",
                    "La boleta siempre es al contado.");
        }

        BigDecimal total = ot.getCotizacion().getTotal();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cotización no tiene total para cobrar.", "ordenTrabajoId",
                    "Revisa la cotización de la OT.");
        }

        AdquirenteResuelto adq = adquirente.resolver(req, ot, tipo, total);
        Usuario emisor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Sesión inválida.", null,
                        "Vuelve a iniciar sesión.", HttpStatus.UNAUTHORIZED));


        SerieComprobante serie = serieRepository.bloquearPorSerie(tipo.getSerie())
                .orElseThrow(() -> new BusinessException("Serie " + tipo.getSerie() + " no configurada.", "tipo",
                        "Contacta soporte."));
        int numero = serie.getUltimoNumero() + 1;
        serie.setUltimoNumero(numero);

        Comprobante c = Comprobante.builder()
                .ordenTrabajo(ot)
                .tipo(tipo)
                .codigoSunat(tipo.getCodigoSunat())
                .serie(serie.getSerie())
                .numero(numero)
                .fechaEmision(AppTime.ahora())
                .moneda("PEN")
                .formaPago(forma)
                .metodoPago(metodo)
                .emisorRuc(empresa.getRuc())
                .emisorRazonSocial(empresa.getRazonSocial())
                .emisorNombreComercial(empresa.getNombreComercial())
                .emisorDireccion(empresa.getDireccion())
                .emisorUbigeo(empresa.getUbigeo())
                .emisorTelefono(empresa.getTelefono())
                .emisorEmail(empresa.getEmail())
                .cliente(adq.cliente())
                .clienteTipoDoc(adq.tipoDoc())
                .clienteNumDoc(adq.numDoc())
                .clienteNombre(adq.nombre())
                .clienteDireccion(adq.direccion())
                .estado(EstadoComprobante.EMITIDO)
                .emitidoPor(emisor)
                .build();

        Vehiculo v = ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo();
        String obs = "Placa " + v.getPlaca() + " - OT #" + ot.getId();
        if (v.getMarca() != null) obs += " " + v.getMarca();
        if (v.getModelo() != null) obs += " " + v.getModelo();
        if (ot.getMecanico() != null) obs += " - Atendido por " + ot.getMecanico().getNombreCompleto();
        if (req.getObservacion() != null && !req.getObservacion().isBlank()) obs += " | " + req.getObservacion().trim();
        c.setObservacion(obs);

        detalle.construir(c, ot, total);
        c = comprobanteRepository.save(c);


        pagoEntregaService.registrarPagoDeComprobante(ot, c.getTotal(), c);
        if (Boolean.TRUE.equals(req.getRegistrarEntrega())) {
            pagoEntregaService.asegurarEntrega(ot);
        }

        return ComprobanteMapper.toDTO(comprobanteRepository.buscarCompleto(c.getId()).orElse(c));
    }

    @Transactional
    public ComprobanteResponseDTO anular(Long id, String motivo) {
        Comprobante c = comprobanteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Comprobante no existe.", null,
                        null, HttpStatus.NOT_FOUND));
        if (c.getEstado() == EstadoComprobante.ANULADO) {
            throw new BusinessException("Ya estaba anulado.", null, null, HttpStatus.CONFLICT);
        }
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Explica el motivo de la anulación.", "motivo",
                    "Queda registrado para auditoría.");
        }
        c.setEstado(EstadoComprobante.ANULADO);
        c.setMotivoAnulacion(motivo.trim());
        return ComprobanteMapper.toDTO(comprobanteRepository.save(c));
    }

    @Transactional(readOnly = true)
    public ComprobanteResponseDTO obtener(Long id) {
        Comprobante c = comprobanteRepository.buscarCompleto(id)
                .orElseThrow(() -> new BusinessException("Comprobante no existe.", null,
                        null, HttpStatus.NOT_FOUND));
        return ComprobanteMapper.toDTO(c);
    }

    @Transactional(readOnly = true)
    public Optional<ComprobanteResponseDTO> vigentePorOT(Long ordenTrabajoId) {
        return comprobanteRepository.findByOrdenTrabajoIdAndEstado(ordenTrabajoId, EstadoComprobante.EMITIDO)
                .map(ComprobanteMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ComprobanteResponseDTO> listar(String tipo, String estado,
                                               LocalDateTime desde, LocalDateTime hasta,
                                               String q, Pageable pageable) {
        return comprobanteRepository.filtrar(tipo, estado, desde, hasta, q,
                        OrdenSeguro.traducir(pageable, COLUMNAS_ORDEN,
                                Sort.Order.desc("fecha_emision"), Sort.Order.desc("id")))
                .map(ComprobanteMapper::toDTO);
    }

    /** Vista previa sin reservar número: totales y adquirente sugerido. */
    @Transactional(readOnly = true)
    public ComprobanteResponseDTO previsualizar(Long ordenTrabajoId, String tipoCodigo) {
        TipoComprobante tipo = TipoComprobante.desde(tipoCodigo);
        if (tipo == null) tipo = TipoComprobante.BOLETA;
        OrdenTrabajo ot = ordenTrabajoRepository.findById(ordenTrabajoId)
                .orElseThrow(() -> new BusinessException("La orden de trabajo no existe.", "ordenTrabajoId",
                        null, HttpStatus.NOT_FOUND));
        Cliente cli = ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo().getCliente();
        BigDecimal total = ot.getCotizacion().getTotal();
        BigDecimal gravado = total.divide(BigDecimal.ONE.add(empresa.getIgv()), 2, RoundingMode.HALF_UP);
        return ComprobanteResponseDTO.builder()
                .ordenTrabajoId(ot.getId())
                .tipo(tipo.name())
                .codigoSunat(tipo.getCodigoSunat())
                .serie(tipo.getSerie())
                .moneda("PEN")
                .clienteId(cli.getId())
                .clienteTipoDoc(cli.getTipoDocumento() != null ? cli.getTipoDocumento().name() : "DNI")
                .clienteNumDoc(cli.getDocumento())
                .clienteNombre(cli.nombreFiscal())
                .clienteDireccion(cli.getDireccion())
                .totalGravado(gravado)
                .igv(total.subtract(gravado))
                .total(total)
                .totalLetras(NumeroALetras.convertir(total))
                .build();
    }
}
