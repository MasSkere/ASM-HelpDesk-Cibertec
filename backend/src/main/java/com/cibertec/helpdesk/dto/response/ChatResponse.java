package com.cibertec.helpdesk.dto.response;

/**
 * Respuesta del asistente IA.
 *
 * @param respuesta texto generado
 * @param origen    "WATSONX" si respondió IBM, o "LOCAL" si usó el
 *                  asistente heurístico de respaldo
 */
public record ChatResponse(
    String respuesta,
    String origen
) {
    public static final String ORIGEN_WATSONX = "WATSONX";
    public static final String ORIGEN_LOCAL = "LOCAL";
}
