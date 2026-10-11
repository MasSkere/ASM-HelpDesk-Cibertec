package com.cibertec.helpdesk.entity;

/**
 * Roles del sistema. Deben coincidir EXACTAMENTE con el CHECK de
 * {@code usuarios.rol} en database/schema.sql.
 */
public enum Rol {
    ESTUDIANTE,
    DOCENTE,
    TECNICO,
    ADMINISTRADOR
}
