package com.autogestion.service;

import com.autogestion.dto.DiagnosticoRequest;
import com.autogestion.dto.DiagnosticoResponseDTO;
import com.autogestion.entity.Diagnostico;
import com.autogestion.entity.EstadoRecepcion;
import com.autogestion.entity.Recepcion;
import com.autogestion.entity.Usuario;
import com.autogestion.exception.BusinessException;
import com.autogestion.util.AppTime;
import com.autogestion.repository.DiagnosticoRepository;
import com.autogestion.repository.RecepcionRepository;
import com.autogestion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosticoService {

    private final DiagnosticoRepository diagnosticoRepository;
    private final RecepcionRepository recepcionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public DiagnosticoResponseDTO crear(DiagnosticoRequest request) {
        Recepcion recepcion = recepcionRepository.findById(request.getRecepcionId())
                .orElseThrow(() -> new BusinessException("Recepción no encontrada.", "recepcionId",
                        null, org.springframework.http.HttpStatus.NOT_FOUND));

        Usuario mecanico = usuarioRepository.findById(request.getMecanicoId())
                .orElseThrow(() -> new BusinessException("Mecánico no encontrado.", "mecanicoId",
                        null, org.springframework.http.HttpStatus.NOT_FOUND));
        if (!"MECANICO".equals(mecanico.getRol())) {
            throw new BusinessException("El usuario elegido no es mecánico.", "mecanicoId",
                    "Elige un usuario con rol MECANICO.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(mecanico.getActivo())) {
            throw new BusinessException("El mecánico está desactivado.", "mecanicoId",
                    "Actívalo en Equipo o elige otro.", org.springframework.http.HttpStatus.CONFLICT);
        }

        
        recepcion.setEstado(EstadoRecepcion.EN_DIAGNOSTICO);
        recepcionRepository.save(recepcion);

        Diagnostico diagnostico = Diagnostico.builder()
                .recepcion(recepcion)
                .mecanico(mecanico)
                .descripcion(request.getDescripcion())
                .fecha(AppTime.ahora())
                .build();
        Diagnostico saved = diagnosticoRepository.save(diagnostico);

        
        return DiagnosticoResponseDTO.builder()
                .id(saved.getId())
                .descripcion(saved.getDescripcion())
                .fecha(saved.getFecha())
                .recepcionId(saved.getRecepcion().getId())
                .mecanicoId(saved.getMecanico().getId())
                .mecanicoNombre(saved.getMecanico().getNombre())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DiagnosticoResponseDTO> listarPorRecepcion(Long recepcionId) {
        return diagnosticoRepository.findByRecepcionId(recepcionId)
                .stream()
                .map(d -> DiagnosticoResponseDTO.builder()
                        .id(d.getId())
                        .descripcion(d.getDescripcion())
                        .fecha(d.getFecha())
                        .recepcionId(d.getRecepcion().getId())
                        .mecanicoId(d.getMecanico().getId())
                        .mecanicoNombre(d.getMecanico().getNombre())
                        .build())
                .toList();
    }
}
