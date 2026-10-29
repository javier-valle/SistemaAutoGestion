package com.autogestion.service;

import com.autogestion.dto.OrdenTrabajoCompletaRequest;
import com.autogestion.dto.OrdenTrabajoFinalizadaDTO;
import com.autogestion.dto.OrdenTrabajoRequest;
import com.autogestion.dto.OrdenTrabajoResponseDTO;
import com.autogestion.dto.OtProductoUsadoResponseDTO;
import com.autogestion.dto.PagoEntregaCompletaRequest;
import com.autogestion.dto.PagoEntregaResponseDTO;
import com.autogestion.dto.ProductoUsadoRequest;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import com.autogestion.service.InventarioService;
import com.autogestion.service.PagoEntregaService;
import lombok.RequiredArgsConstructor;
import com.autogestion.util.AppTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final OtProductoUsadoRepository otProductoUsadoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PagoEntregaService pagoEntregaService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioMovimientoRepository inventarioMovimientoRepository;
    private final RecepcionRepository recepcionRepository;
    private final PagoEntregaRepository pagoEntregaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final InventarioService inventarioService;

    @Transactional
    public OrdenTrabajo crear(OrdenTrabajoRequest request) {
        Cotizacion cotizacion = cotizacionAprobada(request.getCotizacionId());
        Usuario mecanico = mecanico(request.getMecanicoId());
        OrdenTrabajo ot = abrirOrden(cotizacion, mecanico);

        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacionRepository.save(cotizacion);

        return ot;
    }

    @Transactional
    public OrdenTrabajo crearCompleta(OrdenTrabajoCompletaRequest request) {
        Cotizacion cotizacion = cotizacionAprobada(request.getCotizacionId());
        Usuario mecanico = mecanico(request.getMecanicoId());
        OrdenTrabajo ot = abrirOrden(cotizacion, mecanico);

        if (request.getProductosUsados() != null) {
            for (OrdenTrabajoCompletaRequest.ProductoUsadoItem item : request.getProductosUsados()) {
                registrarProductoUsadoInterno(ot, item.getProductoId(), item.getCantidadUsada());
            }
        }

        if (request.getPagoEntrega() != null && request.getPagoEntrega().getMonto() != null) {
            registrarPagoInterno(ot, request.getPagoEntrega().getMonto());
            if (Boolean.TRUE.equals(request.getPagoEntrega().getRegistrarEntrega())) {
                registrarEntregaInterno(ot);
            }
        }

        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacionRepository.save(cotizacion);

        return ordenTrabajoRepository.save(ot);
    }

    /** Solo una cotización APROBADA puede convertirse en OT (fila bloqueada: anti doble OT). */
    private Cotizacion cotizacionAprobada(Long cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository.bloquearPorId(cotizacionId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada.", "cotizacionId",
                        null, HttpStatus.NOT_FOUND));
        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BusinessException("Solo se puede crear OT con cotización aprobada.", "cotizacionId",
                    "La cotización debe estar APROBADA; ya fue convertida o rechazada.", HttpStatus.CONFLICT);
        }
        return cotizacion;
    }

    private Usuario mecanico(Long mecanicoId) {
        Usuario m = usuarioRepository.findById(mecanicoId)
                .orElseThrow(() -> new BusinessException("Mecánico no encontrado.", "mecanicoId",
                        null, HttpStatus.NOT_FOUND));
        if (!"MECANICO".equals(m.getRol())) {
            throw new BusinessException("El usuario elegido no es mecánico.", "mecanicoId",
                    "Elige un usuario con rol MECANICO.", HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new BusinessException("El mecánico está desactivado.", "mecanicoId",
                    "Actívalo en Equipo o elige otro.", HttpStatus.CONFLICT);
        }
        return m;
    }

    /** Abre la OT en PENDIENTE y pone la recepción EN_TRABAJO. Único punto de apertura. */
    private OrdenTrabajo abrirOrden(Cotizacion cotizacion, Usuario mecanico) {
        OrdenTrabajo ot = ordenTrabajoRepository.save(OrdenTrabajo.builder()
                .cotizacion(cotizacion)
                .mecanico(mecanico)
                .estado(EstadoOT.PENDIENTE)
                .fechaCreacion(AppTime.ahora())
                .build());
        Diagnostico diagnostico = cotizacion.getDiagnostico();
        Recepcion recepcion = diagnostico.getRecepcion();
        recepcion.setEstado(EstadoRecepcion.EN_TRABAJO);
        recepcionRepository.save(recepcion);
        return ot;
    }

    /**
     * ÚNICO lugar donde se consume stock por OT. Delega en InventarioService,
     * que guarda costo del momento, OT y usuario (auditoría completa Fase 6).
     */
    private OtProductoUsado consumirProducto(OrdenTrabajo ot, Long productoId, Integer cantidad, Long usuarioId) {
        if (cantidad == null || cantidad < 1) {
            throw new BusinessException("La cantidad mínima es 1.", "cantidadUsada", null);
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("Producto no encontrado.", "productoId",
                        null, HttpStatus.NOT_FOUND));
        inventarioService.registrarConsumoOT(ot.getId(), productoId, cantidad, usuarioId);
        OtProductoUsado uso = OtProductoUsado.builder()
                .ordenTrabajo(ot)
                .producto(producto)
                .cantidadUsada(cantidad)
                .build();
        return otProductoUsadoRepository.save(uso);
    }

    private void registrarProductoUsadoInterno(OrdenTrabajo ot, Long productoId, Integer cantidad) {
        consumirProducto(ot, productoId, cantidad, null);
    }

    private void registrarPagoInterno(OrdenTrabajo ot, BigDecimal monto) {
        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId()).orElse(null);
        if (pago == null) {
            pago = PagoEntrega.builder()
                    .ordenTrabajo(ot)
                    .build();
        } else if (pago.getFechaPago() != null) {
            throw new BusinessException("Esta OT ya tiene un pago registrado el " + pago.getFechaPago() + ".",
                    "ordenTrabajoId", null, HttpStatus.CONFLICT);
        }
        pago.setMonto(monto);
        pago.setFechaPago(AppTime.ahora());
        pagoEntregaRepository.save(pago);
    }

    private void registrarEntregaInterno(OrdenTrabajo ot) {
        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId()).orElse(null);
        if (pago == null) {
            pago = PagoEntrega.builder()
                    .ordenTrabajo(ot)
                    .monto(BigDecimal.ZERO)
                    .build();
        }
        pago.setFechaEntrega(AppTime.ahora());
        pagoEntregaRepository.save(pago);

        ot.setEstado(EstadoOT.FINALIZADA);
        ot.setFechaFin(AppTime.ahora());

        Recepcion recepcion = ot.getCotizacion().getDiagnostico().getRecepcion();
        recepcion.setEstado(EstadoRecepcion.FINALIZADA);
        recepcionRepository.save(recepcion);
    }

    @Transactional
    public PagoEntregaResponseDTO registrarPagoEntregaCompleto(PagoEntregaCompletaRequest request) {

        OrdenTrabajo ot = ordenTrabajoRepository.bloquearPorId(request.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada"));

        if (request.getMonto() != null) {
            registrarPagoInterno(ot, request.getMonto());
        }
        if (Boolean.TRUE.equals(request.getRegistrarEntrega())) {
            registrarEntregaInterno(ot);
        }
        PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(request.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("Error al obtener pago/entrega"));
        return pagoEntregaService.toResponseDTO(pago);
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoFinalizadaDTO> listarFinalizadasConPago() {
        return ordenTrabajoRepository.findByEstado(EstadoOT.FINALIZADA).stream()
                .map(ot -> {
                    BigDecimal monto = pagoEntregaService.obtenerMonto(ot.getId());
                    PagoEntrega pago = pagoEntregaRepository.findByOrdenTrabajoId(ot.getId()).orElse(null);
                    boolean tienePago = pago != null && pago.getFechaPago() != null;
                    boolean tieneEntrega = pago != null && pago.getFechaEntrega() != null;
                    var comp = comprobanteRepository
                            .findByOrdenTrabajoIdAndEstado(ot.getId(), EstadoComprobante.EMITIDO)
                            .orElse(null);
                    return new OrdenTrabajoFinalizadaDTO(
                            ot.getId(),
                            ot.getCotizacion().getId(),
                            ot.getMecanico().getNombre(),
                            ot.getFechaCreacion(),
                            monto,
                            tienePago,
                            tieneEntrega,
                            comp != null ? comp.folio() : null,
                            comp != null ? comp.getTipo().name() : null,
                            comp != null ? comp.getEstado().name() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdenTrabajo cambiarEstado(Long id, String nuevoEstadoCodigo) {
        OrdenTrabajo ot = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada"));

        EstadoOT nuevo = EstadoOT.desde(nuevoEstadoCodigo);
        if (nuevo == null) {
            throw new BusinessException("Estado inválido: " + nuevoEstadoCodigo + ".", "estado",
                    "Usa PENDIENTE, EN_PROCESO, EN_PRUEBA, FINALIZADA o CANCELADA.");
        }
        EstadoOT estadoActual = ot.getEstado();
        if (!estadoActual.puedePasarA(nuevo)) {
            throw new BusinessException("No se puede pasar de " + estadoActual + " a " + nuevo + ".", "estado",
                    "Sigue el orden: PENDIENTE → EN_PROCESO → EN_PRUEBA → FINALIZADA.", HttpStatus.CONFLICT);
        }

        ot.setEstado(nuevo);
        if (nuevo == EstadoOT.FINALIZADA) {
            ot.setFechaFin(AppTime.ahora());
            Recepcion recepcion = ot.getCotizacion().getDiagnostico().getRecepcion();
            recepcion.setEstado(EstadoRecepcion.FINALIZADA);
            recepcionRepository.save(recepcion);
        }

        return ordenTrabajoRepository.save(ot);
    }

    @Transactional
    public OtProductoUsadoResponseDTO registrarProductoUsado(Long ordenTrabajoId, ProductoUsadoRequest request,
                                                             Long usuarioId) {
        OrdenTrabajo ot = ordenTrabajoRepository.findById(ordenTrabajoId)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", null,
                        null, HttpStatus.NOT_FOUND));

        OtProductoUsado uso = consumirProducto(ot, request.getProductoId(), request.getCantidadUsada(), usuarioId);
        return toProductoUsadoResponseDTO(uso);
    }

    /** Reasigna el mecánico (para rotar carga o liberar a quien se desactiva). */
    @Transactional
    public OrdenTrabajoResponseDTO reasignarMecanico(Long id, Long mecanicoId) {
        OrdenTrabajo ot = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada.", null,
                        null, HttpStatus.NOT_FOUND));
        if (ot.getEstado().estaCerrada()) {
            throw new BusinessException("La OT #" + id + " ya está cerrada.", "estado",
                    "Solo se reasignan OT abiertas.", HttpStatus.CONFLICT);
        }
        Usuario mecanico = usuarioRepository.findById(mecanicoId)
                .orElseThrow(() -> new BusinessException("Mecánico no encontrado.", "mecanicoId",
                        null, HttpStatus.NOT_FOUND));
        if (!"MECANICO".equals(mecanico.getRol()) || Boolean.FALSE.equals(mecanico.getActivo())) {
            throw new BusinessException("Debe ser un mecánico activo.", "mecanicoId",
                    "Elige de la lista de mecánicos.");
        }
        ot.setMecanico(mecanico);
        return toResponseDTO(ordenTrabajoRepository.save(ot));
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponseDTO> listar(String estado) {
        List<OrdenTrabajo> ots;
        if (estado == null || estado.isEmpty()) {
            ots = ordenTrabajoRepository.findAll();
        } else {
            EstadoOT est = EstadoOT.desde(estado);
            ots = (est != null) ? ordenTrabajoRepository.findByEstado(est) : List.of();
        }
        return ots.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponseDTO> listarPorMecanico(Long mecanicoId) {
        return ordenTrabajoRepository.findByMecanicoId(mecanicoId).stream()
                .map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenTrabajoResponseDTO obtenerPorId(Long id) {
        OrdenTrabajo ot = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden de trabajo no encontrada"));
        return toResponseDTO(ot);
    }

    @Transactional(readOnly = true)
    public List<OtProductoUsadoResponseDTO> listarProductosUsados(Long ordenTrabajoId) {
        return otProductoUsadoRepository.findByOrdenTrabajoId(ordenTrabajoId).stream()
                .map(this::toProductoUsadoResponseDTO)
                .collect(Collectors.toList());
    }

    private OrdenTrabajoResponseDTO toResponseDTO(OrdenTrabajo ot) {
        Cotizacion cotizacion = ot.getCotizacion();
        Diagnostico diagnostico = cotizacion.getDiagnostico();
        Recepcion recepcion = diagnostico.getRecepcion();
        Vehiculo vehiculo = recepcion.getVehiculo();
        Cliente cliente = vehiculo.getCliente();
        Usuario mecanico = ot.getMecanico();

        return OrdenTrabajoResponseDTO.builder()
                .id(ot.getId())
                .cotizacionId(cotizacion.getId())
                .mecanicoId(mecanico.getId())
                .mecanicoNombre(mecanico.getNombreCompleto())
                .estado(ot.getEstado().name())
                .fechaCreacion(ot.getFechaCreacion())
                .fechaFin(ot.getFechaFin())
                .vehiculoPlaca(vehiculo.getPlaca())
                .clienteNombre(cliente.getNombre())
                .recepcionId(recepcion.getId())
                .problemaReportado(recepcion.getProblemaReportado())
                .build();
    }

    private OtProductoUsadoResponseDTO toProductoUsadoResponseDTO(OtProductoUsado uso) {
        Producto producto = uso.getProducto();
        return OtProductoUsadoResponseDTO.builder()
                .id(uso.getId())
                .ordenTrabajoId(uso.getOrdenTrabajo().getId())
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .cantidadUsada(uso.getCantidadUsada())
                .build();
    }

}
