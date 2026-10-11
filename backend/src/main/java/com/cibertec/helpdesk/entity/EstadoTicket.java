package com.cibertec.helpdesk.entity;

/**
 * Estado del ticket. Coincide con el CHECK de {@code tickets.estado}.
 */
public enum EstadoTicket {
    REGISTRADO,
    EN_PROCESO,
    RESUELTO,
    CERRADO,
    CANCELADO
}
