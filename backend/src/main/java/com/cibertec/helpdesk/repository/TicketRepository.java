package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de {@link Ticket}.
 * Vacío a propósito: los programadores backend definen las consultas aquí.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}