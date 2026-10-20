package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository
        extends JpaRepository<Ticket, Integer> {

    // Tickets asignados a un tecnico
    List<Ticket> findByIdTecnico(Integer idTecnico);

    // Tickets registrados por un solicitante
    List<Ticket> findByIdSolicitante(Integer idSolicitante);
}
