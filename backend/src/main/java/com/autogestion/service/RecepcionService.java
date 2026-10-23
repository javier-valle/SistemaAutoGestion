package com.autogestion.service;

import com.autogestion.dto.RecepcionCompletaRequest;
import com.autogestion.dto.RecepcionRequest;
import com.autogestion.dto.RecepcionResponseDTO;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.EstadoRecepcion;
import com.autogestion.entity.Genero;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.entity.Vehiculo;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.VehiculoRepository;
import com.autogestion.util.AppTime;
import com.autogestion.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecepcionService {

    private final RecepcionRepository recepcionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;

    @Transactional
    public RecepcionResponseDTO crear(RecepcionRequest request) {
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new BusinessException(
                        "Vehículo no encontrado: " + request.getVehiculoId(), "vehiculoId",
                        "Elige un vehículo de la lista.", HttpStatus.NOT_FOUND));

        Recepcion recepcion = Recepcion.builder()
                .vehiculo(vehiculo)
                .problemaReportado(request.getProblemaReportado().trim())
                .fechaIngreso(AppTime.ahora())
                .estado(EstadoRecepcion.PENDIENTE)
                .build();
        Recepcion saved = recepcionRepository.save(recepcion);
        return toDTO(saved);
    }

    @Transactional
    public RecepcionResponseDTO crearCompleto(RecepcionCompletaRequest request) {
        // Wizard: si ya eligió cliente/vehículo existentes, se usan directo (verificados).
        // El nombre y la placa solo se exigen cuando hay que crear (sin IDs).
        if (request.getClienteId() == null
                && (request.getClienteNombre() == null || request.getClienteNombre().trim().length() < 2)) {
            throw new BusinessException("Escribe el nombre del cliente (mínimo 2 letras).", "clienteNombre",
                    "O elige un cliente existente en el buscador.");
        }
        Cliente cliente = request.getClienteId() != null
                ? clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() -> new BusinessException("El cliente elegido no existe.", "clienteId",
                                "Búscalo de nuevo.", HttpStatus.NOT_FOUND))
                : findOrCreateCliente(request);

        Vehiculo vehiculo = request.getVehiculoId() != null
                ? vehiculoDelCliente(request.getVehiculoId(), cliente.getId())
                : findOrCreateVehiculo(cliente, request);

        if (request.getKilometraje() != null && (request.getKilometraje() < 0 || request.getKilometraje() > 2_000_000)) {
            throw new BusinessException("Kilometraje fuera de rango.", "kilometraje",
                    "Debe estar entre 0 y 2 000 000.");
        }
        Recepcion recepcion = Recepcion.builder()
                .vehiculo(vehiculo)
                .problemaReportado(request.getProblemaReportado().trim())
                .fechaIngreso(AppTime.ahora())
                .estado(EstadoRecepcion.PENDIENTE)
                .nivelCombustible(vacioANulo(request.getNivelCombustible()))
                .danosPrevios(vacioANulo(request.getDanosPrevios()))
                .accesorios(vacioANulo(request.getAccesorios()))
                .kilometraje(request.getKilometraje())
                .build();
        return toDTO(recepcionRepository.save(recepcion));
    }

    private Vehiculo vehiculoDelCliente(Long vehiculoId, Long clienteId) {
        Vehiculo v = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new BusinessException("El vehículo elegido no existe.", "vehiculoId",
                        "Elige otro de la lista.", HttpStatus.NOT_FOUND));
        if (!v.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("Ese vehículo es de otro cliente.", "vehiculoId",
                    "Elige un vehículo del cliente actual.", HttpStatus.CONFLICT);
        }
        return v;
    }

    private RecepcionResponseDTO toDTO(Recepcion saved) {
        return RecepcionResponseDTO.builder()
                .id(saved.getId())
                .problemaReportado(saved.getProblemaReportado())
                .fechaIngreso(saved.getFechaIngreso())
                .estado(saved.getEstado().name())
                .vehiculoId(saved.getVehiculo().getId())
                .vehiculoPlaca(saved.getVehiculo().getPlaca())
                .clienteId(saved.getVehiculo().getCliente().getId())
                .clienteNombre(saved.getVehiculo().getCliente().getNombre())
                .nivelCombustible(saved.getNivelCombustible())
                .danosPrevios(saved.getDanosPrevios())
                .accesorios(saved.getAccesorios())
                .kilometraje(saved.getKilometraje())
                .build();
    }

    /**
     * La identidad de una persona es (tipoDocumento, documento).
     * El teléfono y el email NO identifican: dos clientes distintos pueden
     * compartirlos (familia, empresa) y jamás deben fusionarse.
     */
    private Cliente findOrCreateCliente(RecepcionCompletaRequest request) {
        TipoDocumento tipo = request.getClienteTipoDocumento() != null
                && !request.getClienteTipoDocumento().isBlank()
                ? TipoDocumento.desde(request.getClienteTipoDocumento()) : TipoDocumento.DNI;
        if (tipo == null) {
            throw new BusinessException("Tipo de documento inválido.", "clienteTipoDocumento",
                    "Usa DNI, RUC, CE o PASAPORTE.");
        }
        String doc = DocumentoValidator.normalizar(request.getClienteDocumento());
        if (!doc.isEmpty()) {
            if (!DocumentoValidator.valido(tipo, doc)) {
                throw new BusinessException(DocumentoValidator.mensajeError(tipo, doc), "clienteDocumento",
                        "Revisa el tipo y el número.");
            }
            Optional<Cliente> existing = clienteRepository.findByTipoDocumentoAndDocumento(tipo, doc);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Cliente cliente = Cliente.builder()
                .nombre(request.getClienteNombre().trim())
                .telefono(vacioANulo(request.getClienteTelefono()))
                .email(vacioANulo(request.getClienteEmail()))
                .tipoDocumento(tipo)
                .documento(doc.isEmpty() ? null : doc)
                .razonSocial(vacioANulo(request.getClienteRazonSocial()))
                .direccion(vacioANulo(request.getClienteDireccion()))
                .genero(Genero.desde(request.getClienteGenero()))
                .build();
        return clienteRepository.save(cliente);
    }

    /**
     * Si la placa existe y es de OTRO cliente, se bloquea con 409:
     * una placa no puede pertenecer a dos dueños.
     */
    private Vehiculo findOrCreateVehiculo(Cliente cliente, RecepcionCompletaRequest request) {
        if (request.getVehiculoPlaca() == null || request.getVehiculoPlaca().isBlank()) {
            throw new BusinessException("La placa es obligatoria.", "vehiculoPlaca",
                    "Ej. ABC-123.");
        }
        String placa = request.getVehiculoPlaca().toUpperCase().trim();
        Optional<Vehiculo> existing = vehiculoRepository.findByPlaca(placa);
        if (existing.isPresent()) {
            Vehiculo v = existing.get();
            if (!v.getCliente().getId().equals(cliente.getId())) {
                throw new BusinessException("La placa " + placa + " ya está registrada a nombre de "
                        + v.getCliente().getNombre() + ".", "vehiculoPlaca",
                        "Verifica la placa o atiende al dueño registrado.", HttpStatus.CONFLICT);
            }
            return v;
        }

        Vehiculo vehiculo = Vehiculo.builder()
                .cliente(cliente)
                .placa(placa)
                .marca(request.getVehiculoMarca())
                .modelo(request.getVehiculoModelo())
                .anio(request.getVehiculoAnio())
                .build();
        return vehiculoRepository.save(vehiculo);
    }

    private String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @Transactional(readOnly = true)
    public List<RecepcionResponseDTO> listar(String estado) {
        List<Recepcion> recepciones;
        if (estado == null || estado.isEmpty()) {
            recepciones = recepcionRepository.findAll();
        } else {
            EstadoRecepcion est = EstadoRecepcion.desde(estado);
            recepciones = (est != null) ? recepcionRepository.findByEstado(est) : List.of();
        }
        return recepciones.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public RecepcionResponseDTO obtenerPorId(Long id) {
        Recepcion recepcion = recepcionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Recepción no encontrada.", null,
                        null, HttpStatus.NOT_FOUND));
        return toDTO(recepcion);
    }
}
