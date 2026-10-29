package com.autogestion.service;

import com.autogestion.dto.PortalContactoRequest;
import com.autogestion.dto.PortalRegistroRequest;
import com.autogestion.dto.PortalResumenDTO;
import com.autogestion.entity.*;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private final PortalAccesoLogRepository portalAccesoLogRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final ConsultaDocumentoService consultaDocumentoService;

    /**
     * Modo A: documento + placa. La placa tiene que ser DEL cliente
     * (antes bastaba con que ambos existieran por separado).
     */
    @Transactional
    public Optional<Cliente> identificarModoA(String documento, String tipoDocumento, String placa, String ip) {
        Optional<Cliente> cliente = buscarCliente(documento, tipoDocumento)
                .filter(c -> placaDelCliente(c, placa));
        registrar(documento, cliente.orElse(null), cliente.isPresent() ? "IDENTIFICADO" : "FALLIDO", "A", ip);
        return cliente;
    }

    @Transactional(readOnly = true)
    public PortalResumenDTO resumen(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new BusinessException("Sesión no válida", HttpStatus.UNAUTHORIZED));

        List<PortalResumenDTO.VehiculoDTO> vehiculos = vehiculoRepository.findByClienteId(clienteId).stream()
                .map(v -> new PortalResumenDTO.VehiculoDTO(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAnio()))
                .toList();

        List<PortalResumenDTO.OrdenDTO> ordenes = ordenTrabajoRepository.findByClienteId(clienteId).stream()
                .limit(20)
                .map(ot -> new PortalResumenDTO.OrdenDTO(
                        ot.getId(),
                        ot.getEstado().name(),
                        ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo().getPlaca(),
                        ot.getFechaCreacion(),
                        ot.getFechaFin()))
                .toList();

        return new PortalResumenDTO(
                cliente.nombreFiscal(),
                cliente.getTipoDocumento().name(),
                cliente.getDocumento(),
                Boolean.TRUE.equals(cliente.getDocumentoVerificado()),
                cliente.getFuenteVerificacion(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getCreadoEn(),
                vehiculos,
                ordenes);
    }

    /**
     * El cliente actualiza solo sus datos de contacto. El nombre y el documento
     * quedan fuera a propósito: identifican la cuenta y los administra el taller.
     */
    @Transactional
    public PortalResumenDTO actualizarContacto(Long clienteId, PortalContactoRequest req) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new BusinessException("Sesión no válida", HttpStatus.UNAUTHORIZED));

        String telefono = texto(req.getTelefono());
        if (telefono != null && !telefono.matches("^9\\d{8}$")) {
            throw new BusinessException("El celular debe empezar con 9 y tener 9 dígitos.",
                    "telefono", "Ejemplo: 912345678", HttpStatus.BAD_REQUEST);
        }
        String email = texto(req.getEmail());
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            throw new BusinessException("El correo no tiene un formato válido.",
                    "email", "Ejemplo: nombre@dominio.com", HttpStatus.BAD_REQUEST);
        }

        cliente.setTelefono(telefono);
        cliente.setEmail(email);
        clienteRepository.save(cliente);
        return resumen(clienteId);
    }



    private Optional<Cliente> buscarCliente(String documento, String tipoDocumento) {
        TipoDocumento td = TipoDocumento.desde(tipoDocumento);
        if (td == null || documento == null || documento.isBlank()) {
            return Optional.empty();
        }
        return clienteRepository.findByTipoDocumentoAndDocumento(td, documento.trim())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()));
    }

    private boolean placaDelCliente(Cliente cliente, String placa) {
        if (placa == null || placa.isBlank()) return false;
        String normalizada = placa.toUpperCase().trim().replaceAll("\\s+", "");
        return vehiculoRepository.findByPlaca(normalizada)
                .map(v -> v.getCliente() != null && cliente.getId().equals(v.getCliente().getId()))
                .orElse(false);
    }

    private void registrar(String documento, Cliente cliente, String resultado, String tipo, String ip) {
        portalAccesoLogRepository.save(PortalAccesoLog.builder()
                .cliente(cliente)
                .documentoHash(sha256(documento == null ? "" : documento))
                .resultado(resultado)
                .tipo(tipo)
                .ipHash(sha256(ip == null ? "" : ip))
                .build());
    }

    @Transactional
    public Cliente registrarCliente(PortalRegistroRequest req, String ip) {
        TipoDocumento tipo = req.tipoEfectivo();
        String doc = req.documentoNormalizado();


        if (clienteRepository.existsByTipoDocumentoAndDocumento(tipo, doc)) {
            throw new BusinessException("Ya existe una cuenta con ese documento. Intenta ingresar al portal.",
                    HttpStatus.CONFLICT);
        }


        com.autogestion.dto.ConsultaDocumentoResponseDTO consulta =
                consultaDocumentoService.consultar(req.getTipoDocumento(), doc);
        boolean verificado = consulta != null && consulta.isVerificado();

        String nombreCliente;
        String razonSocialCliente = null;
        String direccionFiscalCliente = null;

        if (verificado) {

            nombreCliente = texto(consulta.getNombre());
            if (nombreCliente == null) nombreCliente = texto(consulta.getRazonSocial());
            if (nombreCliente == null) {
                throw new BusinessException("No pudimos leer los datos de tu documento. Intenta más tarde.", HttpStatus.BAD_REQUEST);
            }
            if (tipo == TipoDocumento.RUC) {
                String rs = texto(consulta.getRazonSocial());
                razonSocialCliente = rs != null ? rs : nombreCliente;
                direccionFiscalCliente = texto(consulta.getDireccion());
            }
        } else {

            if (tipo == TipoDocumento.RUC) {
                razonSocialCliente = nombreDeclarado(req.getRazonSocial(), true);
                nombreCliente = razonSocialCliente;
                direccionFiscalCliente = texto(req.getDireccionFiscal());
                if (direccionFiscalCliente == null || direccionFiscalCliente.length() > 250 || "-".equals(direccionFiscalCliente)) {
                    throw new BusinessException("Ingresa la dirección fiscal (máximo 250 caracteres).", HttpStatus.BAD_REQUEST);
                }
            } else {
                nombreCliente = nombreDeclarado(req.getNombre(), false);
            }
        }


        nombreCliente = recortar(nombreCliente, 150);
        razonSocialCliente = recortar(razonSocialCliente, 200);
        direccionFiscalCliente = recortar(direccionFiscalCliente, 250);


        if (verificado && tipo == TipoDocumento.RUC) {
            String estadoDoc = consulta.getEstado() != null ? consulta.getEstado().trim() : "";
            String condicionDoc = consulta.getCondicion() != null ? consulta.getCondicion().trim() : "";
            if (!"ACTIVO".equalsIgnoreCase(estadoDoc) || !"HABIDO".equalsIgnoreCase(condicionDoc)) {
                log.info("Registro con RUC no ACTIVO/HABIDO (estado={}, condicion={})", estadoDoc, condicionDoc);
            }
        }

        Cliente cliente = Cliente.builder()
                .tipoDocumento(tipo)
                .documento(doc)
                .nombre(nombreCliente)
                .razonSocial(razonSocialCliente)
                .telefono(req.getTelefono() == null || req.getTelefono().isBlank() ? null : req.getTelefono().trim())
                .email(req.getEmail() == null || req.getEmail().isBlank() ? null : req.getEmail().trim())
                .direccion(direccionFiscalCliente)
                .activo(true)
                .documentoVerificado(verificado)
                .fuenteVerificacion(verificado ? texto(consulta.getFuente()) : null)
                .build();

        try {
            cliente = clienteRepository.save(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Ya existe una cuenta con ese documento. Intenta ingresar al portal.", HttpStatus.CONFLICT);
        }


        consentimientoRepository.save(Consentimiento.builder()
                .cliente(cliente)
                .politicaVersion("v1")
                .finalidad("portal_cliente")
                .aceptado(true)
                .ipHash(sha256(ip == null ? "" : ip))
                .build());


        String placaNormalizada = req.getPlaca() != null ? req.getPlaca().trim().toUpperCase().replaceAll("\\s+", "") : null;
        if (placaNormalizada != null && !placaNormalizada.isBlank()) {
            Vehiculo veh = Vehiculo.builder()
                    .cliente(cliente)
                    .placa(placaNormalizada)
                    .marca(req.getMarca() == null || req.getMarca().isBlank() ? null : req.getMarca().trim())
                    .modelo(req.getModelo() == null || req.getModelo().isBlank() ? null : req.getModelo().trim())
                    .anio(req.getAnio())
                    .build();
            vehiculoRepository.save(veh);
        }

        return cliente;
    }

    private static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static String texto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static final java.util.regex.Pattern NOMBRE_PERSONA =
            java.util.regex.Pattern.compile("^\\p{L}[\\p{L} .'’\\-]{2,149}$");
    private static final java.util.regex.Pattern NOMBRE_EMPRESA =
            java.util.regex.Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,&'’\\-/()]{2,149}$");

    /** Nombre/razón social escrito por el cliente: se limpia y se valida (3-150 caracteres). */
    private static String nombreDeclarado(String bruto, boolean empresa) {
        String n = texto(bruto);
        if (n != null) n = n.replaceAll("\\s+", " ");
        boolean ok = n != null && (empresa ? NOMBRE_EMPRESA : NOMBRE_PERSONA).matcher(n).matches();
        if (!ok) {
            throw new BusinessException(empresa
                    ? "Ingresa la razón social tal como figura en tu RUC."
                    : "Ingresa tus nombres y apellidos tal como figuran en tu documento.", HttpStatus.BAD_REQUEST);
        }
        return n;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
