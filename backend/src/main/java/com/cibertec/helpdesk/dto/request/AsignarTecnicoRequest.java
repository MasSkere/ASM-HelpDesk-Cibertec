package com.cibertec.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @NotNull(message = "El ID del tecnico es obligatorio")
        @Positive(message = "El ID debe ser positivo")
        Integer idTecnico

) {}
