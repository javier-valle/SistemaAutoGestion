package com.autogestion.service;

import com.autogestion.dto.ClienteDetalleDTO;
import com.autogestion.dto.ClienteRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Genero;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.ComprobanteRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.VehiculoRepository;
import com.autogestion.util.Like;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final RecepcionRepository recepcionRepository;
    private final ComprobanteRepository comprobanteRepository;

    @Transactional
    public Cliente crear(ClienteRequest request) {
        TipoDocumento tipo = request.tipoEfectivo();
        String doc = request.documentoNormalizado();
        if (clienteRepository.existsByTipoDocumentoAndDocumento(tipo, doc)) {
            throw new BusinessException("Ya existe un cliente con " + tipo + " " + doc + ".",
                    "documento", "Búscalo en lugar de crear otro.", HttpStatus.CONFLICT);
        }
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre().trim())
                .telefono(nuloSiVacio(request.getTelefono()))
                .email(nuloSiVacio(request.getEmail()))
                .tipoDocumento(tipo)
                .documento(doc)
                .razonSocial(nuloSiVacio(request.getRazonSocial()))
                .direccion(nuloSiVacio(request.getDireccion()))
                .genero(Genero.desde(request.getGenero()))
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtenerPorId(id);
        TipoDocumento tipo = request.tipoEfectivo();
        String doc = request.documentoNormalizado();
        if ((!tipo.equals(cliente.getTipoDocumento()) || !doc.equals(cliente.getDocumento()))
                && clienteRepository.existsByTipoDocumentoAndDocumento(tipo, doc)) {
            throw new BusinessException("Otro cliente ya usa " + tipo + " " + doc + ".",
                    "documento", "Revisa el número.", HttpStatus.CONFLICT);
        }
        cliente.setNombre(request.getNombre().trim());
        cliente.setTelefono(nuloSiVacio(request.getTelefono()));
        cliente.setEmail(nuloSiVacio(request.getEmail()));
        cliente.setTipoDocumento(tipo);
        cliente.setDocumento(doc);
        cliente.setRazonSocial(nuloSiVacio(request.getRazonSocial()));
        cliente.setDireccion(nuloSiVacio(request.getDireccion()));
        cliente.setGenero(Genero.desde(request.getGenero()));
        return clienteRepository.save(cliente);
    }

    /** Buscador por documento, nombre, teléfono o email (paginado). */
    @Transactional(readOnly = true)
    public Page<Cliente> buscar(String q, Pageable pageable) {
        return clienteRepository.buscar(Like.patron(q), pageable);
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> vehiculosDe(Long clienteId) {
        obtenerPorId(clienteId);
        return vehiculoRepository.findByClienteId(clienteId);
    }

    /** Desactivar no borra: el historial (recepciones, comprobantes) se conserva. */
    @Transactional
    public Cliente cambiarActivo(Long id, boolean activo) {
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(activo);
        return clienteRepository.save(cliente);
    }

    /** Ficha para el panel: datos, vehículos, visitas y comprobantes. */
    @Transactional(readOnly = true)
    public ClienteDetalleDTO resumen(Long id) {
        Cliente c = obtenerPorId(id);
        var recepciones = recepcionRepository.porCliente(id);
        return ClienteDetalleDTO.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .tipoDocumento(c.getTipoDocumento() != null ? c.getTipoDocumento().name() : null)
                .documento(c.getDocumento())
                .razonSocial(c.getRazonSocial())
                .telefono(c.getTelefono())
                .email(c.getEmail())
                .direccion(c.getDireccion())
                .genero(c.getGenero() != null ? c.getGenero().name() : null)
                .activo(c.getActivo())
                .visitas((long) recepciones.size())
                .ultimaVisita(recepciones.stream().findFirst()
                        .map(r -> r.getFechaIngreso()).orElse(null))
                .vehiculos(vehiculoRepository.findByClienteId(id).stream()
                        .map(v -> ClienteDetalleDTO.VehiculoDTO.builder()
                                .id(v.getId()).placa(v.getPlaca()).marca(v.getMarca())
                                .modelo(v.getModelo()).anio(v.getAnio()).build())
                        .toList())
                .recepciones(recepciones.stream()
                        .map(r -> ClienteDetalleDTO.RecepcionDTO.builder()
                                .id(r.getId()).fechaIngreso(r.getFechaIngreso()).estado(r.getEstado().name())
                                .placa(r.getVehiculo().getPlaca())
                                .problema(r.getProblemaReportado()).build())
                        .toList())
                .comprobantes(comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(id).stream()
                        .map(x -> ClienteDetalleDTO.ComprobanteDTO.builder()
                                .id(x.getId()).folio(x.folio()).tipo(x.getTipo().name())
                                .total(x.getTotal()).estado(x.getEstado().name())
                                .fechaEmision(x.getFechaEmision()).build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cliente no encontrado.", null,
                        null, HttpStatus.NOT_FOUND));
    }

    private String nuloSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
