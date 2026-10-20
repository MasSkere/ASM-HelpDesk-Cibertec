package com.cibertec.helpdesk.dto.response;

import java.time.LocalDateTime;

public record HistorialTicketResponse(
        Integer idHistorial,
        Integer idTicket,
        String accion,
        String estadoAnterior,
        String estadoNuevo,
        Integer idTecnico,
        LocalDateTime fecha
) {}
