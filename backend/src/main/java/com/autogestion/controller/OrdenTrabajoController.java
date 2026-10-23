package com.autogestion.controller;

import com.autogestion.dto.OrdenTrabajoCompletaRequest;
import com.autogestion.dto.OrdenTrabajoRequest;
import com.autogestion.dto.OrdenTrabajoResponseDTO;
import com.autogestion.dto.OtProductoUsadoResponseDTO;
import com.autogestion.dto.ProductoUsadoRequest;
import com.autogestion.entity.Cotizacion;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.Usuario;
import com.autogestion.entity.Vehiculo;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.service.OrdenTrabajoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RolesAllowed({"ADMIN", "RECEPCIONISTA", "MECANICO"})
@RestController
@RequestMapping("/api/ordenes-trabajo")
@RequiredArgsConstructor
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenTrabajoService;

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PostMapping
    public ResponseEntity<OrdenTrabajoResponseDTO> crear(@Valid @RequestBody OrdenTrabajoRequest request) {
        OrdenTrabajo ot = ordenTrabajoService.crear(request);
        return ResponseEntity.ok(mapToDTO(ot));
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PostMapping("/completa")
    public ResponseEntity<OrdenTrabajoResponseDTO> crearCompleta(@Valid @RequestBody OrdenTrabajoCompletaRequest request) {
        OrdenTrabajo ot = ordenTrabajoService.crearCompleta(request);
        return ResponseEntity.ok(mapToDTO(ot));
    }

    @RolesAllowed({"ADMIN", "MECANICO"})
    @PutMapping("/{id}/estado")
    public ResponseEntity<OrdenTrabajoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrdenTrabajo ot = ordenTrabajoService.cambiarEstado(id, body.get("estado"));
        return ResponseEntity.ok(mapToDTO(ot));
    }

    @RolesAllowed({"ADMIN", "MECANICO"})
    @PostMapping("/{id}/productos-usados")
    public ResponseEntity<OtProductoUsadoResponseDTO> registrarProductoUsado(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUsadoRequest request,
            Authentication auth) {
        return ResponseEntity.ok(ordenTrabajoService.registrarProductoUsado(id, request, Long.valueOf(auth.getName())));
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @PutMapping("/{id}/mecanico")
    public ResponseEntity<OrdenTrabajoResponseDTO> reasignarMecanico(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(ordenTrabajoService.reasignarMecanico(id, body.get("mecanicoId")));
    }

    @GetMapping
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ordenTrabajoService.listar(estado));
    }

    @GetMapping("/mecanico/{mecanicoId}")
    public ResponseEntity<List<OrdenTrabajoResponseDTO>> listarPorMecanico(@PathVariable Long mecanicoId) {
        return ResponseEntity.ok(ordenTrabajoService.listarPorMecanico(mecanicoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajoResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/productos-usados")
    public ResponseEntity<List<OtProductoUsadoResponseDTO>> listarProductosUsados(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.listarProductosUsados(id));
    }

    private OrdenTrabajoResponseDTO mapToDTO(OrdenTrabajo ot) {
        Cotizacion cotizacion = ot.getCotizacion();
        Diagnostico diagnostico = cotizacion.getDiagnostico();
        Recepcion recepcion = diagnostico.getRecepcion();
        Vehiculo vehiculo = recepcion.getVehiculo();
        Cliente cliente = vehiculo.getCliente();
        Usuario mecanico = ot.getMecanico();

        return OrdenTrabajoResponseDTO.builder()
                .id(ot.getId())
                .cotizacionId(cotizacion.getId())
                .mecanicoId(ot.getMecanico().getId())
                .mecanicoNombre(mecanico.getNombreCompleto())
                .estado(ot.getEstado().name())
                .fechaCreacion(ot.getFechaCreacion())
                .fechaFin(ot.getFechaFin())
                .vehiculoPlaca(recepcion.getVehiculo().getPlaca())
                .clienteNombre(recepcion.getVehiculo().getCliente().getNombre())
                .recepcionId(recepcion.getId())
                .problemaReportado(recepcion.getProblemaReportado())
                .build();
    }
}
