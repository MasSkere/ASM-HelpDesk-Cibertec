package com.cibertec.helpdesk.exception;

import com.cibertec.helpdesk.dto.response.ApiError;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Manejo global de errores con el formato estándar de INSTRUCCIONES_AGENTE.md §6:
 * { timestamp, status, error, message, path, detalles[] }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Recurso no encontrado: HTTP 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> noEncontrado(
            NoSuchElementException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // Datos incorrectos: HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> datosInvalidos(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // Operacion no permitida por el estado actual: HTTP 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> conflicto(
            IllegalStateException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        HttpStatus.CONFLICT.value(),
                        "CONFLICT",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // Credenciales inválidas (login): HTTP 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> noAutenticado(
            AuthenticationException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        "Credenciales incorrectas",
                        request.getRequestURI()));
    }

    // Sin permisos: HTTP 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> prohibido(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        HttpStatus.FORBIDDEN.value(),
                        "FORBIDDEN",
                        ex.getMessage() != null ? ex.getMessage() : "Acceso denegado",
                        request.getRequestURI()));
    }

    // Violación de restricciones de la BD (FK, unique...): HTTP 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        HttpStatus.CONFLICT.value(),
                        "CONFLICT",
                        "La operacion viola una restriccion de datos",
                        request.getRequestURI()));
    }

    // Método HTTP no soportado: HTTP 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoSoportado(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of(
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "METHOD_NOT_ALLOWED",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // Errores en las validaciones de los DTO: HTTP 400 con detalle por campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> errorValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "campo '" + error.getField() + "': "
                        + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Datos invalidos",
                        request.getRequestURI(),
                        detalles));
    }

    // JSON malformado o tipo de dato no convertible (enum inválido): HTTP 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        "El cuerpo de la peticion no es un JSON valido",
                        request.getRequestURI()));
    }

    // Cualquier error no controlado: HTTP 500 sin exponer stacktrace
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorInterno(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "Error interno del servidor",
                        request.getRequestURI()));
    }
}
