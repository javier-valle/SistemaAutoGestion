package com.autogestion.service;

import com.autogestion.dto.CotizacionCompletaRequest;
import com.autogestion.dto.CotizacionRequest;
import com.autogestion.dto.CotizacionResponseDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import com.autogestion.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final CotizacionServicioRepository cotizacionServicioRepository;
    private final CotizacionProductoRepository cotizacionProductoRepository;
    private final DiagnosticoRepository diagnosticoRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final RecepcionRepository recepcionRepository;

    /** Estados que ocupan el diagnóstico: mientras exista uno, no se crea otra cotización. */
    private static final List<EstadoCotizacion> VIGENTES = List.of(
            EstadoCotizacion.PENDIENTE, EstadoCotizacion.EN_DIAGNOSTICO, EstadoCotizacion.APROBADA);

    private record ServicioItem(Long servicioId, BigDecimal precio) { }

    private record ProductoItem(Long productoId, Integer cantidadEstimada, BigDecimal precioUnitario) { }

    @Transactional
    public CotizacionResponseDTO crear(CotizacionRequest request) {
        return crearInterno(request.getDiagnosticoId(),
                request.getServicios() == null ? List.of() : request.getServicios().stream()
                        .map(s -> new ServicioItem(s.getServicioId(), s.getPrecio())).toList(),
                request.getProductos() == null ? List.of() : request.getProductos().stream()
                        .map(p -> new ProductoItem(p.getProductoId(), p.getCantidadEstimada(),
                                p.getPrecioUnitario())).toList());
    }

    @Transactional
    public CotizacionResponseDTO crearCompleta(CotizacionCompletaRequest request) {
        return crearInterno(request.getDiagnosticoId(),
                request.getServicios() == null ? List.of() : request.getServicios().stream()
                        .map(s -> new ServicioItem(s.getServicioId(), s.getPrecio())).toList(),
                request.getProductos() == null ? List.of() : request.getProductos().stream()
                        .map(p -> new ProductoItem(p.getProductoId(), p.getCantidadEstimada(),
                                p.getPrecioUnitario())).toList());
    }

    /**
     * Único camino de creación: valida la vigencia, guarda líneas y devuelve el
     * DTO. Las dos variantes de request solo cambian la forma de entrar.
     */
    private CotizacionResponseDTO crearInterno(Long diagnosticoId, List<ServicioItem> servicios,
                                               List<ProductoItem> productos) {
        try {
            Diagnostico diagnostico = diagnosticoRepository.findById(diagnosticoId)
                    .orElseThrow(() -> new BusinessException("Diagnóstico no encontrado: " + diagnosticoId));

            exigirCotizacionUnica(diagnostico.getId());

            Cotizacion cotizacion = cotizacionRepository.save(Cotizacion.builder()
                    .diagnostico(diagnostico)
                    .diagnosticoVigenteId(diagnostico.getId())
                    .total(BigDecimal.ZERO)
                    .estado(EstadoCotizacion.PENDIENTE)
                    .fecha(AppTime.ahora())
                    .build());

            BigDecimal total = BigDecimal.ZERO;
            for (ServicioItem item : servicios) {
                Servicio servicio = servicioRepository.findById(item.servicioId())
                        .orElseThrow(() -> new BusinessException("Servicio no encontrado: " + item.servicioId()));
                BigDecimal precio = item.precio() != null ? item.precio() : servicio.getPrecioBase();
                cotizacionServicioRepository.save(CotizacionServicio.builder()
                        .cotizacion(cotizacion)
                        .servicio(servicio)
                        .precio(precio)
                        .build());
                total = total.add(precio);
            }

            for (ProductoItem item : productos) {
                Producto producto = productoRepository.findById(item.productoId())
                        .orElseThrow(() -> new BusinessException("Producto no encontrado: " + item.productoId()));
                BigDecimal precioUnitario = item.precioUnitario() != null
                        ? item.precioUnitario() : producto.getPrecioUnitario();
                BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(item.cantidadEstimada()));
                cotizacionProductoRepository.save(CotizacionProducto.builder()
                        .cotizacion(cotizacion)
                        .producto(producto)
                        .cantidadEstimada(item.cantidadEstimada())
                        .precioUnitario(precioUnitario)
                        .build());
                total = total.add(subtotal);
            }

            cotizacion.setTotal(total);
            return toResponseDTO(cotizacionRepository.save(cotizacion));
        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Este diagnóstico ya tiene una cotización vigente.", "diagnosticoId",
                    "Recházala o conviértela antes de crear otra.", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public CotizacionResponseDTO aprobar(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada"));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new BusinessException("Solo se pueden aprobar cotizaciones pendientes.", "estado",
                    null, HttpStatus.CONFLICT);
        }

        cotizacion.setEstado(EstadoCotizacion.APROBADA);
        cotizacion = cotizacionRepository.save(cotizacion);

        Diagnostico diagnostico = cotizacion.getDiagnostico();
        Recepcion recepcion = diagnostico.getRecepcion();
        recepcion.setEstado(EstadoRecepcion.COTIZADA);
        recepcionRepository.save(recepcion);

        Cotizacion saved = cotizacionRepository.findById(cotizacion.getId())
                .orElseThrow(() -> new BusinessException("Error al obtener cotización aprobada"));
        return toResponseDTO(saved);
    }

    @Transactional
    public CotizacionResponseDTO rechazar(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada"));

        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new BusinessException("Solo se pueden rechazar cotizaciones pendientes.", "estado",
                    null, HttpStatus.CONFLICT);
        }

        cotizacion.setEstado(EstadoCotizacion.RECHAZADA);
        cotizacion.setDiagnosticoVigenteId(null);
        cotizacion = cotizacionRepository.save(cotizacion);

        Cotizacion saved = cotizacionRepository.findById(cotizacion.getId())
                .orElseThrow(() -> new BusinessException("Error al obtener cotización rechazada"));
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public CotizacionResponseDTO obtenerPorId(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada"));
        return toResponseDTO(cotizacion);
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponseDTO> listar(String estado) {
        List<Cotizacion> cotizaciones;
        if (estado == null || estado.isEmpty()) {
            cotizaciones = cotizacionRepository.findAll();
        } else {
            EstadoCotizacion est = EstadoCotizacion.desde(estado);
            cotizaciones = (est != null) ? cotizacionRepository.findByEstado(est) : List.of();
        }
        return cotizaciones.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /** Lista solo las cotizaciones del cliente autenticado (el id sale del token). */
    @Transactional(readOnly = true)
    public List<CotizacionResponseDTO> listarPorCliente(Long clienteId) {
        if (clienteId == null) return List.of();
        return cotizacionRepository.findByClienteId(clienteId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CotizacionResponseDTO obtenerDeCliente(Long clienteId, Long id) {
        Cotizacion cotizacion = cotizacionRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada", HttpStatus.NOT_FOUND));
        return toResponseDTO(cotizacion);
    }

    /** El cliente aprueba su propia cotización; la recepción pasa a COTIZADA como en el panel del taller. */
    @Transactional
    public CotizacionResponseDTO aprobarDeCliente(Long clienteId, Long id) {
        Cotizacion cotizacion = cotizacionRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada", HttpStatus.NOT_FOUND));
        return aprobar(cotizacion.getId());
    }

    @Transactional
    public CotizacionResponseDTO rechazarDeCliente(Long clienteId, Long id) {
        Cotizacion cotizacion = cotizacionRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada", HttpStatus.NOT_FOUND));
        return rechazar(cotizacion.getId());
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponseDTO.CotizacionServicioDTO> listarServicios(Long cotizacionId) {
        return cotizacionServicioRepository.findByCotizacionId(cotizacionId).stream()
                .map(cs -> CotizacionResponseDTO.CotizacionServicioDTO.builder()
                        .servicioId(cs.getServicio().getId())
                        .servicioNombre(cs.getServicio().getNombre())
                        .precio(cs.getPrecio())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponseDTO.CotizacionProductoDTO> listarProductos(Long cotizacionId) {
        return cotizacionProductoRepository.findByCotizacionId(cotizacionId).stream()
                .map(cp -> CotizacionResponseDTO.CotizacionProductoDTO.builder()
                        .productoId(cp.getProducto().getId())
                        .productoNombre(cp.getProducto().getNombre())
                        .cantidadEstimada(cp.getCantidadEstimada())
                        .precioUnitario(cp.getPrecioUnitario())
                        .build())
                .toList();
    }

    /** Un diagnóstico puede renegociarse, pero solo con una cotización viva a la vez. */
    private void exigirCotizacionUnica(Long diagnosticoId) {
        if (cotizacionRepository.existsByDiagnosticoIdAndEstadoIn(diagnosticoId, VIGENTES)) {
            throw new BusinessException("Este diagnóstico ya tiene una cotización vigente.", "diagnosticoId",
                    "Recházala o conviértela antes de crear otra.", HttpStatus.CONFLICT);
        }
    }

    private CotizacionResponseDTO toResponseDTO(Cotizacion cotizacion) {
        Diagnostico diag = cotizacion.getDiagnostico();
        Recepcion recep = diag.getRecepcion();
        Vehiculo veh = recep.getVehiculo();

        List<CotizacionServicio> cotizacionServicios = cotizacionServicioRepository.findByCotizacionId(cotizacion.getId());
        List<CotizacionProducto> cotizacionProductos = cotizacionProductoRepository.findByCotizacionId(cotizacion.getId());

        List<CotizacionResponseDTO.CotizacionServicioDTO> servicios = cotizacionServicios.stream()
            .map(cs -> CotizacionResponseDTO.CotizacionServicioDTO.builder()
                .servicioId(cs.getServicio().getId())
                .servicioNombre(cs.getServicio().getNombre())
                .precio(cs.getPrecio())
                .build())
            .collect(Collectors.toList());

        List<CotizacionResponseDTO.CotizacionProductoDTO> productos = cotizacionProductos.stream()
            .map(cp -> CotizacionResponseDTO.CotizacionProductoDTO.builder()
                .productoId(cp.getProducto().getId())
                .productoNombre(cp.getProducto().getNombre())
                .cantidadEstimada(cp.getCantidadEstimada())
                .precioUnitario(cp.getPrecioUnitario())
                .build())
            .collect(Collectors.toList());

        return CotizacionResponseDTO.builder()
            .id(cotizacion.getId())
            .diagnosticoId(diag.getId())
            .diagnosticoDescripcion(diag.getDescripcion())
            .mecanicoId(diag.getMecanico() != null ? diag.getMecanico().getId() : null)
            .mecanicoNombre(diag.getMecanico() != null ? diag.getMecanico().getNombreCompleto() : null)
            .recepcionId(String.valueOf(recep.getId()))
            .vehiculoPlaca(veh.getPlaca())
            .clienteNombre(veh.getCliente().getNombre())
            .total(cotizacion.getTotal())
            .estado(cotizacion.getEstado().name())
            .fecha(cotizacion.getFecha())
            .servicios(servicios)
            .productos(productos)
            .build();
    }
}
