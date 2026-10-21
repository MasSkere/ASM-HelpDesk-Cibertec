package com.cibertec.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mensaje enviado al asistente IA (chatbot flotante del frontend).
 */
public record ChatRequest(

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 1000, message = "El mensaje no puede superar 1000 caracteres")
    String mensaje,

    /** Contexto opcional del chat: GENERAL o NUEVO_TICKET. */
    String contexto

) {}
