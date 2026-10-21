package com.cibertec.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Texto base para que el asistente IA sugiera tipo, prioridad y categoría
 * antes de registrar un ticket.
 */
public record SugerirTicketRequest(

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 150, message = "El asunto no puede superar 150 caracteres")
    String asunto,

    @NotBlank(message = "La descripcion es obligatoria")
    String descripcion

) {}
