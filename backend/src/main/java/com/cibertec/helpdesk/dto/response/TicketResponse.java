package com.cibertec.helpdesk.dto.response;

import com.cibertec.helpdesk.entity.EstadoTicket;
import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;

import java.time.LocalDateTime;

/** Representación pública de un ticket (nunca se expone la Entity). */
public record TicketResponse(
        Integer idTicket,
        String codigo,
        String asunto,
        String descripcion,
        TipoSolicitud tipoSolicitud,
        Prioridad prioridad,
        EstadoTicket estado,
        LocalDateTime fechaCreacion,
        Integer idSolicitante,
        Integer idTecnico,
        Integer idCategoria
) {}
