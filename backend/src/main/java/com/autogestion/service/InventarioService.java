package com.autogestion.service;

import com.autogestion.dto.MovimientoInventarioRequest;
import com.autogestion.dto.MovimientoResponseDTO;
import com.autogestion.dto.ProductoRequest;
import com.autogestion.entity.InventarioMovimiento;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.Producto;
import com.autogestion.entity.Proveedor;
import com.autogestion.entity.TipoMovimiento;
import com.autogestion.entity.Usuario;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.InventarioMovimientoRepository;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.ProductoRepository;
import com.autogestion.repository.ProveedorRepository;
import com.autogestion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import com.autogestion.util.AppTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private static final Set<String> TIPOS_PRODUCTO = Set.of("REPUESTO", "INSUMO");

    private final ProductoRepository productoRepository;
    private final InventarioMovimientoRepository inventarioMovimientoRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    @Transactional
    public Producto crear(ProductoRequest request) {
        validarTipoProducto(request.getTipo());
        Producto producto = Producto.builder()
                .nombre(request.getNombre().trim())
                .tipo(request.getTipo().trim().toUpperCase())
                .precioUnitario(request.getPrecioUnitario())
                .costoUnitario(request.getCostoUnitario() != null ? request.getCostoUnitario() : BigDecimal.ZERO)
                .unidadMedida(request.getUnidadMedida() != null && !request.getUnidadMedida().isBlank()
                        ? request.getUnidadMedida().trim().toUpperCase() : "NIU")
                .stockActual(request.getStockActual() != null ? request.getStockActual() : 0)
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0)
                .build();
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado.", null,
                        null, HttpStatus.NOT_FOUND));
        if (request.getNombre() != null) producto.setNombre(request.getNombre().trim());
        if (request.getTipo() != null) {
            validarTipoProducto(request.getTipo());
            producto.setTipo(request.getTipo().trim().toUpperCase());
        }
        if (request.getPrecioUnitario() != null) producto.setPrecioUnitario(request.getPrecioUnitario());
        if (request.getCostoUnitario() != null) producto.setCostoUnitario(request.getCostoUnitario());
        if (request.getUnidadMedida() != null && !request.getUnidadMedida().isBlank()) {
            producto.setUnidadMedida(request.getUnidadMedida().trim().toUpperCase());
        }
        if (request.getStockMinimo() != null) producto.setStockMinimo(request.getStockMinimo());
        return productoRepository.save(producto);
    }

    private void validarTipoProducto(String tipo) {
        if (tipo == null || !TIPOS_PRODUCTO.contains(tipo.trim().toUpperCase())) {
            throw new BusinessException("Tipo inválido.", "tipo", "Usa REPUESTO o INSUMO.");
        }
    }

    /** Entrada manual (compras desde el formulario) con el usuario que la registra. */
    @Transactional
    public MovimientoResponseDTO registrarMovimiento(MovimientoInventarioRequest request, Long usuarioId) {
        return toDTO(aplicar(request.getProductoId(), request.getTipo(), request.getCantidad(),
                request.getMotivo(), request.getCostoUnitario(), request.getProveedorId(),
                request.getOrdenTrabajoId(), usuarioId, request.getDocumentoRef()));
    }

    /**
     * Consumo desde una OT (lo usa OrdenTrabajoService: un solo camino).
     * Guarda orden_trabajo_id, el costo del momento y quién lo registró.
     */
    @Transactional
    public InventarioMovimiento registrarConsumoOT(Long ordenTrabajoId, Long productoId,
                                                   Integer cantidad, Long usuarioId) {
        return aplicar(productoId, "CONSUMO", cantidad, null, null, null, ordenTrabajoId, usuarioId, null);
    }

    private InventarioMovimiento aplicar(Long productoId, String tipoCodigo, Integer cantidad,
                                         String motivo, BigDecimal costo, Long proveedorId,
                                         Long ordenTrabajoId, Long usuarioId, String documentoRef) {
        TipoMovimiento tipo = TipoMovimiento.desde(tipoCodigo);
        if (tipo == null) {
            throw new BusinessException("Tipo de movimiento inválido.", "tipo",
                    "Usa ENTRADA, CONSUMO, AJUSTE_POSITIVO, AJUSTE_NEGATIVO o MERMA.");
        }
        if (cantidad == null || cantidad < 1) {
            throw new BusinessException("La cantidad mínima es 1.", "cantidad", null);
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("Producto no encontrado.", "productoId",
                        null, HttpStatus.NOT_FOUND));

        Proveedor proveedor = null;
        if (proveedorId != null) {
            proveedor = proveedorRepository.findById(proveedorId)
                    .orElseThrow(() -> new BusinessException("Proveedor no encontrado.", "proveedorId", null,
                            HttpStatus.NOT_FOUND));
        }
        OrdenTrabajo ot = null;
        if (ordenTrabajoId != null) {
            ot = ordenTrabajoRepository.findById(ordenTrabajoId)
                    .orElseThrow(() -> new BusinessException("OT no encontrada.", "ordenTrabajoId",
                            null, HttpStatus.NOT_FOUND));
        }
        Usuario usuario = null;
        if (usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }

        int antes = producto.getStockActual();
        BigDecimal costoFinal = costo;
        String motivoFinal = (motivo == null || motivo.isBlank()) ? null : motivo.trim();

        switch (tipo) {
            case ENTRADA -> {
                if (costo == null) {
                    throw new BusinessException("En la compra indica cuánto costó cada unidad.", "costoUnitario",
                            "Sin costo no hay margen ni valorización.");
                }
                if (costo.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("El costo no puede ser negativo.", "costoUnitario", null);
                }
                producto.setStockActual(antes + cantidad);
                producto.setCostoUnitario(costo); // último costo manda
                if (motivoFinal == null) {
                    motivoFinal = "Compra" + (documentoRef != null ? " " + documentoRef : "")
                            + (proveedor != null ? " - " + proveedor.getNombre() : "");
                }
            }
            case CONSUMO -> {
                if (ot == null) {
                    throw new BusinessException("El consumo necesita su OT.", "ordenTrabajoId",
                            "Registra el uso desde la orden de trabajo.");
                }
                if (antes < cantidad) {
                    throw new BusinessException("Stock insuficiente para " + producto.getNombre()
                            + " (disponible: " + antes + ").", "cantidad",
                            "Registra una compra o usa menos cantidad.", HttpStatus.CONFLICT);
                }
                producto.setStockActual(antes - cantidad);
                costoFinal = producto.getCostoUnitario();
                motivoFinal = "OT #" + ot.getId() + " - " + producto.getNombre();
            }
            case AJUSTE_POSITIVO -> {
                producto.setStockActual(antes + cantidad);
                if (motivoFinal == null) motivoFinal = "Ajuste positivo de inventario";
            }
            case AJUSTE_NEGATIVO, MERMA -> {
                if (motivoFinal == null) {
                    throw new BusinessException("Explica el motivo: queda auditado.", "motivo",
                            tipo == TipoMovimiento.MERMA ? "Ej. vencido, roto, derrame." : "Ej. conteo físico menor.");
                }
                if (antes < cantidad) {
                    throw new BusinessException("No puedes retirar más de lo que hay (" + antes + ").", "cantidad",
                            null, HttpStatus.CONFLICT);
                }
                producto.setStockActual(antes - cantidad);
                costoFinal = producto.getCostoUnitario();
            }
        }
        productoRepository.save(producto);

        InventarioMovimiento mov = InventarioMovimiento.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(cantidad)
                .motivo(motivoFinal)
                .costoUnitario(costoFinal)
                .proveedor(proveedor)
                .ordenTrabajo(ot)
                .usuario(usuario)
                .documentoRef(documentoRef)
                .stockAntes(antes)
                .stockDespues(producto.getStockActual())
                .fecha(AppTime.ahora())
                .build();
        return inventarioMovimientoRepository.save(mov);
    }

    /** Compras del rango (para el reporte de entradas), paginadas. */
    @Transactional(readOnly = true)
    public Page<MovimientoResponseDTO> entradas(LocalDate desde, LocalDate hastaExcluido,
                                                Long proveedorId, int page, int size) {
        return inventarioMovimientoRepository.entradas(desde.atStartOfDay(), hastaExcluido.atStartOfDay(),
                proveedorId, org.springframework.data.domain.PageRequest.of(page, size)).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoResponseDTO> listarMovimientos(Long productoId, String tipoCodigo, Long usuarioId,
                                                        LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        TipoMovimiento tipo = tipoCodigo != null && !tipoCodigo.isBlank() ? TipoMovimiento.desde(tipoCodigo) : null;
        if (tipoCodigo != null && !tipoCodigo.isBlank() && tipo == null) {
            throw new BusinessException("Tipo inválido.", "tipo", null);
        }
        return inventarioMovimientoRepository
                .filtrar(productoId, tipo, usuarioId, desde, hasta, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<Producto> alertasStock() {
        return productoRepository.findConStockBajo();
    }

    @Transactional(readOnly = true)
    public List<InventarioMovimiento> listarMovimientos(Long productoId) {
        if (productoId != null) {
            return inventarioMovimientoRepository.findByProductoId(productoId);
        }
        return inventarioMovimientoRepository.findAll();
    }

    public MovimientoResponseDTO toDTO(InventarioMovimiento m) {
        return MovimientoResponseDTO.builder()
                .id(m.getId())
                .productoId(m.getProducto().getId())
                .productoNombre(m.getProducto().getNombre())
                .tipo(m.getTipo().name())
                .cantidad(m.getCantidad())
                .motivo(m.getMotivo())
                .costoUnitario(m.getCostoUnitario())
                .proveedorId(m.getProveedor() != null ? m.getProveedor().getId() : null)
                .proveedorNombre(m.getProveedor() != null ? m.getProveedor().getNombre() : null)
                .ordenTrabajoId(m.getOrdenTrabajo() != null ? m.getOrdenTrabajo().getId() : null)
                .usuarioId(m.getUsuario() != null ? m.getUsuario().getId() : null)
                .usuarioNombre(m.getUsuario() != null ? m.getUsuario().getNombreCompleto() : null)
                .documentoRef(m.getDocumentoRef())
                .stockAntes(m.getStockAntes())
                .stockDespues(m.getStockDespues())
                .fecha(m.getFecha())
                .build();
    }
}
