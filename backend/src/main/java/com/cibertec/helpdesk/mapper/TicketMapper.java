package com.cibertec.helpdesk.mapper;

import com.cibertec.helpdesk.dto.response.TicketResponse;
import com.cibertec.helpdesk.entity.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversión Entity ⇄ DTO para tickets.
 * Mapeo manual en {@code @Component} (permitido por INSTRUCCIONES_AGENTE.md §3).
 * No contiene lógica de negocio.
 */
@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return new TicketResponse(
                ticket.getIdTicket(),
                ticket.getCodigo(),
                ticket.getAsunto(),
                ticket.getDescripcion(),
                ticket.getTipoSolicitud(),
                ticket.getPrioridad(),
                ticket.getEstado(),
                ticket.getFechaCreacion(),
                ticket.getIdSolicitante(),
                ticket.getIdTecnico(),
                ticket.getIdCategoria()
        );
    }

    public List<TicketResponse> toResponseList(List<Ticket> tickets) {
        return tickets.stream()
                .map(this::toResponse)
                .toList();
    }
}
