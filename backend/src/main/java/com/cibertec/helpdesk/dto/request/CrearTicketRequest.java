package com.cibertec.helpdesk.dto.request;

import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 150, message = "El asunto no puede superar 150 caracteres")
    String asunto,

    @NotBlank(message = "La descripcion es obligatoria")
    String descripcion,

    @NotNull(message = "El tipo de solicitud es obligatorio")
    TipoSolicitud tipoSolicitud,

    @NotNull(message = "La prioridad es obligatoria")
    Prioridad prioridad,

    @NotNull(message = "La categoria es obligatoria")
    Integer idCategoria

) {}
