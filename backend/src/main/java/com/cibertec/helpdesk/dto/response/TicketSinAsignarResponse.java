package com.cibertec.helpdesk.dto.response;

import java.time.LocalDateTime;

public record TicketSinAsignarResponse(
        Integer idTicket,
        String codigo,
        String asunto,
        String prioridad,
        LocalDateTime fechaCreacion
) {}
