package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.response.HistorialTicketResponse;
import com.cibertec.helpdesk.repository.HistorialTicketRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistorialTicketService {

    private final HistorialTicketRepository historialRepository;
    private final TicketService ticketService;

    public HistorialTicketService(
            HistorialTicketRepository historialRepository,
            TicketService ticketService) {

        this.historialRepository = historialRepository;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public List<HistorialTicketResponse> listarHistorial(
            Integer idTicket,
            String correoUsuario) {

        // Verificar primero que el usuario pueda consultar el ticket.
        ticketService.validarAcceso(idTicket, correoUsuario);

        // Recuperar sus movimientos desde PostgreSQL.
        return historialRepository
                .findByIdTicketOrderByIdHistorialAsc(idTicket)
                .stream()
                .map(historial -> new HistorialTicketResponse(
                        historial.getIdHistorial(),
                        historial.getIdTicket(),
                        historial.getAccion(),
                        historial.getEstadoAnterior(),
                        historial.getEstadoNuevo(),
                        historial.getIdTecnico(),
                        historial.getFecha()
                ))
                .toList();
    }
}
