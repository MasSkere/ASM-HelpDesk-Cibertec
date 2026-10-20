package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.HistorialTicket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialTicketRepository
        extends JpaRepository<HistorialTicket, Integer> {

    List<HistorialTicket> findByIdTicketOrderByIdHistorialAsc(
            Integer idTicket
    );
}
