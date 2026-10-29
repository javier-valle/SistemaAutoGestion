package com.autogestion.service;

import com.autogestion.dto.ComprobanteResponseDTO;
import com.autogestion.dto.PortalComprobanteDTO;
import com.autogestion.dto.PortalContactoRequest;
import com.autogestion.dto.PortalEncuestaDTO;
import com.autogestion.dto.PortalEncuestaPendienteDTO;
import com.autogestion.dto.PortalEncuestaRequest;
import com.autogestion.dto.PortalHistorialDTO;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ComprobanteRepository comprobanteRepository;
    private final CotizacionServicioRepository cotizacionServicioRepository;
    private final EncuestaSatisfaccionRepository encuestaSatisfaccionRepository;
    private final ComprobanteService comprobanteService;
    private final CitaRepository citaRepository;
    private final SolicitudCotizacionRepository solicitudCotizacionRepository;
    private final RecepcionRepository recepcionRepository;

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
     * Comprobantes emitidos a nombre del cliente. El id sale del token y la
     * consulta filtra por él: no hay forma de pedir el documento de otro.
     */
    @Transactional(readOnly = true)
    public List<PortalComprobanteDTO> comprobantes(Long clienteId) {
        clienteActivo(clienteId);
        return comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(clienteId).stream()
                .map(c -> new PortalComprobanteDTO(
                        c.getId(),
                        c.getOrdenTrabajo().getId(),
                        c.getTipo().name(),
                        c.folio(),
                        c.getFechaEmision(),
                        c.getTotal(),
                        c.getEstado().name(),
                        c.getOrdenTrabajo().getCotizacion().getDiagnostico()
                                .getRecepcion().getVehiculo().getPlaca()))
                .toList();
    }

    /** Un comprobante propio, listo para generar el PDF. Ajeno = 404, sin filtrar si existe. */
    @Transactional(readOnly = true)
    public ComprobanteResponseDTO comprobanteDeCliente(Long clienteId, Long comprobanteId) {
        ComprobanteResponseDTO c = comprobanteService.obtener(comprobanteId);
        if (c.getClienteId() == null || !c.getClienteId().equals(clienteId)) {
            throw new BusinessException("Comprobante no encontrado.", HttpStatus.NOT_FOUND);
        }
        return c;
    }

    /** Visitas cerradas, con lo que se hizo y el comprobante si se emitió. */
    @Transactional(readOnly = true)
    public List<PortalHistorialDTO> historial(Long clienteId) {
        clienteActivo(clienteId);
        Map<Long, String> folios = comprobanteRepository.findByClienteIdOrderByFechaEmisionDesc(clienteId)
                .stream()
                .filter(c -> c.getEstado() == EstadoComprobante.EMITIDO)
                .collect(Collectors.toMap(
                        c -> c.getOrdenTrabajo().getId(),
                        Comprobante::folio,
                        (primero, segundo) -> primero));

        return ordenTrabajoRepository.findByClienteId(clienteId).stream()
                .filter(ot -> ot.getEstado() == EstadoOT.FINALIZADA)
                .map(ot -> {
                    Cotizacion cotizacion = ot.getCotizacion();
                    Vehiculo vehiculo = cotizacion.getDiagnostico().getRecepcion().getVehiculo();
                    List<String> servicios = cotizacionServicioRepository
                            .findByCotizacionId(cotizacion.getId()).stream()
                            .map(cs -> cs.getServicio().getNombre())
                            .toList();
                    return new PortalHistorialDTO(
                            ot.getId(),
                            vehiculo.getPlaca(),
                            vehiculoTexto(vehiculo),
                            cotizacion.getDiagnostico().getRecepcion().getFechaIngreso(),
                            ot.getFechaFin(),
                            ot.getEstado().name(),
                            cotizacion.getTotal(),
                            folios.get(ot.getId()),
                            servicios);
                })
                .toList();
    }

    /** OT entregadas que todavía no tienen encuesta. */
    @Transactional(readOnly = true)
    public List<PortalEncuestaPendienteDTO> encuestasPendientes(Long clienteId) {
        clienteActivo(clienteId);
        return ordenTrabajoRepository.findByClienteId(clienteId).stream()
                .filter(ot -> ot.getEstado() == EstadoOT.FINALIZADA)
                .filter(ot -> !encuestaSatisfaccionRepository
                        .existsByOrdenTrabajoIdAndClienteId(ot.getId(), clienteId))
                .map(ot -> {
                    Vehiculo v = ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo();
                    return new PortalEncuestaPendienteDTO(ot.getId(), v.getPlaca(), vehiculoTexto(v), ot.getFechaFin());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortalEncuestaDTO> encuestasRespondidas(Long clienteId) {
        clienteActivo(clienteId);
        return encuestaSatisfaccionRepository.findByClienteIdOrderByCreadoEnDesc(clienteId).stream()
                .map(e -> new PortalEncuestaDTO(
                        e.getId(),
                        e.getOrdenTrabajo() == null ? null : e.getOrdenTrabajo().getId(),
                        e.getOrdenTrabajo() == null ? null : placaDe(e.getOrdenTrabajo()),
                        e.getPuntuacion(),
                        e.getComentario(),
                        e.getCreadoEn()))
                .toList();
    }

    /** Una calificación por OT entregada; el dueño sale del token, nunca del body. */
    @Transactional
    public PortalEncuestaDTO responderEncuesta(Long clienteId, PortalEncuestaRequest req) {
        Cliente cliente = clienteActivo(clienteId);
        OrdenTrabajo ot = ordenTrabajoRepository.findById(req.getOrdenTrabajoId())
                .orElseThrow(() -> new BusinessException("No encontramos esa orden de trabajo.",
                        HttpStatus.NOT_FOUND));

        Long dueno = ot.getCotizacion().getDiagnostico().getRecepcion()
                .getVehiculo().getCliente().getId();
        if (!clienteId.equals(dueno)) {
            throw new BusinessException("No encontramos esa orden de trabajo.", HttpStatus.NOT_FOUND);
        }
        if (ot.getEstado() != EstadoOT.FINALIZADA) {
            throw new BusinessException("Solo puedes calificar una orden ya entregada.", HttpStatus.CONFLICT);
        }
        if (encuestaSatisfaccionRepository.existsByOrdenTrabajoIdAndClienteId(ot.getId(), clienteId)) {
            throw new BusinessException("Ya calificaste esta visita. ¡Gracias!", HttpStatus.CONFLICT);
        }

        EncuestaSatisfaccion encuesta = encuestaSatisfaccionRepository.save(
                EncuestaSatisfaccion.builder()
                        .cliente(cliente)
                        .ordenTrabajo(ot)
                        .puntuacion(req.getPuntuacion())
                        .comentario(texto(req.getComentario()))
                        .build());

        return new PortalEncuestaDTO(
                encuesta.getId(),
                ot.getId(),
                placaDe(ot),
                encuesta.getPuntuacion(),
                encuesta.getComentario(),
                encuesta.getCreadoEn());
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



    /**
     * Baja de la cuenta del portal. Borra los datos personales y los registros
     * que solo existen para el portal; lo que ya está emitido (comprobantes y
     * órdenes) se conserva anonimizado por obligación contable.
     */
    @Transactional
    public void eliminarCuenta(Long clienteId, com.autogestion.dto.PortalBajaRequest req) {
        Cliente cliente = clienteActivo(clienteId);
        if (req == null || !"ELIMINAR".equalsIgnoreCase(texto(req.getConfirmacion()))) {
            throw new BusinessException("Escribe ELIMINAR para confirmar la baja de tu cuenta.",
                    "confirmacion", "La confirmación evita borrar tu cuenta por error.", HttpStatus.BAD_REQUEST);
        }

        encuestaSatisfaccionRepository.deleteByClienteId(clienteId);
        consentimientoRepository.deleteByClienteId(clienteId);
        citaRepository.deleteByClienteId(clienteId);
        solicitudCotizacionRepository.deleteByClienteId(clienteId);
        portalAccesoLogRepository.deleteByClienteId(clienteId);

        for (Vehiculo v : vehiculoRepository.findByClienteId(clienteId)) {
            if (recepcionRepository.countByVehiculoId(v.getId()) == 0) {
                vehiculoRepository.delete(v);
            }
        }

        cliente.setNombre("Cliente dado de baja");
        cliente.setDocumento(null);
        cliente.setTelefono(null);
        cliente.setEmail(null);
        cliente.setDireccion(null);
        cliente.setRazonSocial(null);
        cliente.setGenero(null);
        cliente.setDocumentoVerificado(false);
        cliente.setFuenteVerificacion(null);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
        log.info("Baja de cuenta del portal solicitada por el cliente {}", clienteId);
    }

    private Cliente clienteActivo(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new BusinessException("Sesión no válida", HttpStatus.UNAUTHORIZED));
    }

    private static String placaDe(OrdenTrabajo ot) {
        return ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo().getPlaca();
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
