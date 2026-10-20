package com.cibertec.helpdesk.dto.response;

import com.cibertec.helpdesk.entity.Rol;

public record UsuarioResponse(
        Integer idUsuario,
        String nombres,
        String apellidos,
        String correo,
        Rol rol,
        Boolean estado
) {}
