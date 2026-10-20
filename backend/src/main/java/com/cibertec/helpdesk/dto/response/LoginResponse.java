package com.cibertec.helpdesk.dto.response;

public record LoginResponse(
        String token,
        String tipo,
        String correo,
        String rol
) {}
