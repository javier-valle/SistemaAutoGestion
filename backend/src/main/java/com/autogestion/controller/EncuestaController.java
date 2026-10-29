package com.autogestion.controller;

import com.autogestion.dto.EncuestaAdminDTO;
import com.autogestion.service.EncuestaService;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Comentarios y calificaciones que los clientes dejaron en el portal. */
@RolesAllowed({"ADMIN", "RECEPCIONISTA"})
@RestController
@RequestMapping("/api/encuestas")
@RequiredArgsConstructor
public class EncuestaController {

    private final EncuestaService encuestaService;

    @GetMapping
    public List<EncuestaAdminDTO> listar() {
        return encuestaService.listar();
    }
}
