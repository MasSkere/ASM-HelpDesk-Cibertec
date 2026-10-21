package com.cibertec.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta/edición de una categoría (solo ADMINISTRADOR). */
public record CategoriaRequest(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 2000, message = "La descripcion es demasiado larga")
        String descripcion

) {}
