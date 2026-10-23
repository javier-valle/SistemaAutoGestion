package com.autogestion.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "No tienes permisos para realizar esta accion");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            if (err instanceof FieldError) {
                FieldError fe = (FieldError) err;
                errors.put(fe.getField(), fe.getDefaultMessage());
            } else {
                // Errores globales (@AssertTrue): son ObjectError, no FieldError.
                errors.put(err.getObjectName(), err.getDefaultMessage());
            }
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflictoDatos(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "El registro choca con datos existentes (duplicado o referencia en uso).");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleBloqueo(PessimisticLockingFailureException ex) {
        log.warn("Contención de bloqueo pesimista: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Otro usuario está operando ese registro ahora mismo. Intenta de nuevo.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonMalformado(HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "El cuerpo de la petición no es un JSON válido.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Parámetro '" + ex.getName() + "' con formato inválido.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("mensaje", ex.getMessage());
        if (ex.getCampo() != null) error.put("campo", ex.getCampo());
        if (ex.getSugerencia() != null) error.put("sugerencia", ex.getSugerencia());
        return ResponseEntity.status(ex.getEstado()).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        // Legado: los servicios usan RuntimeException para errores de negocio
        // esperados (se migran a BusinessException). Se registra para rastreo.
        log.warn("RuntimeException controlada: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        error.put("error", message != null ? message : "Error inesperado, contacte soporte");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
