package com.cibertec.helpdesk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato de error estándar exigido por INSTRUCCIONES_AGENTE.md §6:
 * { timestamp, status, error, message, path, detalles[] }
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, List<String> detalles) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, detalles);
    }
}
