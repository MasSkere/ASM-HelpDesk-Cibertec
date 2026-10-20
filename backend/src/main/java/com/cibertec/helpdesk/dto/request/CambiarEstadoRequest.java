package com.cibertec.helpdesk.dto.request;

import com.cibertec.helpdesk.entity.EstadoTicket;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoTicket estado

) {}
