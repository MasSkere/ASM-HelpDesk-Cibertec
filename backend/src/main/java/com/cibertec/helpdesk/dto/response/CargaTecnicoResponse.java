package com.cibertec.helpdesk.dto.response;

public record CargaTecnicoResponse(
        Integer idUsuario,
        String tecnico,
        Long abiertos,
        Long atendidos
) {}
